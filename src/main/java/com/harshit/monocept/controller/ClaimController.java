package com.harshit.monocept.controller;

import com.harshit.monocept.dto.request.ClaimDecisionRequest;
import com.harshit.monocept.dto.request.ClaimRequest;
import com.harshit.monocept.dto.request.ClaimReviewRequest;
import com.harshit.monocept.dto.response.ApiResponse;
import com.harshit.monocept.dto.response.ClaimHistoryResponse;
import com.harshit.monocept.dto.response.ClaimResponse;
import com.harshit.monocept.enums.ClaimStatus;
import com.harshit.monocept.service.ClaimService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

	private final ClaimService claimService;

	// SRS FR-CLM-001: Customer claim submit kare
	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<ClaimResponse>> submit(@Valid @RequestBody ClaimRequest req,
			Authentication auth) {
		return ResponseEntity.status(HttpStatus.CREATED).body(
				ApiResponse.success("Claim submitted successfully", claimService.submitClaim(req, auth.getName())));
	}

	// SRS FR-CLM-005: Customer apne claims
	@GetMapping("/my")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Page<ClaimResponse>>> getMyClaims(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String direction, Authentication auth) {

		if (size > 100)
			size = 100;
		Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		return ResponseEntity.ok(ApiResponse.success("My claims",
				claimService.getMyClaims(auth.getName(), PageRequest.of(page, size, sort))));
	}

	// SRS FR-CLM-011: Admin/Agent saare claims
	@GetMapping
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<Page<ClaimResponse>>> getAll(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String direction, @RequestParam(required = false) ClaimStatus status) {

		if (size > 100)
			size = 100;
		Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		Pageable pageable = PageRequest.of(page, size, sort);

		Page<ClaimResponse> result = (status != null) ? claimService.getClaimsByStatus(status, pageable)
				: claimService.getAllClaims(pageable);

		return ResponseEntity.ok(ApiResponse.success("Claims", result));
	}

	// Single claim
	@GetMapping("/{claimId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<ClaimResponse>> getById(@PathVariable Long claimId) {
		return ResponseEntity.ok(ApiResponse.success("Claim details", claimService.getClaimById(claimId)));
	}

	// SRS FR-CLM-006/007: Agent review
	@PatchMapping("/{claimId}/review")
	@PreAuthorize("hasRole('AGENT')")
	public ResponseEntity<ApiResponse<ClaimResponse>> review(@PathVariable Long claimId,
			@Valid @RequestBody ClaimReviewRequest req, Authentication auth) {
		return ResponseEntity
				.ok(ApiResponse.success("Claim reviewed", claimService.reviewClaim(claimId, req, auth.getName())));
	}

	// SRS FR-CLM-008: Admin final decision
	@PatchMapping("/{claimId}/decide")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<ClaimResponse>> decide(@PathVariable Long claimId,
			@Valid @RequestBody ClaimDecisionRequest req, Authentication auth) {
		return ResponseEntity.ok(
				ApiResponse.success("Claim decision recorded", claimService.decideClaim(claimId, req, auth.getName())));
	}

	// SRS FR-CLM-010: Claim history
	@GetMapping("/{claimId}/history")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Page<ClaimHistoryResponse>>> getHistory(@PathVariable Long claimId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(ApiResponse.success("Claim history",
				claimService.getClaimHistory(claimId, PageRequest.of(page, size, Sort.by("updatedAt").ascending()))));
	}
}