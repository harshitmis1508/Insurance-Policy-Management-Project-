package com.harshit.monocept.dto.response;

import com.harshit.monocept.enums.ClaimStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// SRS 11.9: Claim response fields
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimResponse {
	private Long claimId;
	private String claimNumber;
	private Long policyId;
	private String policyNumber; // SRS 11.9
	private String customerName; // SRS 11.9
	private BigDecimal claimAmount;
	private String claimReason;
	private LocalDate incidentDate;
	private ClaimStatus claimStatus;
	private String agentRemarks;
	private String adminRemarks;
	private List<DocumentResponse> documents;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class DocumentResponse {
		private Long documentId;
		private String documentName;
		private String documentType;
		private String documentReference;
	}
}