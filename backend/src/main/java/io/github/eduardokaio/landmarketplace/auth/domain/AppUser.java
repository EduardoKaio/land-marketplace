package io.github.eduardokaio.landmarketplace.auth.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 254, unique = true)
	private String email;

	// Only the BCrypt hash is stored; the password itself never leaves the request
	@Column(name = "password_hash", nullable = false, length = 72)
	private String passwordHash;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AppUser() {
		// required by JPA
	}

	private AppUser(String name, String email, String passwordHash) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
	}

	/** @param passwordHash already hashed: the domain never sees the raw password */
	public static AppUser register(String name, String email, String passwordHash) {
		return new AppUser(name.strip(), normalizeEmail(email), passwordHash);
	}

	/**
	 * Lower case and without surrounding spaces, so "Ana@Example.com" and "ana@example.com" are
	 * the same account and a plain UNIQUE constraint is enough.
	 */
	public static String normalizeEmail(String email) {
		return email.strip().toLowerCase(Locale.ROOT);
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
