package com.harshit.monocept.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

// SRS FR-USER-004: Admin agent account banaye
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAgentRequest {

	@NotBlank(message = "Full name is required")
	private String fullName;

	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	private String email;

	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Min 8 characters")
	private String password;

	@NotBlank(message = "Mobile number is required")
	private String mobileNumber;
}