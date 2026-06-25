package com.harshit.monocept.service;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harshit.monocept.dto.request.PaymentRequest;
import com.harshit.monocept.dto.response.PaymentResponse;
import com.harshit.monocept.entity.Customer;
import com.harshit.monocept.entity.Policy;
import com.harshit.monocept.entity.PremiumPayment;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.enums.PaymentStatus;
import com.harshit.monocept.enums.PolicyStatus;
import com.harshit.monocept.enums.PremiumType;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.exception.DuplicateResourceException;
import com.harshit.monocept.exception.ResourceNotFoundException;
import com.harshit.monocept.repository.CustomerRepository;
import com.harshit.monocept.repository.PaymentRepository;
import com.harshit.monocept.repository.PolicyRepository;
import com.harshit.monocept.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

	private final PaymentRepository paymentRepository;
	private final PolicyRepository policyRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;

	@Transactional
	public PaymentResponse recordPayment(PaymentRequest req, String email) {
		log.info("Payment attempt by customer: email={}, policyId={}, txRef={}", email, req.getPolicyId(),
				req.getTransactionReference());

		Policy policy = policyRepository.findById(req.getPolicyId())
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + req.getPolicyId()));

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		if (!policy.getCustomer().getId().equals(customer.getId())) {
			log.warn("Customer {} trying to pay for another customer's policy: {}", email, req.getPolicyId());
			throw new BusinessRuleException("You can only make payments for your own policies");
		}

		return processPayment(req, policy);
	}

	@Transactional
	public PaymentResponse recordPaymentByAdmin(PaymentRequest req) {
		log.info("Payment by admin/agent: policyId={}, txRef={}", req.getPolicyId(), req.getTransactionReference());

		Policy policy = policyRepository.findById(req.getPolicyId())
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + req.getPolicyId()));

		return processPayment(req, policy);
	}

	public Page<PaymentResponse> getMyPayments(Long policyId, String email, Pageable pageable) {
		log.debug("Fetching payments for policyId={}, email={}", policyId, email);

		Policy policy = policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		if (!policy.getCustomer().getId().equals(customer.getId())) {
			log.warn("Customer {} trying to view another customer's payments", email);
			throw new BusinessRuleException("You can only view payments for your own policies");
		}

		return paymentRepository.findByPolicyId(policyId, pageable).map(this::mapToResponse);
	}

	public Page<PaymentResponse> getAllPayments(Pageable pageable) {
		log.debug("Fetching all payments, page: {}", pageable.getPageNumber());
		return paymentRepository.findAll(pageable).map(this::mapToResponse);
	}

	public Page<PaymentResponse> getPaymentsByPolicy(Long policyId, Pageable pageable) {
		log.debug("Fetching payments for policyId: {}", policyId);

		policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

		return paymentRepository.findByPolicyId(policyId, pageable).map(this::mapToResponse);
	}

	private PaymentResponse processPayment(PaymentRequest req, Policy policy) {

		PremiumType premiumType = policy.getPlan().getPremiumType();

		if (paymentRepository.existsByTransactionReference(req.getTransactionReference())) {
			log.warn("Duplicate transaction reference: {}", req.getTransactionReference());
			throw new DuplicateResourceException(
					"Transaction reference already exists: " + req.getTransactionReference());
		}
		if (req.getAmount().compareTo(policy.getPlan().getPremiumAmount()) != 0) {
			throw new BusinessRuleException("Payment amount must match premium amount");
		}

		if (premiumType == PremiumType.ONE_TIME
				&& policy.getTotalPremiumPaid().compareTo(java.math.BigDecimal.ZERO) > 0) {

			throw new BusinessRuleException("One-time premium already paid");
		}

		if (premiumType == PremiumType.ANNUAL && policy.getPremiumsPaid() >= policy.getPlan().getDurationYears()) {

			throw new BusinessRuleException("All annual premiums already paid");
		}

		if (premiumType == PremiumType.ANNUAL && req.getPaymentStatus() == PaymentStatus.SUCCESS) {
			validateAnnualPremiumPaymentWindow(policy);
		}

		if (policy.getStatus() == PolicyStatus.CANCELLED) {
			log.warn("Payment on cancelled policy: policyId={}", policy.getId());
			throw new BusinessRuleException("Cannot make payment for a cancelled policy");
		}

		if (policy.getStatus() == PolicyStatus.EXPIRED) {
			log.warn("Payment on expired policy: policyId={}", policy.getId());
			throw new BusinessRuleException("Cannot make payment for an expired policy");
		}

		PremiumPayment payment = PremiumPayment.builder().policy(policy).amount(req.getAmount())
				.paymentMode(req.getPaymentMode()).transactionReference(req.getTransactionReference())
				.paymentStatus(req.getPaymentStatus()).build();

		PremiumPayment saved = paymentRepository.save(payment);

		log.info("Payment recorded: txRef={}, status={}, policyId={}, amount={}", req.getTransactionReference(),
				req.getPaymentStatus(), policy.getId(), req.getAmount());

		if (req.getPaymentStatus() == PaymentStatus.SUCCESS) {

			policy.setTotalPremiumPaid(policy.getTotalPremiumPaid().add(req.getAmount()));

			if (premiumType == PremiumType.ONE_TIME) {

				policy.setStatus(PolicyStatus.ACTIVE);
			}

			if (premiumType == PremiumType.ANNUAL) {

				policy.setPremiumsPaid(policy.getPremiumsPaid() + 1);

				policy.setStatus(PolicyStatus.ACTIVE);

				if (policy.getNextPremiumDueDate() == null) {
					policy.setNextPremiumDueDate(LocalDate.now().plusYears(1));
				} else {
					policy.setNextPremiumDueDate(policy.getNextPremiumDueDate().plusYears(1));
				}
			}

			policyRepository.save(policy);
		} else {
			log.warn("Payment {} for policyId={}: policy remains {}", req.getPaymentStatus(), policy.getId(),
					policy.getStatus());
		}

		return mapToResponse(saved);
	}

	private void validateAnnualPremiumPaymentWindow(Policy policy) {
		if (policy.getPremiumsPaid() == null || policy.getPremiumsPaid() == 0
				|| policy.getStatus() == PolicyStatus.PENDING_PAYMENT) {
			return;
		}

		LocalDate nextDueDate = policy.getNextPremiumDueDate();
		if (nextDueDate == null) {
			throw new BusinessRuleException("Next premium due date is not available for this annual policy");
		}

		LocalDate paymentWindowStart = nextDueDate.minusMonths(1);
		LocalDate today = LocalDate.now();

		if (today.isBefore(paymentWindowStart)) {
			throw new BusinessRuleException("Next annual premium can be paid only from " + paymentWindowStart
					+ " onwards. Your next premium due date is " + nextDueDate);
		}
	}

	private PaymentResponse mapToResponse(PremiumPayment p) {
		return PaymentResponse.builder().paymentId(p.getId()).policyId(p.getPolicy().getId())
				.policyNumber(p.getPolicy().getPolicyNumber()).amount(p.getAmount()).paymentDate(p.getPaymentDate())
				.paymentMode(p.getPaymentMode()).transactionReference(p.getTransactionReference())
				.paymentStatus(p.getPaymentStatus()).createdAt(p.getCreatedAt()).build();
	}
}