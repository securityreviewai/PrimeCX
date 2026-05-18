package com.primecx.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.primecx.model.SavedReply;

@Repository
public interface SavedReplyRepository extends JpaRepository<SavedReply, Long> {

    List<SavedReply> findAllByOrderByTitleAsc();
}
