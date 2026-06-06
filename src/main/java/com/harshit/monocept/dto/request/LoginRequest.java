package com.harshit.monocept.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
	@NotBlank
	@Email
	private String email;

	@NotBlank
	private String password;
}