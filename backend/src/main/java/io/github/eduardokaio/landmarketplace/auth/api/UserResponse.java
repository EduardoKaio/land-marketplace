package io.github.eduardokaio.landmarketplace.auth.api;

import java.util.UUID;

import io.github.eduardokaio.landmarketplace.auth.domain.AppUser;

public record UserResponse(UUID id, String name, String email) {

	static UserResponse from(AppUser user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail());
	}

}
