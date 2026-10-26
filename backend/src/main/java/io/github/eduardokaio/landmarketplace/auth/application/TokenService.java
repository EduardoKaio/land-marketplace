package io.github.eduardokaio.landmarketplace.auth.application;

import java.time.Clock;
import java.time.Instant;

import io.github.eduardokaio.landmarketplace.auth.domain.AppUser;
import io.github.eduardokaio.landmarketplace.auth.infrastructure.JwtProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues access tokens. Validation happens in the JwtDecoder of SecurityConfig, on every request.
 */
@Service
public class TokenService {

	private final JwtEncoder encoder;

	private final JwtProperties properties;

	private final Clock clock;

	public TokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
		this.encoder = encoder;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * The subject is the user id, the only claim the API trusts to know who is calling. The name
	 * is a convenience for clients; nothing on the server reads it back.
	 */
	public AccessToken issue(AppUser user) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(properties.expiration());
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(user.getId().toString())
			.issuedAt(now)
			.expiresAt(expiresAt)
			.claim("name", user.getName())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken(value, expiresAt, user);
	}

}
