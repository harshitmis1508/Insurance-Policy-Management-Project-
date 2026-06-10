package com.harshit.monocept.service;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harshit.monocept.dto.request.PolicyIssueRequest;
import com.harshit.monocept.dto.request.PolicyPurchaseRequest;
import com.harshit.monocept.dto.response.PolicyResponse;
import com.harshit.monocept.entity.Customer;
import com.harshit.monocept.entity.Policy;
import com.harshit.monocept.entity.PolicyPlan;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.enums.PolicyStatus;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.exception.ResourceNotFoundException;
import com.harshit.monocept.repository.CustomerRepository;
import com.harshit.monocept.repository.PolicyPlanRepository;
import com.harshit.monocept.repository.PolicyRepository;
import com.harshit.monocept.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyService {


	private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

	private final PolicyRepository policyRepository;
	private final PolicyPlanRepository planRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;

	@Transactional
	public PolicyResponse purchasePolicy(PolicyPurchaseRequest req, String email) {
		log.info("Policy purchase attempt: email={}, planId={}", email, req.getPlanId());

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId()).orElseThrow(() -> {
			log.warn("Policy purchase without profile: email={}", email);
			return new BusinessRuleException("Please complete your profile before purchasing a policy");
		});

		PolicyPlan plan = planRepository.findById(req.getPlanId())
				.orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + req.getPlanId()));

		if (!plan.getIsActive()) {
			log.warn("Purchase attempt on inactive plan: planId={}", req.getPlanId());
			throw new BusinessRuleException("Cannot purchase an inactive plan");
		}

		if (!plan.getProduct().getIsActive()) {
			log.warn("Purchase attempt on inactive product: productId={}", plan.getProduct().getId());
			throw new BusinessRuleException("Cannot purchase plan of an inactive product");
		}

		Policy policy = policyRepository.save(buildPolicy(customer, plan, req.getStartDate()));

		
		log.info("Policy purchased: policyNumber={}, customer={}, planId={}", policy.getPolicyNumber(), email,
				req.getPlanId());

		return mapToResponse(policy);
	}

	@Transactional
	public PolicyResponse issuePolicy(PolicyIssueRequest req) {
		log.info("Policy issue attempt: customerId={}, planId={}", req.getCustomerId(), req.getPlanId());

		Customer customer = customerRepository.findById(req.getCustomerId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + req.getCustomerId()));

		PolicyPlan plan = planRepository.findById(req.getPlanId())
				.orElseThrow(() -> new ResourceNotFoundException("Plan not found with id: " + req.getPlanId()));

		if (!plan.getIsActive()) {
			log.warn("Issue attempt on inactive plan: planId={}", req.getPlanId());
			throw new BusinessRuleException("Cannot issue an inactive plan");
		}

		if (!plan.getProduct().getIsActive()) {
			log.warn("Issue attempt on inactive product: productId={}", plan.getProduct().getId());
			throw new BusinessRuleException("Cannot issue plan of an inactive product");
		}

		Policy policy = policyRepository.save(buildPolicy(customer, plan, req.getStartDate()));

		
		log.info("Policy issued: policyNumber={}, customerId={}, planId={}", policy.getPolicyNumber(),
				req.getCustomerId(), req.getPlanId());

		return mapToResponse(policy);
	}

	public Page<PolicyResponse> getMyPolicies(String email, Pageable pageable) {
		log.debug("Fetching policies for: {}", email);

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		return policyRepository.findByCustomerId(customer.getId(), pageable).map(this::mapToResponse);
	}

	public Page<PolicyResponse> getAllPolicies(Pageable pageable) {
		log.debug("Fetching all policies, page: {}", pageable.getPageNumber());
		return policyRepository.findAll(pageable).map(this::mapToResponse);
	}

	public Page<PolicyResponse> getPoliciesByCustomer(Long customerId, Pageable pageable) {
		log.debug("Fetching policies for customerId: {}", customerId);

		customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

		return policyRepository.findByCustomerId(customerId, pageable).map(this::mapToResponse);
	}

	@Transactional
	public PolicyResponse cancelPolicy(Long policyId, String email) {
		log.info("Policy cancel attempt: policyId={}, by={}", policyId, email);

		Policy policy = policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

		if (policy.getStatus() == PolicyStatus.CANCELLED) {
			log.warn("Already cancelled policy: policyId={}", policyId);
			throw new BusinessRuleException("Policy is already cancelled");
		}

		if (policy.getStatus() == PolicyStatus.EXPIRED) {
			log.warn("Cancel attempt on expired policy: policyId={}", policyId);
			throw new BusinessRuleException("Expired policy cannot be cancelled");
		}

		policy.setStatus(PolicyStatus.CANCELLED);
		Policy saved = policyRepository.save(policy);
		log.info("Policy cancelled: policyNumber={}", saved.getPolicyNumber());

		return mapToResponse(saved);
	}

	public PolicyResponse getPolicyById(Long policyId) {
		Policy policy = policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

		
		if (policy.getStatus() == PolicyStatus.ACTIVE && policy.getEndDate().isBefore(LocalDate.now())) {
			policy.setStatus(PolicyStatus.EXPIRED);
			policyRepository.save(policy);
			log.info("Policy auto-expired: policyNumber={}", policy.getPolicyNumber());
		}

		return mapToResponse(policy);
	}

	private String generatePolicyNumber() {
		String number;
		do {
			number = "POL-" + System.currentTimeMillis() + "-"
					+ UUID.randomUUID().toString().substring(0, 4).toUpperCase();
		} while (policyRepository.existsByPolicyNumber(number));
		return number;
	}

	private Policy buildPolicy(Customer customer, PolicyPlan plan, LocalDate startDate) {
		return Policy.builder().policyNumber(generatePolicyNumber()).customer(customer).plan(plan).startDate(startDate)
				.endDate(startDate.plusYears(plan.getDurationYears())).status(PolicyStatus.PENDING_PAYMENT).build();
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