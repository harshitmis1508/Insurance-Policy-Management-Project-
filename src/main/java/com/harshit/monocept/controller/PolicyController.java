package com.harshit.monocept.controller;

import com.harshit.monocept.dto.request.PolicyIssueRequest;
import com.harshit.monocept.dto.request.PolicyPurchaseRequest;
import com.harshit.monocept.dto.response.ApiResponse;
import com.harshit.monocept.dto.response.PolicyResponse;
import com.harshit.monocept.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

	private final PolicyService policyService;

	// SRS FR-POL-001: Customer policy purchase kare
	@PostMapping("/purchase")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<PolicyResponse>> purchase(@Valid @RequestBody PolicyPurchaseRequest req,
			Authentication auth) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Policy purchased successfully",
				policyService.purchasePolicy(req, auth.getName())));
	}

	// SRS FR-POL-002: Agent/Admin issue kare
	@PostMapping("/issue")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<PolicyResponse>> issue(@Valid @RequestBody PolicyIssueRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Policy issued successfully", policyService.issuePolicy(req)));
	}

	// SRS FR-POL-006: Customer apni policies dekhe
	@GetMapping("/my")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Page<PolicyResponse>>> getMyPolicies(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String direction, Authentication auth) {

		if (size > 100)
			size = 100;
		Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		return ResponseEntity.ok(ApiResponse.success("My policies",
				policyService.getMyPolicies(auth.getName(), PageRequest.of(page, size, sort))));
	}

	// SRS FR-POL-007: Admin/Agent saari policies
	@GetMapping
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<Page<PolicyResponse>>> getAll(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		if (size > 100)
			size = 100;
		Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		return ResponseEntity.ok(
				ApiResponse.success("All policies", policyService.getAllPolicies(PageRequest.of(page, size, sort))));
	}

	// SRS FR-POL-007: Customer ke policies by ID
	@GetMapping("/customer/{customerId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<Page<PolicyResponse>>> getByCustomer(@PathVariable Long customerId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(ApiResponse.success("Customer policies", policyService
				.getPoliciesByCustomer(customerId, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
	}

	// Single policy
	@GetMapping("/{policyId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<PolicyResponse>> getById(@PathVariable Long policyId) {
		return ResponseEntity.ok(ApiResponse.success("Policy details", policyService.getPolicyById(policyId)));
	}

	// SRS FR-POL-008: Cancel policy
	@PatchMapping("/{policyId}/cancel")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<PolicyResponse>> cancel(@PathVariable Long policyId, Authentication auth) {
		return ResponseEntity
				.ok(ApiResponse.success("Policy cancelled", policyService.cancelPolicy(policyId, auth.getName())));
	}
}