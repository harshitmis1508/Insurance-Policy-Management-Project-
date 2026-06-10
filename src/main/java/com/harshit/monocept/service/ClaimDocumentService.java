package com.harshit.monocept.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.harshit.monocept.dto.response.DocumentUploadResponse;
import com.harshit.monocept.entity.Claim;
import com.harshit.monocept.entity.ClaimDocument;
import com.harshit.monocept.entity.Customer;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.enums.ClaimStatus;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.exception.ResourceNotFoundException;
import com.harshit.monocept.repository.ClaimDocumentRepository;
import com.harshit.monocept.repository.ClaimRepository;
import com.harshit.monocept.repository.CustomerRepository;
import com.harshit.monocept.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClaimDocumentService {

	private static final Logger log = LoggerFactory.getLogger(ClaimDocumentService.class);

	private final ClaimDocumentRepository documentRepository;
	private final ClaimRepository claimRepository;
	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;
	private final CloudinaryService cloudinaryService;

	// ✅ Upload document for a claim
	@Transactional
	public DocumentUploadResponse uploadDocument(Long claimId, String documentName, String documentType,
			MultipartFile file, String email) {

		log.info("Document upload: claimId={}, type={}, user={}", claimId, documentType, email);

		// Claim exist karta hai?
		Claim claim = claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

		// User exist karta hai?
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// SRS CLM-BR-006: Customer sirf apni claim pe document upload kare
		if (user.getRole().name().equals("CUSTOMER")) {
			Customer customer = customerRepository.findByUserId(user.getId())
					.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

			if (!claim.getPolicy().getCustomer().getId().equals(customer.getId()))
				throw new BusinessRuleException("You can only upload documents for your own claims");
		}

		// SRS CLM-BR-009: APPROVED/REJECTED claim mein document add nahi
		if (claim.getClaimStatus() == ClaimStatus.APPROVED || claim.getClaimStatus() == ClaimStatus.REJECTED)
			throw new BusinessRuleException(
					"Cannot upload documents for a " + claim.getClaimStatus().name() + " claim");

		// Cloudinary pe upload karo
		Map<String, String> uploadResult = cloudinaryService.uploadFile(file, claim.getClaimNumber());

		// Database mein save karo
		ClaimDocument document = ClaimDocument.builder().claim(claim).documentName(documentName)
				.documentType(documentType).documentReference(uploadResult.get("url"))
				.cloudinaryPublicId(uploadResult.get("publicId")).originalFileName(uploadResult.get("originalName"))
				.fileSizeBytes(Long.parseLong(uploadResult.get("size"))).fileFormat(uploadResult.get("format")).build();

		ClaimDocument saved = documentRepository.save(document);
		log.info("Document saved: id={}, claimId={}, url={}", saved.getId(), claimId, uploadResult.get("url"));

		return mapToResponse(saved);
	}

	// ✅ Get all documents for a claim
	public List<DocumentUploadResponse> getClaimDocuments(Long claimId, String email) {

		Claim claim = claimRepository.findById(claimId)
				.orElseThrow(() -> new ResourceNotFoundException("Claim not found with id: " + claimId));

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		// Customer sirf apni claim ke documents dekhe
		if (user.getRole().name().equals("CUSTOMER")) {
			Customer customer = customerRepository.findByUserId(user.getId())
					.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

			if (!claim.getPolicy().getCustomer().getId().equals(customer.getId()))
				throw new BusinessRuleException("You can only view documents for your own claims");
		}

		return documentRepository.findByClaimId(claimId).stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	// ✅ Delete a document
	@Transactional
	public void deleteDocument(Long documentId, String email) {
		log.info("Document delete: documentId={}, by={}", documentId, email);

		ClaimDocument document = documentRepository.findById(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found with id: " + documentId));

		// APPROVED/REJECTED claim ka document delete nahi ho sakta
		ClaimStatus status = document.getClaim().getClaimStatus();
		if (status == ClaimStatus.APPROVED || status == ClaimStatus.REJECTED)
			throw new BusinessRuleException("Cannot delete documents from a " + status.name() + " claim");

		// Customer sirf apne claim ka document delete kare
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		if (user.getRole().name().equals("CUSTOMER")) {
			Customer customer = customerRepository.findByUserId(user.getId())
					.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

			if (!document.getClaim().getPolicy().getCustomer().getId().equals(customer.getId()))
				throw new BusinessRuleException("You can only delete your own documents");
		}

		// Cloudinary se delete karo
		cloudinaryService.deleteFile(document.getCloudinaryPublicId());

		// Database se delete karo
		documentRepository.delete(document);
		log.info("Document deleted: id={}", documentId);
	}

	// Helper: readable file size
	private String getReadableSize(Long bytes) {
		if (bytes == null)
			return "Unknown";
		if (bytes < 1024)
			return bytes + " B";
		if (bytes < 1024 * 1024)
			return String.format("%.1f KB", bytes / 1024.0);
		return String.format("%.1f MB", bytes / (1024.0 * 1024));
	}

	private DocumentUploadResponse mapToResponse(ClaimDocument d) {
		return DocumentUploadResponse.builder().documentId(d.getId()).claimId(d.getClaim().getId())
				.claimNumber(d.getClaim().getClaimNumber()).documentName(d.getDocumentName())
				.documentType(d.getDocumentType()).documentUrl(d.getDocumentReference())
				.originalFileName(d.getOriginalFileName()).fileFormat(d.getFileFormat())
				.fileSizeBytes(d.getFileSizeBytes()).fileSizeReadable(getReadableSize(d.getFileSizeBytes()))
				.uploadedAt(d.getUploadedAt()).build();
	}
}