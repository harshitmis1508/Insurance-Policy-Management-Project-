package com.harshit.monocept.controller;

import com.harshit.monocept.dto.request.LoginRequest;
import com.harshit.monocept.dto.request.RegisterRequest;
import com.harshit.monocept.dto.response.ApiResponse;
import com.harshit.monocept.dto.response.LoginResponse;
import com.harshit.monocept.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest req) {
		var user = authService.register(req);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Registered successfully", "User ID: " + user.getId()));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
		return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(req)));
	}
}