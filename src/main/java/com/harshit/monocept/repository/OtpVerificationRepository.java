package com.harshit.monocept.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.harshit.monocept.entity.OtpVerification;
import com.harshit.monocept.entity.User;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

	// Finds the latest unused OTP for a user (for verification)
	Optional<OtpVerification> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);
}