package com.harshit.monocept.dto.request;

import com.harshit.monocept.enums.ClaimStatus;
import jakarta.validation.constraints.*;
import lombok.*;

// SRS: Admin final decision leta hai
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimDecisionRequest {

	// Admin sirf APPROVED ya REJECTED set kar sakta hai
	@NotNull(message = "Decision is required")
	private ClaimStatus finalStatus;

	@NotBlank(message = "Remarks are required")
	private String remarks;
}