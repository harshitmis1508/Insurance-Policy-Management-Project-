package com.harshit.monocept.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.harshit.monocept.dto.request.PlanRequest;
import com.harshit.monocept.dto.response.PlanResponse;
import com.harshit.monocept.entity.InsuranceProduct;
import com.harshit.monocept.entity.PolicyPlan;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.exception.ResourceNotFoundException;
import com.harshit.monocept.repository.PolicyPlanRepository;
import com.harshit.monocept.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyPlanService {

	private final PolicyPlanRepository planRepository;
	private final ProductRepository productRepository;

	// SRS FR-PLN-001
	public PlanResponse createPlan(PlanRequest req) {

		InsuranceProduct product = productRepository.findById(req.getProductId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + req.getProductId()));

		// SRS PRD-BR-002: inactive product pe plan nahi bana sakte
		if (!product.getIsActive())
			throw new BusinessRuleException("Cannot create plan for inactive product");

		// SRS PLN-BR-004: coverage > premium
		if (req.getCoverageAmount().compareTo(req.getPremiumAmount()) <= 0)
			throw new BusinessRuleException("Coverage amount must be greater than premium amount");

		PolicyPlan plan = PolicyPlan.builder().product(product).planName(req.getPlanName())
				.coverageAmount(req.getCoverageAmount()).premiumAmount(req.getPremiumAmount())
				.premiumType(req.getPremiumType()).durationYears(req.getDurationYears())
				.termsAndConditions(req.getTermsAndConditions())
				.isActive(req.getIsActive() != null ? req.getIsActive() : true).build();

		return mapToResponse(planRepository.save(plan));
	}

	// SRS FR-PLN-002
	public PlanResponse updatePlan(Long id, PlanRequest req) {

		PolicyPlan plan = planRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

		InsuranceProduct product = productRepository.findById(req.getProductId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found"));

		if (!product.getIsActive())
			throw new BusinessRuleException("Cannot link plan to inactive product");

		// SRS PLN-BR-004
		if (req.getCoverageAmount().compareTo(req.getPremiumAmount()) <= 0)
			throw new BusinessRuleException("Coverage amount must be greater than premium amount");

		plan.setProduct(product);
		plan.setPlanName(req.getPlanName());
		plan.setCoverageAmount(req.getCoverageAmount());
		plan.setPremiumAmount(req.getPremiumAmount());
		plan.setPremiumType(req.getPremiumType());
		plan.setDurationYears(req.getDurationYears());
		plan.setTermsAndConditions(req.getTermsAndConditions());
		if (req.getIsActive() != null)
			plan.setIsActive(req.getIsActive());

		return mapToResponse(planRepository.save(plan));
	}

	// SRS FR-PLN-003: Deactivate — hard delete nahi (PLN-BR-006)
	public PlanResponse deactivatePlan(Long id) {
		PolicyPlan plan = planRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + id));

		if (!plan.getIsActive())
			throw new BusinessRuleException("Plan is already inactive");

		plan.setIsActive(false);
		return mapToResponse(planRepository.save(plan));
	}

	// SRS FR-PLN-004: Sabhi active plans
	public Page<PlanResponse> getActivePlans(Pageable pageable) {
		return planRepository.findByIsActiveTrue(pageable).map(this::mapToResponse);
	}

	// SRS FR-PLN-005: Ek product ke active plans
	public Page<PlanResponse> getPlansByProduct(Long productId, Pageable pageable) {
		// product exist karta hai?
		productRepository.findById(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

		return planRepository.findByProductIdAndIsActiveTrue(productId, pageable).map(this::mapToResponse);
	}

	private PlanResponse mapToResponse(PolicyPlan p) {
		return PlanResponse.builder().planId(p.getId()).productId(p.getProduct().getId())
				.productName(p.getProduct().getProductName()).productType(p.getProduct().getProductType())
				.planName(p.getPlanName()).coverageAmount(p.getCoverageAmount()).premiumAmount(p.getPremiumAmount())
				.premiumType(p.getPremiumType()).durationYears(p.getDurationYears())
				.termsAndConditions(p.getTermsAndConditions()).isActive(p.getIsActive()).createdAt(p.getCreatedAt())
				.updatedAt(p.getUpdatedAt()).build();
	}
}