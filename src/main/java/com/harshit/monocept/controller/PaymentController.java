package com.harshit.monocept.controller;

import com.harshit.monocept.dto.request.PaymentRequest;
import com.harshit.monocept.dto.response.ApiResponse;
import com.harshit.monocept.dto.response.PaymentResponse;
import com.harshit.monocept.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	// SRS FR-PAY-001: Customer payment kare
	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(@Valid @RequestBody PaymentRequest req,
			Authentication auth) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Payment recorded", paymentService.recordPayment(req, auth.getName())));
	}

	// SRS FR-PAY-002: Admin/Agent payment kare
	@PostMapping("/admin")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<PaymentResponse>> recordByAdmin(@Valid @RequestBody PaymentRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Payment recorded by admin/agent", paymentService.recordPaymentByAdmin(req)));
	}

	// SRS FR-PAY-008: Customer apne payments dekhe
	@GetMapping("/my/{policyId}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(@PathVariable Long policyId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			Authentication auth) {

		if (size > 100)
			size = 100;
		return ResponseEntity.ok(ApiResponse.success("My payments", paymentService.getMyPayments(policyId,
				auth.getName(), PageRequest.of(page, size, Sort.by("createdAt").descending()))));
	}

	// SRS FR-PAY-009: Admin/Agent saare payments
	@GetMapping
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAll(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		if (size > 100)
			size = 100;
		Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		return ResponseEntity.ok(
				ApiResponse.success("All payments", paymentService.getAllPayments(PageRequest.of(page, size, sort))));
	}

	// By policy
	@GetMapping("/policy/{policyId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getByPolicy(@PathVariable Long policyId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(ApiResponse.success("Policy payments", paymentService.getPaymentsByPolicy(policyId,
				PageRequest.of(page, size, Sort.by("createdAt").descending()))));
	}
}