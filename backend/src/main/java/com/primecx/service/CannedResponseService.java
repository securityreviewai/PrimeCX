package com.primecx.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CannedResponseDto;
import com.primecx.dto.CreateCannedResponseRequest;
import com.primecx.dto.UpdateCannedResponseRequest;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.CannedResponse;
import com.primecx.model.User;
import com.primecx.repository.CannedResponseRepository;
import com.primecx.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CannedResponseService {

    private final CannedResponseRepository cannedResponseRepository;
    private final UserRepository userRepository;

    @Transactional
    public CannedResponse create(CreateCannedResponseRequest request, Long userId) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (cannedResponseRepository.existsByShortcode(request.shortcode())) {
            throw new IllegalArgumentException("Shortcode already exists: /" + request.shortcode());
        }

        CannedResponse cr = CannedResponse.builder()
                .shortcode(request.shortcode())
                .title(request.title())
                .content(request.content())
                .category(request.category())
                .createdBy(creator)
                .build();

        log.info("Canned response created: /{} by user {}", request.shortcode(), userId);
        return cannedResponseRepository.save(cr);
    }

    @Transactional
    public CannedResponse update(Long id, UpdateCannedResponseRequest request) {
        CannedResponse cr = getById(id);

        if (request.shortcode() != null && !request.shortcode().equals(cr.getShortcode())) {
            if (cannedResponseRepository.existsByShortcode(request.shortcode())) {
                throw new IllegalArgumentException("Shortcode already exists: /" + request.shortcode());
            }
            cr.setShortcode(request.shortcode());
        }
        if (request.title() != null) {
            cr.setTitle(request.title());
        }
        if (request.content() != null) {
            cr.setContent(request.content());
        }
        if (request.category() != null) {
            cr.setCategory(request.category());
        }

        log.info("Canned response updated: id={} shortcode=/{}", id, cr.getShortcode());
        return cannedResponseRepository.save(cr);
    }

    @Transactional
    public void delete(Long id) {
        CannedResponse cr = getById(id);
        log.info("Canned response deleted: id={} shortcode=/{}", id, cr.getShortcode());
        cannedResponseRepository.delete(cr);
    }

    public CannedResponse getById(Long id) {
        return cannedResponseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CannedResponse", id));
    }

    public List<CannedResponse> getAll() {
        return cannedResponseRepository.findAllByOrderByShortcodeAsc();
    }

    public CannedResponseDto toDto(CannedResponse cr) {
        String createdByName = cr.getCreatedBy() != null
                ? cr.getCreatedBy().getFirstName() + " " + cr.getCreatedBy().getLastName()
                : null;
        Long createdById = cr.getCreatedBy() != null ? cr.getCreatedBy().getId() : null;

        return new CannedResponseDto(
                cr.getId(),
                cr.getShortcode(),
                cr.getTitle(),
                cr.getContent(),
                cr.getCategory(),
                createdById,
                createdByName,
                cr.getCreatedAt(),
                cr.getUpdatedAt()
        );
    }
}
