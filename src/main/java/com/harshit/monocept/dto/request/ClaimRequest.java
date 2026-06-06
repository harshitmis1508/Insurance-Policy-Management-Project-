package com.harshit.monocept.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimRequest {

	@NotNull(message = "Policy ID is required")
	private Long policyId;

	// SRS CLM-BR-003: amount > 0
	@NotNull(message = "Claim amount is required")
	@DecimalMin(value = "0.01", message = "Claim amount must be greater than 0")
	private BigDecimal claimAmount;

	@NotBlank(message = "Claim reason is required")
	private String claimReason;

	// SRS CLM-BR-005: future date nahi
	@NotNull(message = "Incident date is required")
	@PastOrPresent(message = "Incident date cannot be a future date")
	private LocalDate incidentDate;

	// SRS DOC-BR-001: kam se kam ek document
	@NotEmpty(message = "At least one supporting document is required")
	private List<DocumentRequest> documents;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DocumentRequest {
		@NotBlank(message = "Document name is required")
		private String documentName;

		@NotBlank(message = "Document type is required")
		private String documentType;

		@NotBlank(message = "Document reference is required")
		private String documentReference;
	}
}