package io.github.eduardokaio.landmarketplace.auth.infrastructure;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

	private static final String SECRET = "x".repeat(JwtProperties.MIN_SECRET_BYTES);

	@Test
	void acceptsASecretOfAtLeast256Bits() {
		assertThatCode(() -> new JwtProperties(SECRET, Duration.ofHours(2), "issuer")).doesNotThrowAnyException();
	}

	@Test
	void rejectsAShortOrMissingSecret() {
		assertThatThrownBy(() -> new JwtProperties(SECRET.substring(1), Duration.ofHours(2), "issuer"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("app.jwt.secret");
		assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(2), "issuer"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsAnExpirationThatIsNotPositive() {
		assertThatThrownBy(() -> new JwtProperties(SECRET, Duration.ZERO, "issuer"))
			.hasMessageContaining("app.jwt.expiration");
		assertThatThrownBy(() -> new JwtProperties(SECRET, null, "issuer")).hasMessageContaining("app.jwt.expiration");
	}

	@Test
	void requiresAnIssuer() {
		assertThatThrownBy(() -> new JwtProperties(SECRET, Duration.ofHours(2), " "))
			.hasMessageContaining("app.jwt.issuer");
	}

}
