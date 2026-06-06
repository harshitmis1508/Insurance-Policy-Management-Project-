package com.harshit.monocept.dto.response;

import com.harshit.monocept.enums.ClaimStatus;
import lombok.*;
import java.time.LocalDateTime;

// SRS 9.9: Claim status history
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimHistoryResponse {
	private Long historyId;
	private Long claimId;
	private String claimNumber;
	private ClaimStatus previousStatus;
	private ClaimStatus newStatus;
	private String remarks;
	private String updatedByName; // HIS-BR-003: kisne change kiya
	private String updatedByRole;
	private LocalDateTime updatedAt;
}