package com.harshit.monocept.service;

import com.harshit.monocept.dto.request.ClaimDecisionRequest;
import com.harshit.monocept.dto.request.ClaimRequest;
import com.harshit.monocept.dto.request.ClaimReviewRequest;
import com.harshit.monocept.dto.response.ClaimHistoryResponse;
import com.harshit.monocept.dto.response.ClaimResponse;
import com.harshit.monocept.entity.*;
import com.harshit.monocept.enums.ClaimStatus;
import com.harshit.monocept.enums.PolicyStatus;
import com.harshit.monocept.exception.*;
import com.harshit.monocept.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimService {

	private final ClaimRepository claimRepository;
	private final ClaimDocumentRepository documentRepository;
	private final ClaimStatusHistoryRepository historyRepository;
	private final PolicyRepository policyRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;
	

	// SRS FR-CLM-001: Customer claim submit kare
	@Transactional
	public ClaimResponse submitClaim(ClaimRequest req, String email) {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		Policy policy = policyRepository.findById(req.getPolicyId())
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + req.getPolicyId()));

		// SRS CLM-BR-006: Customer sirf apni policy pe claim kare
		if (!policy.getCustomer().getId().equals(customer.getId()))
			throw new BusinessRuleException("You can only raise claims for your own policies");

		// SRS CLM-BR-001/010: Sirf ACTIVE policy pe claim
		if (policy.getStatus() != PolicyStatus.ACTIVE)
			throw new BusinessRuleException(
					"Claims can only be raised on ACTIVE policies. Current status: " + policy.getStatus());

		// SRS CLM-BR-004: Claim amount > coverage nahi
		if (req.getClaimAmount().compareTo(policy.getPlan().getCoverageAmount()) > 0)
			throw new BusinessRuleException(
					"Claim amount cannot exceed policy coverage amount of " + policy.getPlan().getCoverageAmount());

		// SRS CLM-BR-003: amount > 0 (validation mein hai bhi)

		Claim claim = Claim.builder().claimNumber(generateClaimNumber()).policy(policy)
				.claimAmount(req.getClaimAmount()).claimReason(req.getClaimReason()).incidentDate(req.getIncidentDate())
				.claimStatus(ClaimStatus.SUBMITTED) // SRS CLC-RUL-001
				.build();

		Claim saved = claimRepository.save(claim);

		// SRS DOC-BR-001: Documents save karo
		List<ClaimDocument> docs = req.getDocuments().stream()
				.map(d -> ClaimDocument.builder().claim(saved).documentName(d.getDocumentName())
						.documentType(d.getDocumentType()).documentReference(d.getDocumentReference()).build())
				.collect(Collectors.toList());
		documentRepository.saveAll(docs);

		// SRS HIS-BR-001: History record karo
		recordHistory(saved, null, ClaimStatus.SUBMITTED, "Claim submitted by customer", user);

		return mapToResponse(saved);
	}

	// SRS FR-CLM-006/007: Agent review + recommend
	@Transactional
	public ClaimResponse reviewClaim(Long claimId, ClaimReviewRequest req, String email) {
		User agent = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Claim claim = claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

		// SRS CLM-BR-009: Approved/Rejected modify nahi
		validateNotFinal(claim.getClaimStatus());

		// SRS CLC-RUL-002/003: Agent valid transitions
		ClaimStatus allowed = req.getRecommendedStatus();
		if (allowed != ClaimStatus.UNDER_REVIEW && allowed != ClaimStatus.RECOMMENDED_FOR_APPROVAL
				&& allowed != ClaimStatus.RECOMMENDED_FOR_REJECTION)
			throw new BusinessRuleException(
					"Agent can only set: UNDER_REVIEW, RECOMMENDED_FOR_APPROVAL, " + "RECOMMENDED_FOR_REJECTION");

		// CLC-RUL-008: Invalid transition check
		validateTransition(claim.getClaimStatus(), allowed, "AGENT");

		ClaimStatus previous = claim.getClaimStatus();
		claim.setClaimStatus(allowed);
		claim.setAgentRemarks(req.getRemarks());

		Claim updated = claimRepository.save(claim);

		// SRS HIS-BR-001: History
		recordHistory(updated, previous, allowed, req.getRemarks(), agent);

		return mapToResponse(updated);
	}

	// SRS FR-CLM-008: Admin final decision
	@Transactional
	public ClaimResponse decideClaim(Long claimId, ClaimDecisionRequest req, String email) {
		User admin = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Claim claim = claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

		// SRS CLM-BR-009: Approved/Rejected modify nahi
		validateNotFinal(claim.getClaimStatus());

		// SRS CLC-RUL-004: Admin sirf APPROVED/REJECTED set kare
		ClaimStatus decision = req.getFinalStatus();
		if (decision != ClaimStatus.APPROVED && decision != ClaimStatus.REJECTED)
			throw new BusinessRuleException("Admin can only set: APPROVED or REJECTED");

		ClaimStatus previous = claim.getClaimStatus();
		claim.setClaimStatus(decision);
		claim.setAdminRemarks(req.getRemarks());

		Claim updated = claimRepository.save(claim);

		// SRS HIS-BR-001
		recordHistory(updated, previous, decision, req.getRemarks(), admin);

		return mapToResponse(updated);
	}

	// SRS FR-CLM-005: Customer apne claims dekhe
	public Page<ClaimResponse> getMyClaims(String email, Pageable pageable) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		return claimRepository.findByPolicyCustomerId(customer.getId(), pageable).map(this::mapToResponse);
	}

	// SRS FR-CLM-011: Admin/Agent saare claims
	public Page<ClaimResponse> getAllClaims(Pageable pageable) {
		return claimRepository.findAll(pageable).map(this::mapToResponse);
	}

	// Filter by status
	public Page<ClaimResponse> getClaimsByStatus(ClaimStatus status, Pageable pageable) {
		return claimRepository.findByClaimStatus(status, pageable).map(this::mapToResponse);
	}

	// Single claim
	public ClaimResponse getClaimById(Long claimId) {
		return mapToResponse(claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId)));
	}

	// SRS: Claim history
	public Page<ClaimHistoryResponse> getClaimHistory(Long claimId, Pageable pageable) {
		claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

		return historyRepository.findByClaimId(claimId, pageable).map(this::mapHistoryToResponse);
	}

	// ---- Private Helpers ----

	// SRS CLM-BR-002: Unique claim number
	private String generateClaimNumber() {
		String number;
		do {
			number = "CLM-" + System.currentTimeMillis() + "-"
					+ UUID.randomUUID().toString().substring(0, 4).toUpperCase();
		} while (claimRepository.existsByClaimNumber(number));
		return number;
	}

	// SRS CLM-BR-009: APPROVED/REJECTED ke baad koi change nahi
	private void validateNotFinal(ClaimStatus status) {
		if (status == ClaimStatus.APPROVED || status == ClaimStatus.REJECTED)
			throw new BusinessRuleException(
					"Cannot modify a claim that is already " + status.name() + ". SRS Rule CLM-BR-009");
	}

	// SRS CLC-RUL-008: Invalid state transitions block karo
	private void validateTransition(ClaimStatus current, ClaimStatus next, String role) {
		boolean valid = switch (current) {
		case SUBMITTED -> next == ClaimStatus.UNDER_REVIEW;
		case UNDER_REVIEW ->
			next == ClaimStatus.RECOMMENDED_FOR_APPROVAL || next == ClaimStatus.RECOMMENDED_FOR_REJECTION;
		case RECOMMENDED_FOR_APPROVAL, RECOMMENDED_FOR_REJECTION ->
			next == ClaimStatus.APPROVED || next == ClaimStatus.REJECTED;
		default -> false;
		};

		if (!valid)
			throw new BusinessRuleException("Invalid claim status transition from " + current + " to " + next);
	}

	// SRS HIS-BR-001/002/003: History record — insert only, no update
	private void recordHistory(Claim claim, ClaimStatus previous, ClaimStatus newStatus, String remarks,
			User updatedBy) {
		ClaimStatusHistory history = ClaimStatusHistory.builder().claim(claim).previousStatus(previous)
				.newStatus(newStatus).remarks(remarks).updatedBy(updatedBy).build();
		historyRepository.save(history);
	}

	private ClaimResponse mapToResponse(Claim c) {
		List<ClaimDocument> docs = documentRepository.findByClaimId(c.getId());

		List<ClaimResponse.DocumentResponse> docResponses = docs.stream()
				.map(d -> ClaimResponse.DocumentResponse.builder().documentId(d.getId())
						.documentName(d.getDocumentName()).documentType(d.getDocumentType())
						.documentReference(d.getDocumentReference()).build())
				.collect(Collectors.toList());

		return ClaimResponse.builder().claimId(c.getId()).claimNumber(c.getClaimNumber())
				.policyId(c.getPolicy().getId()).policyNumber(c.getPolicy().getPolicyNumber())
				.customerName(c.getPolicy().getCustomer().getUser().getFullName()).claimAmount(c.getClaimAmount())
				.claimReason(c.getClaimReason()).incidentDate(c.getIncidentDate()).claimStatus(c.getClaimStatus())
				.agentRemarks(c.getAgentRemarks()).adminRemarks(c.getAdminRemarks()).documents(docResponses)
				.createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
	}

	private ClaimHistoryResponse mapHistoryToResponse(ClaimStatusHistory h) {
		return ClaimHistoryResponse.builder().historyId(h.getId()).claimId(h.getClaim().getId())
				.claimNumber(h.getClaim().getClaimNumber()).previousStatus(h.getPreviousStatus())
				.newStatus(h.getNewStatus()).remarks(h.getRemarks()).updatedByName(h.getUpdatedBy().getFullName())
				.updatedByRole(h.getUpdatedBy().getRole().name()).updatedAt(h.getUpdatedAt()).build();
	}
}