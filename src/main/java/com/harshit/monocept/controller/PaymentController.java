package com.harshit.monocept.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harshit.monocept.dto.request.PaymentRequest;
import com.harshit.monocept.dto.response.ApiResponse;
import com.harshit.monocept.dto.response.PaymentResponse;
import com.harshit.monocept.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(@Valid @RequestBody PaymentRequest req,
			Authentication auth) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Payment recorded", paymentService.recordPayment(req, auth.getName())));
	}

	@PostMapping("/admin")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<PaymentResponse>> recordByAdmin(@Valid @RequestBody PaymentRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Payment recorded by admin/agent", paymentService.recordPaymentByAdmin(req)));
	}

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

	@GetMapping("/policy/{policyId}")
	@PreAuthorize("hasRole('ADMIN') or hasRole('AGENT')")
	public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getByPolicy(@PathVariable Long policyId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		return ResponseEntity.ok(ApiResponse.success("Policy payments", paymentService.getPaymentsByPolicy(policyId,
				PageRequest.of(page, size, Sort.by("createdAt").descending()))));
	}
}