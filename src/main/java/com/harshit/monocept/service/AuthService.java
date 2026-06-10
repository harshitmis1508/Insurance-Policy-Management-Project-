package com.harshit.monocept.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.harshit.monocept.dto.request.LoginRequest;
import com.harshit.monocept.dto.request.RegisterRequest;
import com.harshit.monocept.dto.response.LoginResponse;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.enums.Role;
import com.harshit.monocept.exception.DuplicateResourceException;
import com.harshit.monocept.repository.UserRepository;
import com.harshit.monocept.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil;

	public User register(RegisterRequest req) {
		if (userRepository.existsByEmail(req.getEmail())) {
			log.warn("Registration failed - duplicate email: {}", req.getEmail());
			throw new DuplicateResourceException("Email already exists: " + req.getEmail());
		}

		User user = User.builder().fullName(req.getFullName()).email(req.getEmail())
				.password(passwordEncoder.encode(req.getPassword())).mobileNumber(req.getMobileNumber())
				.role(Role.CUSTOMER).isActive(true).build();

		User saved = userRepository.save(user);
		log.info("New customer registered: id={}, email={}", saved.getId(), saved.getEmail());
		return saved;
	}

	public LoginResponse login(LoginRequest req) {
		try {
			authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
		} catch (BadCredentialsException e) {
			log.warn("Login failed for email: {}", req.getEmail());
			throw new BadCredentialsException("Invalid email or password");
		}

		User user = userRepository.findByEmail(req.getEmail()).orElseThrow();
		String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

		log.info("Login successful: email={}, role={}", user.getEmail(), user.getRole());

		return LoginResponse.builder().token(token).tokenType("Bearer").email(user.getEmail())
				.fullName(user.getFullName()).role(user.getRole()).expiresIn(jwtUtil.getExpirationTime()).build();
	}
}