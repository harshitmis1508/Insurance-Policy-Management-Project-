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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harshit.monocept.dto.request.CustomerRequest;
import com.harshit.monocept.dto.response.ApiResponse;
import com.harshit.monocept.dto.response.CustomerResponse;
import com.harshit.monocept.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	// SRS FR-CUS-001: Customer profile banaye
	// Authentication = Spring Security se current logged-in user
	@PostMapping("/profile")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<CustomerResponse>> createProfile(@Valid @RequestBody CustomerRequest req,
			Authentication auth) {
		CustomerResponse res = customerService.createProfile(req, auth.getName()); // auth.getName() = email from JWT
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Profile created", res));
	}

	// SRS FR-CUS-002
	@PutMapping("/profile")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<CustomerResponse>> updateProfile(@Valid @RequestBody CustomerRequest req,
			Authentication auth) {
		return ResponseEntity
				.ok(ApiResponse.success("Profile updated", customerService.updateProfile(req, auth.getName())));
	}

	// SRS FR-CUS-003
	@GetMapping("/profile")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<CustomerResponse>> getMyProfile(Authentication auth) {
		return ResponseEntity.ok(ApiResponse.success("Profile fetched", customerService.getMyProfile(auth.getName())));
	}

	// SRS FR-CUS-004: Admin/Agent kisi bhi customer ko dekhe
	@GetMapping("/{customerId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable Long customerId) {
		return ResponseEntity.ok(ApiResponse.success("Customer fetched", customerService.getCustomerById(customerId)));
	}

	// SRS FR-CUS-006: Paginated list - admin/agent only
	@GetMapping
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getAll(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		// SRS PAGRUL-005: max page size 100
		if (size > 100)
			size = 100;

		Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		Pageable pageable = PageRequest.of(page, size, sort);
		return ResponseEntity.ok(ApiResponse.success("Customers fetched", customerService.getAllCustomers(pageable)));
	}
}