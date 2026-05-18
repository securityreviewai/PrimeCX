package com.primecx.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateSavedReplyRequest;
import com.primecx.dto.SavedReplyDto;
import com.primecx.dto.UpdateSavedReplyRequest;
import com.primecx.exception.ForbiddenException;
import com.primecx.exception.ResourceNotFoundException;
import com.primecx.model.Role;
import com.primecx.model.SavedReply;
import com.primecx.model.User;
import com.primecx.repository.SavedReplyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SavedReplyService {

    private final SavedReplyRepository savedReplyRepository;

    @Transactional(readOnly = true)
    public List<SavedReplyDto> listAll() {
        return savedReplyRepository.findAllByOrderByTitleAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public SavedReplyDto create(User actor, CreateSavedReplyRequest request) {
        assertCanManage(actor);
        SavedReply row = new SavedReply();
        row.setTitle(request.title().strip());
        row.setBody(request.body().strip());
        SavedReply saved = savedReplyRepository.save(row);
        return toDto(saved);
    }

    @Transactional
    public SavedReplyDto update(User actor, Long id, UpdateSavedReplyRequest request) {
        assertCanManage(actor);
        SavedReply row = savedReplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SavedReply", id));
        row.setTitle(request.title().strip());
        row.setBody(request.body().strip());
        return toDto(savedReplyRepository.save(row));
    }

    @Transactional
    public void delete(User actor, Long id) {
        assertCanManage(actor);
        SavedReply row = savedReplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SavedReply", id));
        savedReplyRepository.delete(row);
    }

    private void assertCanManage(User actor) {
        if (actor.getRole() != Role.ROLE_SUPPORT_ADMIN && actor.getRole() != Role.ROLE_SUPPORT_MANAGER) {
            throw new ForbiddenException("Only support administrators or managers can manage saved replies.");
        }
    }

    private SavedReplyDto toDto(SavedReply row) {
        return new SavedReplyDto(row.getId(), row.getTitle(), row.getBody(), row.getCreatedAt());
    }
}
