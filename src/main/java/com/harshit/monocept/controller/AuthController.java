package com.harshit.monocept.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.harshit.monocept.dto.request.LoginRequest;
import com.harshit.monocept.dto.request.RegisterRequest;
import com.harshit.monocept.dto.request.ResendOtpRequest;
import com.harshit.monocept.dto.request.VerifyOtpRequest;
import com.harshit.monocept.dto.response.ApiResponse;
import com.harshit.monocept.dto.response.LoginResponse;
import com.harshit.monocept.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	/**
	 * Step 1: Register — saves user and sends email + phone OTP. User cannot login
	 * until OTP is verified.
	 */
	@PostMapping("/register")
	public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest req) {
		var user = authService.register(req);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Registration successful! OTP sent to your email and mobile number. "
						+ "Please verify both OTPs to activate your account.", "User ID: " + user.getId()));
	}

	/**
	 * Step 2: Verify OTP — validates email OTP + phone OTP. Activates the account
	 * after both are correct.
	 */
	@PostMapping("/verify-otp")
	public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
		authService.verifyOtp(req);
		return ResponseEntity.ok(ApiResponse.success("Account verified successfully! You can now login.",
				"Email and phone verified for: " + req.getEmail()));
	}

	/**
	 * Step 2b (optional): Resend OTP if user didn't receive or OTP expired.
	 */
	@PostMapping("/resend-otp")
	public ResponseEntity<ApiResponse<String>> resendOtp(@Valid @RequestBody ResendOtpRequest req) {
		authService.resendOtp(req);
		return ResponseEntity.ok(ApiResponse.success("New OTP sent to your registered email and mobile number.",
				"OTP resent for: " + req.getEmail()));
	}

	/**
	 * Step 3: Login — only allowed after OTP verification.
	 */
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
		return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(req)));
	}
}