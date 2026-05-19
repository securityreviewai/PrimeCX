package com.primecx.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.primecx.dto.UserDto;
import com.primecx.model.Role;
import com.primecx.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers().stream()
                .map(userService::toDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    /**
     * Compact list of active support executives for ticket assignment UI.
     */
    @GetMapping("/assignable-executives")
    @PreAuthorize("hasAnyRole('SUPPORT_ADMIN', 'SUPPORT_MANAGER')")
    public ResponseEntity<List<UserDto>> listAssignableExecutives() {
        return ResponseEntity.ok(userService.listActiveSupportExecutivesForAssignment());
    }

    /**
     * Resolves platform directory entry by email (e.g. ticket customer picker, @-mentions).
     */
    @GetMapping("/directory")
    public ResponseEntity<UserDto> lookupByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.toDto(userService.getUserByEmail(email.strip())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toDto(userService.getUserById(id)));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<UserDto> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Role role = Role.valueOf(body.get("role"));
        return ResponseEntity.ok(userService.toDto(userService.updateUserRole(id, role)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPPORT_ADMIN')")
    public ResponseEntity<UserDto> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.toDto(userService.deactivateUser(id)));
    }
}
