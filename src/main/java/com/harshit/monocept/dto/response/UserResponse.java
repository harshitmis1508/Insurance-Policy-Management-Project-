package com.harshit.monocept.dto.response;

import com.harshit.monocept.enums.Role;
import lombok.*;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
	private Long userId;
	private String fullName;
	private String email;
	private String mobileNumber;
	private Role role;
	private Boolean isActive;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	
}