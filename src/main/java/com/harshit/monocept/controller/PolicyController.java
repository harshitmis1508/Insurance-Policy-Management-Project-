package com.harshit.monocept.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

import com.harshit.monocept.dto.request.PolicyIssueRequest;
import com.harshit.monocept.dto.request.PolicyPurchaseRequest;
import com.harshit.monocept.dto.response.ApiResponse;
import com.harshit.monocept.dto.response.PolicyResponse;
import com.harshit.monocept.service.PolicyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

	private final PolicyService policyService;

	@PostMapping("/purchase")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<PolicyResponse>> purchase(@Valid @RequestBody PolicyPurchaseRequest req,
			Authentication auth) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Policy purchased successfully",
				policyService.purchasePolicy(req, auth.getName())));
	}

	@PostMapping("/issue")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<PolicyResponse>> issue(@Valid @RequestBody PolicyIssueRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Policy issued successfully", policyService.issuePolicy(req)));
	}

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

	@GetMapping("/customer/{customerId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<Page<PolicyResponse>>> getByCustomer(@PathVariable Long customerId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(ApiResponse.success("Customer policies", policyService
				.getPoliciesByCustomer(customerId, PageRequest.of(page, size, Sort.by("createdAt").descending()))));
	}

	@GetMapping("/{policyId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<PolicyResponse>> getById(@PathVariable Long policyId) {
		return ResponseEntity.ok(ApiResponse.success("Policy details", policyService.getPolicyById(policyId)));
	}

	@PatchMapping("/{policyId}/cancel")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<PolicyResponse>> cancel(@PathVariable Long policyId, Authentication auth) {
		return ResponseEntity
				.ok(ApiResponse.success("Policy cancelled", policyService.cancelPolicy(policyId, auth.getName())));
	}
}