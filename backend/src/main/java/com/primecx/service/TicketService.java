package com.primecx.service;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateTicketRequest;
import com.primecx.dto.PagedTicketsResponse;
import com.primecx.dto.SubmitTicketSatisfactionRequest;
import com.primecx.dto.TicketCategorizationResult;
import com.primecx.dto.TicketDto;
import com.primecx.dto.TicketStatsResponse;
import com.primecx.dto.UpdateTicketRequest;
import com.primecx.exception.ForbiddenException;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Role;
import com.primecx.model.Ticket;
import com.primecx.model.TicketActivityType;
import com.primecx.model.TicketCategory;
import com.primecx.model.TicketPriority;
import com.primecx.model.TicketStatus;
import com.primecx.model.User;
import com.primecx.repository.TicketRepository;
import com.primecx.repository.UserRepository;
import com.primecx.specification.TicketSpecifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final int EXPORT_ROW_CAP = 5_000;
    private static final DateTimeFormatter CSV_INSTANT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MAX_TAGS_PER_TICKET = 20;

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketActivityService ticketActivityService;
    private final AIAnalysisService aiAnalysisService;

    @Transactional
    public Ticket claimTicket(Long ticketId, User executive) {
        if (executive.getRole() != Role.ROLE_SUPPORT_EXECUTIVE) {
            throw new ForbiddenException("Only support executives can claim tickets.");
        }
        Ticket ticket = getTicketById(ticketId);
        if (ticket.getAssignedTo() != null) {
            throw new IllegalArgumentException("This ticket already has an assignee.");
        }
        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Only open or in-progress tickets can be claimed.");
        }

        User assigneeEntity = userRepository.findById(executive.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", executive.getId()));
        ticket.setAssignedTo(assigneeEntity);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        ticketActivityService.record(saved.getId(), executive, TicketActivityType.CLAIMED, "Ticket claimed from queue");
        log.info("Ticket {} claimed by executive {}", ticketId, executive.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean writeVisibleTicketsCsv(User viewer, Writer writer) throws IOException {
        Specification<Ticket> spec = Specification.where(TicketSpecifications.visibleToUser(viewer));
        Pageable pageable = PageRequest.of(0, EXPORT_ROW_CAP, Sort.by("createdAt").descending());
        Page<Ticket> page = ticketRepository.findAll(spec, pageable);

        writer.write('\ufeff');
        writer.write("id,title,description,status,priority,category,tags,user_id,user_name,assignee_id,assignee_name,created_at,updated_at,customer_rating,customer_feedback,satisfaction_submitted_at,sla_respond_by,sla_breached\n");
        for (Ticket ticket : page.getContent()) {
            TicketDto row = toDto(ticket);
            writer.write(Long.toString(row.id()));
            writer.write(',');
            writer.write(csvField(row.title()));
            writer.write(',');
            writer.write(csvField(row.description()));
            writer.write(',');
            writer.write(row.status() != null ? row.status().name() : "");
            writer.write(',');
            writer.write(row.priority() != null ? row.priority().name() : "");
            writer.write(',');
            writer.write(row.category() != null ? row.category().name() : "");
            writer.write(',');
            writer.write(csvField(row.tags().isEmpty() ? "" : String.join("|", row.tags())));
            writer.write(',');
            writer.write(Long.toString(row.userId()));
            writer.write(',');
            writer.write(csvField(row.userName()));
            writer.write(',');
            writer.write(row.assignedToId() != null ? Long.toString(row.assignedToId()) : "");
            writer.write(',');
            writer.write(row.assignedToName() != null ? csvField(row.assignedToName()) : "");
            writer.write(',');
            writer.write(row.createdAt() != null ? CSV_INSTANT.format(row.createdAt()) : "");
            writer.write(',');
            writer.write(row.updatedAt() != null ? CSV_INSTANT.format(row.updatedAt()) : "");
            writer.write(',');
            writer.write(row.customerRating() != null ? Integer.toString(row.customerRating()) : "");
            writer.write(',');
            writer.write(row.customerFeedback() != null ? csvField(row.customerFeedback()) : "");
            writer.write(',');
            writer.write(row.satisfactionSubmittedAt() != null ? CSV_INSTANT.format(row.satisfactionSubmittedAt()) : "");
            writer.write(',');
            writer.write(row.slaRespondBy() != null ? CSV_INSTANT.format(row.slaRespondBy()) : "");
            writer.write(',');
            writer.write(row.slaBreached() ? "Y" : "N");
            writer.write('\n');
        }
        return page.getTotalElements() > EXPORT_ROW_CAP;
    }

    private static String csvField(String value) {
        if (value == null) {
            return "\"\"";
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
    }

    private LocalDateTime computeSlaRespondBy(TicketPriority priority, LocalDateTime from) {
        return switch (priority) {
            case CRITICAL -> from.plusHours(4);
            case HIGH -> from.plusHours(24);
            case MEDIUM -> from.plusHours(72);
            case LOW -> from.plusDays(5);
        };
    }

    private boolean computeSlaBreached(Ticket ticket) {
        if (ticket.getSlaRespondBy() == null) {
            return false;
        }
        if (ticket.getStatus() != TicketStatus.OPEN && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            return false;
        }
        return LocalDateTime.now().isAfter(ticket.getSlaRespondBy());
    }

    @Transactional
    public Ticket createTicket(CreateTicketRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
        ticket.setPriority(request.priority());
        ticket.setCategory(request.category() != null ? request.category() : TicketCategory.GENERAL_INQUIRY);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setUser(user);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticket.setSlaRespondBy(computeSlaRespondBy(request.priority(), now));

        log.info("Creating ticket '{}' for user {}", request.title(), userId);
        Ticket saved = ticketRepository.save(ticket);
        ticketActivityService.record(saved.getId(), user, TicketActivityType.TICKET_CREATED, "Ticket opened");
        return saved;
    }

    @Transactional
    public Ticket updateTicket(Long ticketId, UpdateTicketRequest request, User updater) {
        Ticket ticket = getTicketById(ticketId);
        assertCanUpdateTicket(updater, ticket);

        TicketStatus prevStatus = ticket.getStatus();
        TicketPriority prevPriority = ticket.getPriority();
        TicketCategory prevCategory = ticket.getCategory();
        String prevTitle = ticket.getTitle();
        String prevDesc = ticket.getDescription();
        Long prevAssigneeId = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null;

        boolean fullAccess = updater.getRole() == Role.ROLE_SUPPORT_ADMIN
                || updater.getRole() == Role.ROLE_SUPPORT_MANAGER
                || updater.getRole() == Role.ROLE_SUPPORT_EXECUTIVE;

        List<String> notes = new ArrayList<>();

        if (fullAccess) {
            if (request.title() != null && !Objects.equals(request.title(), prevTitle)) {
                ticket.setTitle(request.title());
                notes.add("Title updated");
            }
            if (request.description() != null && !Objects.equals(request.description(), prevDesc)) {
                ticket.setDescription(request.description());
                notes.add("Description updated");
            }
            if (request.status() != null && request.status() != prevStatus) {
                ticket.setStatus(request.status());
                notes.add("Status: " + prevStatus + " → " + request.status());
            }
            if (request.priority() != null && request.priority() != prevPriority) {
                ticket.setPriority(request.priority());
                ticket.setSlaRespondBy(computeSlaRespondBy(request.priority(), LocalDateTime.now()));
                notes.add("Priority: " + prevPriority + " → " + request.priority());
            }
            if (request.category() != null && !Objects.equals(request.category(), prevCategory)) {
                ticket.setCategory(request.category());
                notes.add("Category: "
                        + (prevCategory != null ? prevCategory : TicketCategory.GENERAL_INQUIRY)
                        + " → "
                        + request.category());
            }
            if (request.assignedToId() != null) {
                Long aid = request.assignedToId();
                if (!Objects.equals(prevAssigneeId, aid)) {
                    User assignee = userRepository.findById(aid)
                            .orElseThrow(() -> new ResourceNotFoundException("User", aid));
                    ticket.setAssignedTo(assignee);
                    notes.add("Assignee → user #" + aid);
                }
            }
            if (request.tags() != null) {
                LinkedHashSet<String> nextTags = normalizeTags(request.tags());
                Set<String> prevTags = new TreeSet<>(ticket.getTags());
                if (!prevTags.equals(nextTags)) {
                    ticket.getTags().clear();
                    ticket.getTags().addAll(nextTags);
                    notes.add("Tags updated");
                }
            }
        } else {
            if (request.title() != null && !Objects.equals(request.title(), prevTitle)) {
                ticket.setTitle(request.title());
                notes.add("Title updated");
            }
            if (request.description() != null && !Objects.equals(request.description(), prevDesc)) {
                ticket.setDescription(request.description());
                notes.add("Description updated");
            }
            if (request.priority() != null && request.priority() != prevPriority) {
                ticket.setPriority(request.priority());
                ticket.setSlaRespondBy(computeSlaRespondBy(request.priority(), LocalDateTime.now()));
                notes.add("Priority: " + prevPriority + " → " + request.priority());
            }
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        if (!notes.isEmpty()) {
            ticketActivityService.record(saved.getId(), updater, TicketActivityType.TICKET_UPDATED,
                    String.join("; ", notes));
        }
        return saved;
    }

    /**
     * Returns the ticket if it exists and the viewer is permitted to access it under the same rules as listing tickets.
     */
    @Transactional(readOnly = true)
    public Ticket getTicketVisibleTo(Long ticketId, User viewer) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        if (!canUserViewTicket(viewer, ticket)) {
            throw new ForbiddenException("You do not have access to this ticket.");
        }
        return ticket;
    }

    public boolean canUserViewTicket(User viewer, Ticket ticket) {
        return switch (viewer.getRole()) {
            case ROLE_SUPPORT_ADMIN, ROLE_SUPPORT_MANAGER -> true;
            case ROLE_SUPPORT_EXECUTIVE -> ticket.getAssignedTo() != null
                    && viewer.getId().equals(ticket.getAssignedTo().getId());
            case ROLE_USER -> viewer.getId().equals(ticket.getUser().getId());
        };
    }

    private void assertCanUpdateTicket(User updater, Ticket ticket) {
        switch (updater.getRole()) {
            case ROLE_SUPPORT_ADMIN, ROLE_SUPPORT_MANAGER -> {}
            case ROLE_SUPPORT_EXECUTIVE -> {
                if (ticket.getAssignedTo() == null
                        || !updater.getId().equals(ticket.getAssignedTo().getId())) {
                    throw new ForbiddenException("Only the assigned support executive can update this ticket.");
                }
            }
            case ROLE_USER -> {
                if (!updater.getId().equals(ticket.getUser().getId())) {
                    throw new ForbiddenException("You can only edit your own tickets.");
                }
            }
        }
    }

    public Ticket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    public List<Ticket> getTicketsByUser(Long userId) {
        return ticketRepository.findByUserId(userId);
    }

    public List<Ticket> getTicketsByAssignee(Long assigneeId) {
        return ticketRepository.findByAssignedToId(assigneeId);
    }

    public List<Ticket> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status);
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PagedTicketsResponse searchTickets(User viewer, TicketStatus status,
            TicketPriority priority, TicketCategory category, String tag, String q, Pageable pageable) {
        Specification<Ticket> spec = Specification.where(TicketSpecifications.visibleToUser(viewer));
        if (status != null) {
            spec = spec.and(TicketSpecifications.hasStatus(status));
        }
        if (priority != null) {
            spec = spec.and(TicketSpecifications.hasPriority(priority));
        }
        if (category != null) {
            spec = spec.and(TicketSpecifications.hasCategory(category));
        }
        String normalizedTag = normalizeTag(tag);
        if (!normalizedTag.isEmpty()) {
            spec = spec.and(TicketSpecifications.hasTag(normalizedTag));
        }
        spec = spec.and(TicketSpecifications.matchesSearch(q));

        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        List<TicketDto> content = page.getContent().stream().map(this::toDto).toList();
        return new PagedTicketsResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    @Transactional(readOnly = true)
    public PagedTicketsResponse listSlaBreachedTickets(User viewer, Pageable pageable) {
        Specification<Ticket> spec = Specification.where(TicketSpecifications.visibleToUser(viewer))
                .and(TicketSpecifications.slaOverdueActive());
        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        List<TicketDto> content = page.getContent().stream().map(this::toDto).toList();
        return new PagedTicketsResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    @Transactional(readOnly = true)
    public TicketStatsResponse getTicketStats(User viewer) {
        Specification<Ticket> base = Specification.where(TicketSpecifications.visibleToUser(viewer));
        long total = ticketRepository.count(base);
        EnumMap<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
        for (TicketStatus s : TicketStatus.values()) {
            byStatus.put(s, ticketRepository.count(base.and(TicketSpecifications.hasStatus(s))));
        }
        EnumMap<TicketCategory, Long> byCategory = new EnumMap<>(TicketCategory.class);
        for (TicketCategory c : TicketCategory.values()) {
            byCategory.put(c, ticketRepository.count(base.and(TicketSpecifications.hasCategory(c))));
        }
        long activeCount = byStatus.getOrDefault(TicketStatus.OPEN, 0L)
                + byStatus.getOrDefault(TicketStatus.IN_PROGRESS, 0L);
        long resolvedCount = byStatus.getOrDefault(TicketStatus.RESOLVED, 0L)
                + byStatus.getOrDefault(TicketStatus.CLOSED, 0L);
        return new TicketStatsResponse(byStatus, byCategory, total, activeCount, resolvedCount);
    }

    @Transactional(readOnly = true)
    public PagedTicketsResponse listClaimablePool(Pageable pageable) {
        Specification<Ticket> spec = Specification.where(TicketSpecifications.unassignedClaimable());
        Page<Ticket> page = ticketRepository.findAll(spec, pageable);
        List<TicketDto> content = page.getContent().stream().map(this::toDto).toList();
        return new PagedTicketsResponse(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }

    public TicketDto toDto(Ticket ticket) {
        String userName = ticket.getUser().getFirstName() + " " + ticket.getUser().getLastName();
        String assignedToName = ticket.getAssignedTo() != null
                ? ticket.getAssignedTo().getFirstName() + " " + ticket.getAssignedTo().getLastName()
                : null;
        Long assignedToId = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null;

        return new TicketDto(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory() != null ? ticket.getCategory() : TicketCategory.GENERAL_INQUIRY,
                sortedTags(ticket),
                ticket.getUser().getId(),
                userName,
                assignedToId,
                assignedToName,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getCustomerRating(),
                ticket.getCustomerFeedback(),
                ticket.getSatisfactionSubmittedAt(),
                ticket.getSlaRespondBy(),
                computeSlaBreached(ticket));
    }

    @Transactional
    public TicketDto submitSatisfaction(Long ticketId, User customer, SubmitTicketSatisfactionRequest request) {
        if (customer.getRole() != Role.ROLE_USER) {
            throw new ForbiddenException("Only customers can submit satisfaction feedback.");
        }
        Ticket ticket = getTicketById(ticketId);
        if (!ticket.getUser().getId().equals(customer.getId())) {
            throw new ForbiddenException("You can only rate your own tickets.");
        }
        if (ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new IllegalArgumentException(
                    "Feedback can be submitted after the ticket is resolved or closed.");
        }
        if (ticket.getCustomerRating() != null) {
            throw new IllegalArgumentException("Feedback has already been submitted for this ticket.");
        }

        ticket.setCustomerRating(request.rating());
        String fb = request.feedback();
        ticket.setCustomerFeedback(fb != null && !fb.isBlank() ? fb.strip() : null);
        ticket.setSatisfactionSubmittedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        ticketActivityService.record(saved.getId(), customer, TicketActivityType.SATISFACTION_SUBMITTED,
                "Rating " + request.rating() + "/5");
        log.info("Recorded satisfaction {} for ticket {}", request.rating(), ticketId);
        return toDto(saved);
    }

    /**
     * Runs AI categorization and persists suggested category + priority on the ticket (support roles).
     */
    @Transactional
    public TicketDto applyAiClassification(Long ticketId, User actor) {
        if (actor.getRole() != Role.ROLE_SUPPORT_ADMIN
                && actor.getRole() != Role.ROLE_SUPPORT_MANAGER
                && actor.getRole() != Role.ROLE_SUPPORT_EXECUTIVE) {
            throw new ForbiddenException("Only support staff can apply AI classification.");
        }
        Ticket ticket = getTicketVisibleTo(ticketId, actor);
        TicketCategorizationResult result = aiAnalysisService.categorizeTicket(ticketId);
        TicketCategory newCat = TicketCategory.fromAiSlug(result.suggestedCategory());
        TicketPriority newPri = parseSuggestedPriority(result.suggestedPriority());
        TicketCategory prevCat = ticket.getCategory() != null ? ticket.getCategory() : TicketCategory.GENERAL_INQUIRY;
        TicketPriority prevPri = ticket.getPriority();

        ticket.setCategory(newCat);
        ticket.setPriority(newPri);
        ticket.setSlaRespondBy(computeSlaRespondBy(newPri, LocalDateTime.now()));
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);

        ticketActivityService.record(saved.getId(), actor, TicketActivityType.TICKET_UPDATED,
                "AI classification applied — category "
                        + prevCat + " → " + newCat + "; priority " + prevPri + " → " + newPri);
        log.info("Applied AI classification on ticket {}", ticketId);
        return toDto(saved);
    }

    private static TicketPriority parseSuggestedPriority(String raw) {
        if (raw == null || raw.isBlank()) {
            return TicketPriority.MEDIUM;
        }
        try {
            return TicketPriority.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return TicketPriority.MEDIUM;
        }
    }

    private List<String> sortedTags(Ticket ticket) {
        if (ticket.getTags() == null || ticket.getTags().isEmpty()) {
            return List.of();
        }
        return ticket.getTags().stream().sorted().toList();
    }

    /** Normalize tag input to lowercase [a-z0-9-]{1,48}; empty string when invalid after strip. */
    public static String normalizeTag(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        t = t.replaceAll("[^a-z0-9\\-]", "");
        if (t.length() > 48) {
            t = t.substring(0, 48);
        }
        return t;
    }

    private static LinkedHashSet<String> normalizeTags(List<String> raw) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (raw == null) {
            return out;
        }
        for (String s : raw) {
            String n = normalizeTag(s);
            if (!n.isEmpty()) {
                out.add(n);
            }
            if (out.size() >= MAX_TAGS_PER_TICKET) {
                break;
            }
        }
        return out;
    }
}