package com.harshit.monocept.entity;

import java.time.LocalDateTime;

import com.harshit.monocept.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String fullName;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String mobileNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Builder.Default
	private Boolean isActive = true;

	// ✅ NEW: OTP verification flags
	@Builder.Default
	@Column(nullable = false)
	private Boolean emailVerified = false;

	@Builder.Default
	@Column(nullable = false)
	private Boolean phoneVerified = false;

	// ✅ NEW: Account is fully verified only after both OTPs confirmed
	@Builder.Default
	@Column(nullable = false)
	private Boolean isVerified = false;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}