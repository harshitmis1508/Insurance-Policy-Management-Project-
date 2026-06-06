package com.harshit.monocept.config;

import com.harshit.monocept.entity.User;
import com.harshit.monocept.enums.Role;
import com.harshit.monocept.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// CommandLineRunner = Application start hone ke baad
// automatically run hota hai
// SRS USR-BR-004: Admin account internally managed
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) {
		createAdminIfNotExists();
	}

	private void createAdminIfNotExists() {

		// Agar admin already hai toh kuch mat karo
		if (userRepository.existsByEmail("admin@insurance.com")) {
			log.info("Admin already exists — skipping creation");
			return;
		}

		// Admin nahi hai — banao
		// BCryptPasswordEncoder se password encode hoga
		// Yahi CORRECT hash hoga — koi mismatch nahi
		User admin = User.builder().fullName("Admin User").email("admin@insurance.com")
				.password(passwordEncoder.encode("password123")).mobileNumber("9999999999").role(Role.ADMIN)
				.isActive(true).build();

		userRepository.save(admin);
		log.info("Admin created successfully: admin@insurance.com");
	}
}