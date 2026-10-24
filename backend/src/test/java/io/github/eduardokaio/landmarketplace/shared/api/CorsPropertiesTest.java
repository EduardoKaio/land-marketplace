package io.github.eduardokaio.landmarketplace.shared.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

	// An unset or empty property binds to null; CorsConfig would fail on it instead of allowing no origin
	@Test
	void missingOriginsMeanNoOriginAllowed() {
		assertThat(new CorsProperties(null).allowedOrigins()).isEmpty();
	}

}
