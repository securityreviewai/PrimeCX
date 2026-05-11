package com.primecx.repository;

import com.primecx.model.Role;
import com.primecx.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByOktaId(String oktaId);

    List<User> findByRole(Role role);

    List<User> findByActive(boolean active);

    Page<User> findByActive(boolean active, Pageable pageable);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);

    Page<User> findByActiveAndFirstNameContainingIgnoreCaseOrActiveAndLastNameContainingIgnoreCaseOrActiveAndEmailContainingIgnoreCase(
            boolean active, String firstName, boolean active2, String lastName, boolean active3, String email, Pageable pageable);

    boolean existsByEmail(String email);
}
