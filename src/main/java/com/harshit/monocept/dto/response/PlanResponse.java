package com.harshit.monocept.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.harshit.monocept.enums.PremiumType;
import com.harshit.monocept.enums.ProductType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// SRS 11.6: Plan response mein product name aur type bhi chahiye
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResponse {
	private Long planId;
	private Long productId;
	private String productName; // SRS 11.6
	private ProductType productType; // SRS 11.6
	private String planName;
	private BigDecimal coverageAmount;
	private BigDecimal premiumAmount;
	private PremiumType premiumType;
	private Integer durationYears;
	private String termsAndConditions;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}