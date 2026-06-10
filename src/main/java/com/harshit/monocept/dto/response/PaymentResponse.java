package com.harshit.monocept.dto.response;

import com.harshit.monocept.enums.PaymentMode;
import com.harshit.monocept.enums.PaymentStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
	private Long paymentId;
	private Long policyId;
	private String policyNumber; 
	private BigDecimal amount;
	private LocalDateTime paymentDate;
	private PaymentMode paymentMode;
	private String transactionReference;
	private PaymentStatus paymentStatus;
	private LocalDateTime createdAt;
}