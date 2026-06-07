package com.harshit.monocept.dto.request;

import com.harshit.monocept.enums.PaymentMode;
import com.harshit.monocept.enums.PaymentStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

	@NotNull(message = "Policy ID is required")
	private Long policyId;

	// SRS PAY-BR-002: amount > 0
	@NotNull(message = "Amount is required")
	@DecimalMin(value = "0.01", message = "Amount must be greater than 0")
	private BigDecimal amount;

	@NotNull(message = "Payment mode is required")
	private PaymentMode paymentMode;

	// SRS PAY-BR-003: unique transaction reference
	@NotBlank(message = "Transaction reference is required")
	private String transactionReference;

	@NotNull(message = "Payment status is required")
	private PaymentStatus paymentStatus;
}