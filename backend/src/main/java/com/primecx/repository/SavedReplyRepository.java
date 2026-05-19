package com.primecx.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.primecx.model.SavedReply;

@Repository
public interface SavedReplyRepository extends JpaRepository<SavedReply, Long> {

    List<SavedReply> findAllByOrderByTitleAsc();

    @Query("SELECT s FROM SavedReply s WHERE LOWER(s.title) LIKE :pattern OR LOWER(s.body) LIKE :pattern ORDER BY s.title ASC")
    List<SavedReply> searchByTitleOrBodyLike(@Param("pattern") String pattern, Pageable pageable);
}
