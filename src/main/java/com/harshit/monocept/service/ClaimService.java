package com.harshit.monocept.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harshit.monocept.dto.request.ClaimDecisionRequest;
import com.harshit.monocept.dto.request.ClaimRequest;
import com.harshit.monocept.dto.request.ClaimReviewRequest;
import com.harshit.monocept.dto.response.ClaimHistoryResponse;
import com.harshit.monocept.dto.response.ClaimResponse;
import com.harshit.monocept.entity.Claim;
import com.harshit.monocept.entity.ClaimDocument;
import com.harshit.monocept.entity.ClaimStatusHistory;
import com.harshit.monocept.entity.Customer;
import com.harshit.monocept.entity.Policy;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.enums.ClaimStatus;
import com.harshit.monocept.enums.PolicyStatus;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.exception.ResourceNotFoundException;
import com.harshit.monocept.repository.ClaimDocumentRepository;
import com.harshit.monocept.repository.ClaimRepository;
import com.harshit.monocept.repository.ClaimStatusHistoryRepository;
import com.harshit.monocept.repository.CustomerRepository;
import com.harshit.monocept.repository.PolicyRepository;
import com.harshit.monocept.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaimService {

	// SRS LOG-009 to LOG-013
	private static final Logger log = LoggerFactory.getLogger(ClaimService.class);

	private final ClaimRepository claimRepository;
	private final ClaimDocumentRepository documentRepository;
	private final ClaimStatusHistoryRepository historyRepository;
	private final PolicyRepository policyRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;

	@Transactional
	public ClaimResponse submitClaim(ClaimRequest req, String email) {
		log.info("Claim submission attempt: email={}, policyId={}", email, req.getPolicyId());

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		Policy policy = policyRepository.findById(req.getPolicyId())
				.orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + req.getPolicyId()));

		if (!policy.getCustomer().getId().equals(customer.getId())) {
			log.warn("Customer {} trying to claim on another customer's policy: {}", email, req.getPolicyId());
			throw new BusinessRuleException("You can only raise claims for your own policies");
		}

		if (policy.getStatus() != PolicyStatus.ACTIVE) {
			// SRS LOG-015: Business rule violation
			log.warn("Claim on non-ACTIVE policy: policyId={}, status={}", req.getPolicyId(), policy.getStatus());
			throw new BusinessRuleException(
					"Claims can only be raised on ACTIVE policies. Current status: " + policy.getStatus());
		}

		if (req.getClaimAmount().compareTo(policy.getPlan().getCoverageAmount()) > 0) {
			log.warn("Claim amount {} exceeds coverage {} for policyId={}", req.getClaimAmount(),
					policy.getPlan().getCoverageAmount(), req.getPolicyId());
			throw new BusinessRuleException(
					"Claim amount cannot exceed policy coverage amount of " + policy.getPlan().getCoverageAmount());
		}

		Claim claim = Claim.builder().claimNumber(generateClaimNumber()).policy(policy)
				.claimAmount(req.getClaimAmount()).claimReason(req.getClaimReason()).incidentDate(req.getIncidentDate())
				.claimStatus(ClaimStatus.SUBMITTED).build();

		Claim saved = claimRepository.save(claim);

		List<ClaimDocument> docs = req.getDocuments().stream()
				.map(d -> ClaimDocument.builder().claim(saved).documentName(d.getDocumentName())
						.documentType(d.getDocumentType()).documentReference(d.getDocumentReference()).build())
				.collect(Collectors.toList());
		documentRepository.saveAll(docs);

		recordHistory(saved, null, ClaimStatus.SUBMITTED, "Claim submitted by customer", user);

		// SRS LOG-009: Claim submission log
		log.info("Claim submitted: claimNumber={}, policyId={}, amount={}", saved.getClaimNumber(), req.getPolicyId(),
				req.getClaimAmount());

		return mapToResponse(saved);
	}

	@Transactional
	public ClaimResponse reviewClaim(Long claimId, ClaimReviewRequest req, String email) {
		log.info("Claim review attempt: claimId={}, agent={}, status={}", claimId, email, req.getRecommendedStatus());

		User agent = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Claim claim = claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

		validateNotFinal(claim.getClaimStatus());

		ClaimStatus allowed = req.getRecommendedStatus();
		if (allowed != ClaimStatus.UNDER_REVIEW && allowed != ClaimStatus.RECOMMENDED_FOR_APPROVAL
				&& allowed != ClaimStatus.RECOMMENDED_FOR_REJECTION) {
			log.warn("Agent {} attempted invalid status: {}", email, allowed);
			throw new BusinessRuleException(
					"Agent can only set: UNDER_REVIEW, RECOMMENDED_FOR_APPROVAL, " + "RECOMMENDED_FOR_REJECTION");
		}

		validateTransition(claim.getClaimStatus(), allowed);

		ClaimStatus previous = claim.getClaimStatus();
		claim.setClaimStatus(allowed);
		claim.setAgentRemarks(req.getRemarks());

		Claim updated = claimRepository.save(claim);
		recordHistory(updated, previous, allowed, req.getRemarks(), agent);

		// SRS LOG-010/011: Claim review/recommendation log
		if (allowed == ClaimStatus.UNDER_REVIEW) {
			log.info("Claim taken under review: claimId={}, agent={}", claimId, email);
		} else {
			log.info("Claim recommendation: claimId={}, status={}, agent={}", claimId, allowed, email);
		}

		return mapToResponse(updated);
	}

	@Transactional
	public ClaimResponse decideClaim(Long claimId, ClaimDecisionRequest req, String email) {
		log.info("Claim decision attempt: claimId={}, admin={}, decision={}", claimId, email, req.getFinalStatus());

		User admin = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Claim claim = claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

		validateNotFinal(claim.getClaimStatus());

		ClaimStatus decision = req.getFinalStatus();
		if (decision != ClaimStatus.APPROVED && decision != ClaimStatus.REJECTED) {
			log.warn("Admin {} attempted invalid final status: {}", email, decision);
			throw new BusinessRuleException("Admin can only set: APPROVED or REJECTED");
		}

		ClaimStatus previous = claim.getClaimStatus();
		claim.setClaimStatus(decision);
		claim.setAdminRemarks(req.getRemarks());

		Claim updated = claimRepository.save(claim);
		recordHistory(updated, previous, decision, req.getRemarks(), admin);

		// SRS LOG-012/013: Final approval/rejection
		if (decision == ClaimStatus.APPROVED) {
			log.info("Claim APPROVED: claimId={}, claimNumber={}, admin={}", claimId, claim.getClaimNumber(), email);
		} else {
			log.info("Claim REJECTED: claimId={}, claimNumber={}, admin={}", claimId, claim.getClaimNumber(), email);
		}

		return mapToResponse(updated);
	}

	public Page<ClaimResponse> getMyClaims(String email, Pageable pageable) {
		log.debug("Fetching claims for: {}", email);

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		Customer customer = customerRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		return claimRepository.findByPolicyCustomerId(customer.getId(), pageable).map(this::mapToResponse);
	}

	public Page<ClaimResponse> getAllClaims(Pageable pageable) {
		log.debug("Fetching all claims, page: {}", pageable.getPageNumber());
		return claimRepository.findAll(pageable).map(this::mapToResponse);
	}

	public Page<ClaimResponse> getClaimsByStatus(ClaimStatus status, Pageable pageable) {
		log.debug("Fetching claims by status: {}", status);
		return claimRepository.findByClaimStatus(status, pageable).map(this::mapToResponse);
	}

	public ClaimResponse getClaimById(Long claimId) {
		return mapToResponse(claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId)));
	}

	public Page<ClaimHistoryResponse> getClaimHistory(Long claimId, Pageable pageable) {
		log.debug("Fetching history for claimId: {}", claimId);

		claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

		return historyRepository.findByClaimId(claimId, pageable).map(this::mapHistoryToResponse);
	}

	private String generateClaimNumber() {
		String number;
		do {
			number = "CLM-" + System.currentTimeMillis() + "-"
					+ UUID.randomUUID().toString().substring(0, 4).toUpperCase();
		} while (claimRepository.existsByClaimNumber(number));
		return number;
	}

	private void validateNotFinal(ClaimStatus status) {
		if (status == ClaimStatus.APPROVED || status == ClaimStatus.REJECTED) {
			// SRS LOG-016: Attempt to modify final claim
			log.warn("Attempt to modify final claim with status: {}", status);
			throw new BusinessRuleException(
					"Cannot modify a claim that is already " + status.name() + ". SRS Rule CLM-BR-009");
		}
	}

	private void validateTransition(ClaimStatus current, ClaimStatus next) {
		boolean valid = switch (current) {
		case SUBMITTED -> next == ClaimStatus.UNDER_REVIEW;
		case UNDER_REVIEW ->
			next == ClaimStatus.RECOMMENDED_FOR_APPROVAL || next == ClaimStatus.RECOMMENDED_FOR_REJECTION;
		case RECOMMENDED_FOR_APPROVAL, RECOMMENDED_FOR_REJECTION ->
			next == ClaimStatus.APPROVED || next == ClaimStatus.REJECTED;
		default -> false;
		};

		if (!valid) {
			log.warn("Invalid claim transition: {} -> {}", current, next);
			throw new BusinessRuleException("Invalid claim status transition from " + current + " to " + next);
		}
	}

	private void recordHistory(Claim claim, ClaimStatus previous, ClaimStatus newStatus, String remarks,
			User updatedBy) {
		ClaimStatusHistory history = ClaimStatusHistory.builder().claim(claim).previousStatus(previous)
				.newStatus(newStatus).remarks(remarks).updatedBy(updatedBy).build();
		historyRepository.save(history);
		log.debug("History recorded: claimId={}, {} -> {}", claim.getId(), previous, newStatus);
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