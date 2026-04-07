package com.primecx.repository;

import com.primecx.model.Role;
import com.primecx.model.User;
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
}
