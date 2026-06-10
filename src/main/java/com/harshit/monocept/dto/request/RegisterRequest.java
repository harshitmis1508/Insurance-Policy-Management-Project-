package com.harshit.monocept.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
	@NotBlank(message = "Full name is required")
	private String fullName;

	@NotBlank
	@Email(message = "Invalid email")
	private String email;

	@NotBlank
	@Size(min = 8, message = "Min 8 characters")
	private String password;

	@NotBlank(message = "Mobile number is required")
	@Size(min = 10, max = 10, message = "Mobile number must be 10 digits")
	@Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number")
	private String mobileNumber;
}