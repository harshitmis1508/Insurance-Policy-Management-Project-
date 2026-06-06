package com.harshit.monocept.dto.request;

import com.harshit.monocept.enums.ClaimStatus;
import jakarta.validation.constraints.*;
import lombok.*;

// SRS: Agent claim review karta hai
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReviewRequest {

	// Agent sirf UNDER_REVIEW, RECOMMENDED_FOR_APPROVAL,
	// RECOMMENDED_FOR_REJECTION set kar sakta hai
	@NotNull(message = "Status is required")
	private ClaimStatus recommendedStatus;

	@NotBlank(message = "Remarks are required")
	private String remarks;
}