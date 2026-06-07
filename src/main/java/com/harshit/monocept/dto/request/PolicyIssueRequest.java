package com.harshit.monocept.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

// SRS: Agent/Admin customer ke liye policy issue karte hain
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyIssueRequest {

	@NotNull(message = "Customer ID is required")
	private Long customerId;

	@NotNull(message = "Plan ID is required")
	private Long planId;

	@NotNull(message = "Start date is required")
	@FutureOrPresent(message = "Start date cannot be in the past")
	private LocalDate startDate;
}