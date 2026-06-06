package com.harshit.monocept.service;

import com.harshit.monocept.dto.request.PolicyIssueRequest;
import com.harshit.monocept.dto.request.PolicyPurchaseRequest;
import com.harshit.monocept.dto.response.PolicyResponse;
import com.harshit.monocept.entity.*;
import com.harshit.monocept.enums.PolicyStatus;
import com.harshit.monocept.exception.*;
import com.harshit.monocept.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {

	private final PolicyRepository policyRepository;
	private final PolicyPlanRepository planRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;

	// SRS FR-POL-001: Customer apni policy purchase kare
	@Transactional
	public PolicyResponse purchasePolicy(PolicyPurchaseRequest req, String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// SRS CUS-BR-003: Policy se pehle profile hona chahiye
		Customer customer = customerRepository.findByUserId(user.getId()).orElseThrow(
				() -> new BusinessRuleException("Please complete your profile before purchasing a policy"));

		PolicyPlan plan = planRepository.findById(req.getPlanId())
				.orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + req.getPlanId()));

		// SRS PLN-BR-005: Sirf active plan purchase ho sakta hai
		if (!plan.getIsActive())
			throw new BusinessRuleException("Cannot purchase an inactive plan");

		// SRS PRD-BR-002: Product active hona chahiye
		if (!plan.getProduct().getIsActive())
			throw new BusinessRuleException("Cannot purchase plan of an inactive product");

		return mapToResponse(policyRepository.save(buildPolicy(customer, plan, req.getStartDate())));
	}

	// SRS FR-POL-002: Agent/Admin customer ke liye policy issue kare
	@Transactional
	public PolicyResponse issuePolicy(PolicyIssueRequest req) {

		Customer customer = customerRepository.findById(req.getCustomerId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + req.getCustomerId()));

		PolicyPlan plan = planRepository.findById(req.getPlanId())
				.orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + req.getPlanId()));

		if (!plan.getIsActive())
			throw new BusinessRuleException("Cannot issue an inactive plan");

		if (!plan.getProduct().getIsActive())
			throw new BusinessRuleException("Cannot issue plan of an inactive product");

		return mapToResponse(policyRepository.save(buildPolicy(customer, plan, req.getStartDate())));
	}

	// SRS FR-POL-006: Customer sirf apni policies dekhe
	public Page<PolicyResponse> getMyPolicies(String email, Pageable pageable) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		return policyRepository.findByCustomerId(customer.getId(), pageable).map(this::mapToResponse);
	}

	// SRS FR-POL-007: Admin/Agent saari policies dekhe
	public Page<PolicyResponse> getAllPolicies(Pageable pageable) {
		return policyRepository.findAll(pageable).map(this::mapToResponse);
	}

	// SRS FR-POL-007: Customer ke policies by customerId
	public Page<PolicyResponse> getPoliciesByCustomer(Long customerId, Pageable pageable) {
		customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

		return policyRepository.findByCustomerId(customerId, pageable).map(this::mapToResponse);
	}

	// SRS FR-POL-008: Admin/Agent policy cancel kare
	@Transactional
	public PolicyResponse cancelPolicy(Long policyId, String email) {

		Policy policy = policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

		// SRS PLC-RUL-007: Cancelled policy reactivate nahi hoti
		if (policy.getStatus() == PolicyStatus.CANCELLED)
			throw new BusinessRuleException("Policy is already cancelled");

		if (policy.getStatus() == PolicyStatus.EXPIRED)
			throw new BusinessRuleException("Expired policy cannot be cancelled");

		policy.setStatus(PolicyStatus.CANCELLED);
		return mapToResponse(policyRepository.save(policy));
	}

	// SRS FR-POL-010: Expired check
	public PolicyResponse getPolicyById(Long policyId) {
		Policy policy = policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

		// Auto expire check
		if (policy.getStatus() == PolicyStatus.ACTIVE && policy.getEndDate().isBefore(LocalDate.now())) {
			policy.setStatus(PolicyStatus.EXPIRED);
			policyRepository.save(policy);
		}

		return mapToResponse(policy);
	}

	// ---- Private Helpers ----

	// SRS POL-BR-003: Unique policy number generate
	private String generatePolicyNumber() {
		String number;
		do {
			number = "POL-" + System.currentTimeMillis() + "-"
					+ UUID.randomUUID().toString().substring(0, 4).toUpperCase();
		} while (policyRepository.existsByPolicyNumber(number));
		return number;
	}

	private Policy buildPolicy(Customer customer, PolicyPlan plan, LocalDate startDate) {
		// SRS POL-BR-001/002/003/004
		return Policy.builder().policyNumber(generatePolicyNumber()).customer(customer).plan(plan).startDate(startDate)
				// SRS: end date = start + duration years
				.endDate(startDate.plusYears(plan.getDurationYears())).status(PolicyStatus.PENDING_PAYMENT) // SRS
																											// PLC-RUL-001
				.build();
	}

	public PolicyResponse mapToResponse(Policy p) {
		return PolicyResponse.builder().policyId(p.getId()).policyNumber(p.getPolicyNumber())
				.customerId(p.getCustomer().getId()).customerName(p.getCustomer().getUser().getFullName())
				.planId(p.getPlan().getId()).planName(p.getPlan().getPlanName())
				.productType(p.getPlan().getProduct().getProductType()).coverageAmount(p.getPlan().getCoverageAmount())
				.premiumAmount(p.getPlan().getPremiumAmount()).premiumType(p.getPlan().getPremiumType())
				.startDate(p.getStartDate()).endDate(p.getEndDate()).status(p.getStatus())
				.totalPremiumPaid(p.getTotalPremiumPaid()).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
				.build();
	}
}