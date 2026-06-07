package com.harshit.monocept.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harshit.monocept.dto.request.PlanRequest;
import com.harshit.monocept.dto.response.ApiResponse;
import com.harshit.monocept.dto.response.PlanResponse;
import com.harshit.monocept.service.PolicyPlanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PolicyPlanController {

	private final PolicyPlanService planService;

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<PlanResponse>> create(@Valid @RequestBody PlanRequest req) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Plan created", planService.createPlan(req)));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<PlanResponse>> update(@PathVariable Long id,
			@Valid @RequestBody PlanRequest req) {
		return ResponseEntity.ok(ApiResponse.success("Plan updated", planService.updatePlan(id, req)));
	}

	@PatchMapping("/{id}/deactivate")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<PlanResponse>> deactivate(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Plan deactivated", planService.deactivatePlan(id)));
	}

	@GetMapping("/active")
	public ResponseEntity<ApiResponse<Page<PlanResponse>>> getActive(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String direction) {

		if (size > 100)
			size = 100;
		Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		return ResponseEntity
				.ok(ApiResponse.success("Active plans", planService.getActivePlans(PageRequest.of(page, size, sort))));
	}

	@GetMapping("/product/{productId}")
	public ResponseEntity<ApiResponse<Page<PlanResponse>>> getByProduct(@PathVariable Long productId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		if (size > 100)
			size = 100;
		return ResponseEntity.ok(ApiResponse.success("Plans by product", planService.getPlansByProduct(productId,
				PageRequest.of(page, size, Sort.by("createdAt").descending()))));
	}
}