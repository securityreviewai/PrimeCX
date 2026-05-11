package com.primecx.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.primecx.dto.CreateOrganizationRequest;
import com.primecx.dto.OrganizationDto;
import com.primecx.model.Organization;
import com.primecx.repository.OrganizationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public List<OrganizationDto> listAll() {
        return organizationRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public OrganizationDto create(CreateOrganizationRequest request) {
        Organization o = Organization.builder().name(request.name().trim()).build();
        return toDto(organizationRepository.save(o));
    }

    public Organization getById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new com.primecx.exception.ResourceNotFoundException("Organization", id));
    }

    private OrganizationDto toDto(Organization o) {
        return new OrganizationDto(o.getId(), o.getName());
    }
}
