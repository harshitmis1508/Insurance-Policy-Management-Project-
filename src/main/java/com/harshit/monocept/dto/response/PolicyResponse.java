package com.harshit.monocept.dto.response;

import com.harshit.monocept.enums.PolicyStatus;
import com.harshit.monocept.enums.PremiumType;
import com.harshit.monocept.enums.ProductType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// SRS 11.7: Policy response fields
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyResponse {
	private Long policyId;
	private String policyNumber;
	private Long customerId;
	private String customerName; // SRS 11.7
	private Long planId;
	private String planName; // SRS 11.7
	private ProductType productType; // SRS 11.7
	private BigDecimal coverageAmount; // SRS 11.7
	private BigDecimal premiumAmount; // SRS 11.7
	private PremiumType premiumType; // SRS 11.7
	private LocalDate startDate;
	private LocalDate endDate;
	private PolicyStatus status;
	private BigDecimal totalPremiumPaid;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}