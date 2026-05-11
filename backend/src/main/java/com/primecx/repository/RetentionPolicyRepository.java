package com.primecx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.primecx.model.RetentionPolicy;

@Repository
public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, Long> {

    Optional<RetentionPolicy> findByOrganizationIdAndS3Bucket(Long organizationId, String s3Bucket);

    List<RetentionPolicy> findByOrganizationIsNullAndS3Bucket(String s3Bucket);
}
