package com.harshit.monocept.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.harshit.monocept.dto.request.LoginRequest;
import com.harshit.monocept.dto.request.RegisterRequest;
import com.harshit.monocept.dto.request.ResendOtpRequest;
import com.harshit.monocept.dto.request.VerifyOtpRequest;
import com.harshit.monocept.dto.response.LoginResponse;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.enums.OtpChannel;
import com.harshit.monocept.enums.Role;
import com.harshit.monocept.exception.BusinessRuleException;
import com.harshit.monocept.exception.DuplicateResourceException;
import com.harshit.monocept.exception.ResourceNotFoundException;
import com.harshit.monocept.repository.UserRepository;
import com.harshit.monocept.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil;
	private final OtpService otpService;

	@Transactional
	public User register(RegisterRequest req) {
		if (userRepository.existsByEmail(req.getEmail())) {
			log.warn("Registration failed - duplicate email: {}", req.getEmail());
			throw new DuplicateResourceException("Email already exists: " + req.getEmail());
		}

		User user = User.builder().fullName(req.getFullName()).email(req.getEmail())
				.password(passwordEncoder.encode(req.getPassword())).mobileNumber(req.getMobileNumber())
				.preferredOtpChannel(req.getOtpChannel()).role(Role.CUSTOMER).isActive(true).emailVerified(false)
				.phoneVerified(false).isVerified(false).build();

		User saved = userRepository.save(user);
		log.info("New user registered (pending OTP): id={}, email={}, channel={}", saved.getId(), saved.getEmail(),
				req.getOtpChannel());

		otpService.createAndSendOtp(saved, req.getOtpChannel());
		return saved;
	}

	@Transactional
	public void verifyOtp(VerifyOtpRequest req) {
		User user = userRepository.findByEmail(req.getEmail())
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + req.getEmail()));

		if (Boolean.TRUE.equals(user.getIsVerified())) {
			throw new BusinessRuleException("Account is already verified. Please login.");
		}

		otpService.verifyOtp(user, req.getOtpChannel(), req.getOtp());

		if (req.getOtpChannel() == OtpChannel.EMAIL) {
			user.setEmailVerified(true);
		} else {
			user.setPhoneVerified(true);
		}

		user.setIsVerified(true);
		userRepository.save(user);

		log.info("User verified successfully via {}: email={}", req.getOtpChannel(), user.getEmail());
	}

	@Transactional
	public void resendOtp(ResendOtpRequest req) {
		User user = userRepository.findByEmail(req.getEmail())
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + req.getEmail()));

		if (Boolean.TRUE.equals(user.getIsVerified())) {
			throw new BusinessRuleException("Account is already verified. No need to resend OTP.");
		}

		otpService.createAndSendOtp(user, req.getOtpChannel());
		log.info("OTP resent for user: {} via {}", user.getEmail(), req.getOtpChannel());
	}

	public LoginResponse login(LoginRequest req) {
		User user = userRepository.findByEmail(req.getEmail())
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

		if (!Boolean.TRUE.equals(user.getIsVerified())) {
			throw new BusinessRuleException("Account not verified. Please verify your email or phone OTP first.");
		}

		try {
			authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
		} catch (BadCredentialsException e) {
			log.warn("Login failed for email: {}", req.getEmail());
			throw new BadCredentialsException("Invalid email or password");
		}

		String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
		log.info("Login successful: email={}, role={}", user.getEmail(), user.getRole());

		return LoginResponse.builder().token(token).tokenType("Bearer").email(user.getEmail())
				.fullName(user.getFullName()).role(user.getRole()).expiresIn(jwtUtil.getExpirationTime()).build();
	}
}