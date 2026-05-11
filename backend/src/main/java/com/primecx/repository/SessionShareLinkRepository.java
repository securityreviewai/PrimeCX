package com.primecx.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.primecx.model.SessionShareLink;

@Repository
public interface SessionShareLinkRepository extends JpaRepository<SessionShareLink, Long> {

    Optional<SessionShareLink> findByToken(String token);
}
