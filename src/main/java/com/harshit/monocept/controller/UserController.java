package com.harshit.monocept.controller;

import com.harshit.monocept.dto.request.CreateAgentRequest;
import com.harshit.monocept.dto.request.UserStatusUpdateRequest;
import com.harshit.monocept.dto.response.ApiResponse;
import com.harshit.monocept.dto.response.UserResponse;
import com.harshit.monocept.enums.Role;
import com.harshit.monocept.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	// SRS FR-USER-001: Admin all users
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String direction, @RequestParam(required = false) Role role,
			@RequestParam(required = false) Boolean isActive) {

		if (size > 100)
			size = 100;
		Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
		Pageable pageable = PageRequest.of(page, size, sort);

		Page<UserResponse> result;
		if (role != null)
			result = userService.getUsersByRole(role, pageable);
		else if (isActive != null)
			result = userService.getUsersByStatus(isActive, pageable);
		else
			result = userService.getAllUsers(pageable);

		return ResponseEntity.ok(ApiResponse.success("Users fetched", result));
	}

	// Single user
	@GetMapping("/{userId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long userId) {
		return ResponseEntity.ok(ApiResponse.success("User fetched", userService.getUserById(userId)));
	}

	// SRS FR-USER-004: Admin agent banaye
	@PostMapping("/agent")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserResponse>> createAgent(@Valid @RequestBody CreateAgentRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Agent created successfully", userService.createAgent(req)));
	}

	// SRS FR-USER-002/003: Activate/Deactivate
	@PatchMapping("/{userId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserResponse>> updateStatus(@PathVariable Long userId,
			@Valid @RequestBody UserStatusUpdateRequest req) {
		return ResponseEntity.ok(ApiResponse.success("User status updated", userService.updateUserStatus(userId, req)));
	}
}