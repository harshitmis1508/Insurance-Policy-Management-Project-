package com.harshit.monocept.service;

import com.harshit.monocept.dto.request.CreateAgentRequest;
import com.harshit.monocept.dto.request.UserStatusUpdateRequest;
import com.harshit.monocept.dto.response.UserResponse;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.enums.Role;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.exception.DuplicateResourceException;
import com.harshit.monocept.exception.ResourceNotFoundException;
import com.harshit.monocept.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	// SRS FR-USER-001: Admin saare users dekhe
	public Page<UserResponse> getAllUsers(Pageable pageable) {
		return userRepository.findAll(pageable).map(this::mapToResponse);
	}

	// SRS FR-USER-001: Filter by role
	public Page<UserResponse> getUsersByRole(Role role, Pageable pageable) {
		return userRepository.findByRole(role, pageable).map(this::mapToResponse);
	}

	// SRS FR-USER-001: Filter by active status
	public Page<UserResponse> getUsersByStatus(Boolean isActive, Pageable pageable) {
		return userRepository.findByIsActive(isActive, pageable).map(this::mapToResponse);
	}

	// SRS FR-USER-004: Admin agent account banaye — USR-BR-004
	public UserResponse createAgent(CreateAgentRequest req) {

		// SRS USR-BR-001: Duplicate email
		if (userRepository.existsByEmail(req.getEmail()))
			throw new DuplicateResourceException("Email already exists: " + req.getEmail());

		User agent = User.builder().fullName(req.getFullName()).email(req.getEmail())
				.password(passwordEncoder.encode(req.getPassword())).mobileNumber(req.getMobileNumber())
				.role(Role.AGENT) // Hardcode AGENT — SRS USR-BR-004
				.isActive(true).build();

		User saved = userRepository.save(agent);
		log.info("Agent created by admin: {}", saved.getEmail());
		return mapToResponse(saved);
	}

	// SRS FR-USER-002/003: Admin activate/deactivate
	public UserResponse updateUserStatus(Long userId, UserStatusUpdateRequest req) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

		// Admin khud ko deactivate na kare
		if (user.getRole() == Role.ADMIN && !req.getIsActive())
			throw new BusinessRuleException("Cannot deactivate an admin account");

		Boolean oldStatus = user.getIsActive();
		user.setIsActive(req.getIsActive());
		User updated = userRepository.save(user);

		log.info("User {} status changed from {} to {}. Reason: {}", user.getEmail(), oldStatus, req.getIsActive(),
				req.getReason());

		return mapToResponse(updated);
	}

	// Single user by ID
	public UserResponse getUserById(Long userId) {
		return mapToResponse(userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId)));
	}

	private UserResponse mapToResponse(User u) {
		return UserResponse.builder().userId(u.getId()).fullName(u.getFullName()).email(u.getEmail())
				.mobileNumber(u.getMobileNumber()).role(u.getRole()).isActive(u.getIsActive())
				.createdAt(u.getCreatedAt()).updatedAt(u.getUpdatedAt()).build();
	}
}