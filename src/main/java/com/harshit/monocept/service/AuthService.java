package com.harshit.monocept.service;

import com.harshit.monocept.dto.request.LoginRequest;
import com.harshit.monocept.dto.request.RegisterRequest;
import com.harshit.monocept.dto.response.LoginResponse;
import com.harshit.monocept.entity.User;
import com.harshit.monocept.enums.Role;
import com.harshit.monocept.exception.DuplicateResourceException;
import com.harshit.monocept.repository.UserRepository;
import com.harshit.monocept.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil;

	public User register(RegisterRequest req) {
		if (userRepository.existsByEmail(req.getEmail()))
			throw new DuplicateResourceException("Email already exists: " + req.getEmail());

		User user = User.builder().fullName(req.getFullName()).email(req.getEmail())
				.password(passwordEncoder.encode(req.getPassword())).mobileNumber(req.getMobileNumber())
				.role(Role.CUSTOMER).isActive(true).build();

		return userRepository.save(user);
	}

	public LoginResponse login(LoginRequest req) {
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

		User user = userRepository.findByEmail(req.getEmail()).orElseThrow();
		String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

		return LoginResponse.builder().token(token).tokenType("Bearer").email(user.getEmail())
				.fullName(user.getFullName()).role(user.getRole()).expiresIn(jwtUtil.getExpirationTime()).build();
	}
}