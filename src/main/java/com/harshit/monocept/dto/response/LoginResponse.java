package com.harshit.monocept.dto.response;

import com.harshit.monocept.enums.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
	private String token;
	private String tokenType = "Bearer";
	private String email;
	private String fullName;
	private Role role;
	private long expiresIn;
}