package com.primecx.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.primecx.dto.TicketPresenceSnapshotDto;
import com.primecx.dto.TicketPresenceViewerDto;
import com.primecx.model.User;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TicketPresenceService {

    private static final int MAX_CONNECTIONS_PER_TICKET = 50;
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;
    private static final long HEARTBEAT_SECONDS = 15;

    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, PresenceConnection>> subscribers =
            new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            1,
            runnable -> {
                Thread t = new Thread(runnable, "ticket-presence-heartbeat");
                t.setDaemon(true);
                return t;
            });

    public SseEmitter subscribe(Long ticketId, User viewer) {
        String connId = UUID.randomUUID().toString();
        ConcurrentHashMap<String, PresenceConnection> map =
                subscribers.computeIfAbsent(ticketId, k -> new ConcurrentHashMap<>());

        synchronized (map) {
            if (map.size() >= MAX_CONNECTIONS_PER_TICKET) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Presence capacity reached");
            }
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        String displayName = buildDisplayName(viewer);
        PresenceConnection pc = new PresenceConnection(connId, emitter, viewer.getId(), displayName);

        ScheduledFuture<?> heartbeat = scheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (Exception e) {
                removeConnection(ticketId, connId);
            }
        }, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
        pc.setHeartbeat(heartbeat);

        java.util.concurrent.atomic.AtomicBoolean ended = new java.util.concurrent.atomic.AtomicBoolean(false);
        Runnable onEnd = () -> {
            if (!ended.compareAndSet(false, true)) {
                return;
            }
            pc.cancelHeartbeat();
            removeConnection(ticketId, connId);
        };
        emitter.onCompletion(onEnd);
        emitter.onTimeout(onEnd);
        emitter.onError(e -> onEnd.run());

        synchronized (map) {
            map.put(connId, pc);
        }

        log.debug("Presence join ticket {} user {}", ticketId, viewer.getId());
        broadcastSnapshot(ticketId);
        return emitter;
    }

    private void removeConnection(Long ticketId, String connId) {
        ConcurrentHashMap<String, PresenceConnection> map = subscribers.get(ticketId);
        if (map == null) {
            return;
        }
        PresenceConnection removed;
        synchronized (map) {
            removed = map.remove(connId);
            if (map.isEmpty()) {
                subscribers.remove(ticketId);
            }
        }
        if (removed != null) {
            removed.cancelHeartbeat();
            log.debug("Presence leave ticket {} user {}", ticketId, removed.userId);
            broadcastSnapshot(ticketId);
        }
    }

    private void broadcastSnapshot(Long ticketId) {
        ConcurrentHashMap<String, PresenceConnection> map = subscribers.get(ticketId);
        if (map == null || map.isEmpty()) {
            return;
        }

        List<PresenceConnection> copy;
        synchronized (map) {
            copy = List.copyOf(map.values());
        }

        TicketPresenceSnapshotDto payload = buildPayload(copy);
        List<String> dead = new ArrayList<>();
        for (PresenceConnection pc : copy) {
            try {
                pc.emitter.send(SseEmitter.event()
                        .name("presence")
                        .data(payload, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                dead.add(pc.connId);
            }
        }
        for (String id : dead) {
            removeConnection(ticketId, id);
        }
    }

    private static TicketPresenceSnapshotDto buildPayload(List<PresenceConnection> connections) {
        Map<Long, TicketPresenceViewerDto> dedup = new LinkedHashMap<>();
        for (PresenceConnection pc : connections) {
            dedup.putIfAbsent(pc.userId, new TicketPresenceViewerDto(pc.userId, pc.displayName));
        }
        return new TicketPresenceSnapshotDto(List.copyOf(dedup.values()));
    }

    private static String buildDisplayName(User u) {
        String fn = u.getFirstName() != null ? u.getFirstName() : "";
        String ln = u.getLastName() != null ? u.getLastName() : "";
        String combined = (fn + " " + ln).trim();
        return combined.isEmpty() ? "Teammate" : combined;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private static final class PresenceConnection {
        final String connId;
        final SseEmitter emitter;
        final Long userId;
        final String displayName;
        private volatile ScheduledFuture<?> heartbeat;

        PresenceConnection(String connId, SseEmitter emitter, Long userId, String displayName) {
            this.connId = connId;
            this.emitter = emitter;
            this.userId = userId;
            this.displayName = displayName;
        }

        void setHeartbeat(ScheduledFuture<?> heartbeat) {
            this.heartbeat = heartbeat;
        }

        void cancelHeartbeat() {
            ScheduledFuture<?> h = heartbeat;
            if (h != null) {
                h.cancel(false);
            }
        }
    }
}
