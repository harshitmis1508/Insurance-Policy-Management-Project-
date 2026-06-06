package com.harshit.monocept.service;

import com.harshit.monocept.dto.request.PaymentRequest;
import com.harshit.monocept.dto.response.PaymentResponse;
import com.harshit.monocept.entity.*;
import com.harshit.monocept.enums.PaymentStatus;
import com.harshit.monocept.enums.PolicyStatus;
import com.harshit.monocept.exception.*;
import com.harshit.monocept.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final PolicyRepository policyRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;

	// SRS FR-PAY-001: Customer apni policy ka payment kare
	@Transactional
	public PaymentResponse recordPayment(PaymentRequest req, String email) {

		Policy policy = policyRepository.findById(req.getPolicyId())
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + req.getPolicyId()));

		// SRS PAY-BR-009: Customer sirf apni policy ka payment kare
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		// Ownership check
		if (!policy.getCustomer().getId().equals(customer.getId()))
			throw new BusinessRuleException("You can only make payments for your own policies");

		return processPayment(req, policy);
	}

	// SRS FR-PAY-002: Agent/Admin kisi bhi policy ka payment kare
	@Transactional
	public PaymentResponse recordPaymentByAdmin(PaymentRequest req) {

		Policy policy = policyRepository.findById(req.getPolicyId())
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + req.getPolicyId()));

		return processPayment(req, policy);
	}

	// SRS FR-PAY-008: Customer apne payments dekhe
	public Page<PaymentResponse> getMyPayments(Long policyId, String email, Pageable pageable) {
		Policy policy = policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		// SRS PAY-BR-009: Ownership check
		if (!policy.getCustomer().getId().equals(customer.getId()))
			throw new BusinessRuleException("You can only view payments for your own policies");

		return paymentRepository.findByPolicyId(policyId, pageable).map(this::mapToResponse);
	}

	// SRS FR-PAY-009: Admin/Agent saare payments dekhe
	public Page<PaymentResponse> getAllPayments(Pageable pageable) {
		return paymentRepository.findAll(pageable).map(this::mapToResponse);
	}

	// SRS FR-PAY-009: By policy ID
	public Page<PaymentResponse> getPaymentsByPolicy(Long policyId, Pageable pageable) {
		policyRepository.findById(policyId)
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + policyId));

		return paymentRepository.findByPolicyId(policyId, pageable).map(this::mapToResponse);
	}

	// ---- Private Helpers ----

	private PaymentResponse processPayment(PaymentRequest req, Policy policy) {

		// SRS PAY-BR-003: Unique transaction reference
		if (paymentRepository.existsByTransactionReference(req.getTransactionReference()))
			throw new DuplicateResourceException(
					"Transaction reference already exists: " + req.getTransactionReference());

		// Cancelled/Expired policy ka payment nahi
		if (policy.getStatus() == PolicyStatus.CANCELLED)
			throw new BusinessRuleException("Cannot make payment for a cancelled policy");

		if (policy.getStatus() == PolicyStatus.EXPIRED)
			throw new BusinessRuleException("Cannot make payment for an expired policy");

		PremiumPayment payment = PremiumPayment.builder().policy(policy).amount(req.getAmount())
				.paymentMode(req.getPaymentMode()).transactionReference(req.getTransactionReference())
				.paymentStatus(req.getPaymentStatus()).build();

		PremiumPayment saved = paymentRepository.save(payment);

		// SRS PAY-BR-004/007: SUCCESS payment se policy activate
		if (req.getPaymentStatus() == PaymentStatus.SUCCESS) {
			policy.setTotalPremiumPaid(policy.getTotalPremiumPaid().add(req.getAmount()));

			// SRS PLC-RUL-002: required premium >= plan premium
			if (policy.getStatus() == PolicyStatus.PENDING_PAYMENT
					&& policy.getTotalPremiumPaid().compareTo(policy.getPlan().getPremiumAmount()) >= 0) {
				policy.setStatus(PolicyStatus.ACTIVE);
			}
			policyRepository.save(policy);
		}
		// SRS PAY-BR-005/006: FAILED/PENDING = policy pending rahe

		return mapToResponse(saved);
	}

	private PaymentResponse mapToResponse(PremiumPayment p) {
		return PaymentResponse.builder().paymentId(p.getId()).policyId(p.getPolicy().getId())
				.policyNumber(p.getPolicy().getPolicyNumber()).amount(p.getAmount()).paymentDate(p.getPaymentDate())
				.paymentMode(p.getPaymentMode()).transactionReference(p.getTransactionReference())
				.paymentStatus(p.getPaymentStatus()).createdAt(p.getCreatedAt()).build();
	}
}