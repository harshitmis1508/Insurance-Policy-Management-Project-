package com.harshit.monocept.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

	private final ClaimService claimService;

	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<ClaimResponse>> submit(@Valid @RequestBody ClaimRequest req,
			Authentication auth) {
		return ResponseEntity.status(HttpStatus.CREATED).body(
				ApiResponse.success("Claim submitted successfully", claimService.submitClaim(req, auth.getName())));
	}

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

	@GetMapping("/{claimId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<ClaimResponse>> getById(@PathVariable Long claimId) {
		return ResponseEntity.ok(ApiResponse.success("Claim details", claimService.getClaimById(claimId)));
	}

	@PatchMapping("/{claimId}/review")
	@PreAuthorize("hasRole('AGENT')")
	public ResponseEntity<ApiResponse<ClaimResponse>> review(@PathVariable Long claimId,
			@Valid @RequestBody ClaimReviewRequest req, Authentication auth) {
		return ResponseEntity
				.ok(ApiResponse.success("Claim reviewed", claimService.reviewClaim(claimId, req, auth.getName())));
	}

	@PatchMapping("/{claimId}/decide")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<ClaimResponse>> decide(@PathVariable Long claimId,
			@Valid @RequestBody ClaimDecisionRequest req, Authentication auth) {
		return ResponseEntity.ok(
				ApiResponse.success("Claim decision recorded", claimService.decideClaim(claimId, req, auth.getName())));
	}

	@GetMapping("/{claimId}/history")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Page<ClaimHistoryResponse>>> getHistory(@PathVariable Long claimId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(ApiResponse.success("Claim history",
				claimService.getClaimHistory(claimId, PageRequest.of(page, size, Sort.by("updatedAt").ascending()))));
	}
}