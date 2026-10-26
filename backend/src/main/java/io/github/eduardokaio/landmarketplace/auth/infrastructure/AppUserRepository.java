package io.github.eduardokaio.landmarketplace.auth.infrastructure;

import java.util.Optional;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.auth.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

	/** @param email already normalized, see {@link AppUser#normalizeEmail} */
	Optional<AppUser> findByEmail(String email);

	/** @param email already normalized, see {@link AppUser#normalizeEmail} */
	boolean existsByEmail(String email);

}
