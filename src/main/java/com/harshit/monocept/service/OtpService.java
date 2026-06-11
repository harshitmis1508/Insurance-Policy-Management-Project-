package com.harshit.monocept.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harshit.monocept.entity.OtpVerification;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.repository.OtpVerificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

	private final OtpVerificationRepository otpRepository;
	private final EmailService emailService;
	private final SecureRandom secureRandom = new SecureRandom();

	@Value("${app.otp.expiry-minutes:5}")
	private long expiryMinutes;

	@Transactional
	public void createAndSendOtp(User user) {
		String emailOtp = generateSixDigitOtp();

		OtpVerification otpVerification = OtpVerification.builder().user(user).emailOtp(emailOtp).phoneOtp("N/A") // phone
																													// OTP
																													// removed,
																													// keeping
																													// column
																													// non-null
				.expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes)).used(false).build();

		otpRepository.save(otpVerification);
		log.info("OTP created for user: {}", user.getEmail());

		emailService.sendOtp(user.getEmail(), emailOtp);
	}

	@Transactional
	public void verifyOtp(User user, String submittedEmailOtp) {
		OtpVerification latestOtp = otpRepository.findTopByUserAndUsedFalseOrderByCreatedAtDesc(user).orElseThrow(
				() -> new BusinessRuleException("No active OTP found. Please register again or use Resend OTP."));

		if (latestOtp.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new BusinessRuleException("OTP has expired. Please use the Resend OTP option.");
		}

		if (!latestOtp.getEmailOtp().equals(submittedEmailOtp)) {
			throw new BusinessRuleException("Invalid Email OTP. Please check and try again.");
		}

		latestOtp.setUsed(true);
		otpRepository.save(latestOtp);
		log.info("OTP verified successfully for user: {}", user.getEmail());
	}

	private String generateSixDigitOtp() {
		int number = secureRandom.nextInt(900000) + 100000;
		return String.valueOf(number);
	}
}