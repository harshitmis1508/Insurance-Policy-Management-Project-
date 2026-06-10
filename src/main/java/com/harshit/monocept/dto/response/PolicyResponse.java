package com.harshit.monocept.dto.response;

import com.harshit.monocept.enums.PolicyStatus;
import com.harshit.monocept.enums.PremiumType;
import com.harshit.monocept.enums.ProductType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyResponse {
	private Long policyId;
	private String policyNumber;
	private Long customerId;
	private String customerName; 
	private Long planId;
	private String planName; 
	private ProductType productType; 
	private BigDecimal coverageAmount; 
	private BigDecimal premiumAmount; 
	private PremiumType premiumType; 
	private LocalDate startDate;
	private LocalDate endDate;
	private PolicyStatus status;
	private BigDecimal totalPremiumPaid;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}