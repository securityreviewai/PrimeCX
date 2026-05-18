package com.primecx.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.primecx.model.TicketAttachment;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByTicket_IdOrderByUploadedAtDesc(Long ticketId);
}
