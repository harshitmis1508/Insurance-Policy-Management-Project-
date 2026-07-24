package com.harshit.monocept.enums;

import java.math.BigDecimal;

public enum PremiumFrequency {

	ANNUAL(1, new BigDecimal("-0.02")), HALF_YEARLY(2, new BigDecimal("-0.01")), QUARTERLY(4, BigDecimal.ZERO),
	MONTHLY(12, new BigDecimal("0.03"));

	private final int installmentsPerYear;
	private final BigDecimal loadingFactor;

	PremiumFrequency(int installmentsPerYear, BigDecimal loadingFactor) {
		this.installmentsPerYear = installmentsPerYear;
		this.loadingFactor = loadingFactor;
	}

	public int getInstallmentsPerYear() {
		return installmentsPerYear;
	}

	public BigDecimal getLoadingFactor() {
		return loadingFactor;
	}

	public int getMonthsPerInstallment() {
		return 12 / installmentsPerYear;
	}
}