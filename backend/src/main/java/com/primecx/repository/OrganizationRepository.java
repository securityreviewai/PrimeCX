package com.primecx.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.primecx.model.Organization;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
