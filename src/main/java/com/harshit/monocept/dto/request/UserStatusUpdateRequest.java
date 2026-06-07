package com.harshit.monocept.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

// SRS 11.3: User status update
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusUpdateRequest {

	@NotNull(message = "Active status is required")
	private Boolean isActive;

	@NotBlank(message = "Reason is required")
	private String reason;
}