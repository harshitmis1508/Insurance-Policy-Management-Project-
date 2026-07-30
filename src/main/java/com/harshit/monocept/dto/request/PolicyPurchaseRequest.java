package com.harshit.monocept.dto.request;

import java.time.LocalDate;

import com.harshit.monocept.enums.PremiumFrequency;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyPurchaseRequest {

	@NotNull(message = "Plan ID is required")
	private Long planId;

	@NotNull(message = "Start date is required")
	@FutureOrPresent(message = "Start date cannot be in the past")
	private LocalDate startDate;

	// Required only when the chosen plan's premiumType is ANNUAL
	private PremiumFrequency premiumFrequency;

	// Required only when the chosen plan's product type is MOTOR
	private String vehicleRegistrationNumber;
	private String vehicleMake;
	private String vehicleModel;
	private Integer vehicleManufactureYear;
}