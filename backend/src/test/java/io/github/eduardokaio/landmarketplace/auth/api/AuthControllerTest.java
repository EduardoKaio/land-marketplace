package io.github.eduardokaio.landmarketplace.auth.api;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.spec.SecretKeySpec;

import io.github.eduardokaio.landmarketplace.TestcontainersConfiguration;
import io.github.eduardokaio.landmarketplace.auth.AccountFixtures;
import io.github.eduardokaio.landmarketplace.auth.infrastructure.AppUserRepository;
import io.github.eduardokaio.landmarketplace.auth.infrastructure.JwtProperties;
import io.github.eduardokaio.landmarketplace.landlot.infrastructure.LandLotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registration, login and real signed tokens, end to end. The tests of the land lot access rules
 * use jwt() instead; here the tokens go through the JwtDecoder of SecurityConfig.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

	private static final String EMAIL = "kaio@example.com";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppUserRepository accounts;

	@Autowired
	private LandLotRepository lots;

	@Autowired
	private JwtEncoder encoder;

	@Autowired
	private JwtProperties properties;

	@BeforeEach
	void cleanDatabase() {
		lots.deleteAll();
		accounts.deleteAll();
	}

	@Test
	void registersLogsInAndStoresOnlyTheHash() throws Exception {
		register("Kaio", "Kaio@Example.COM", "secret123").andExpect(status().isCreated())
			.andExpect(jsonPath("$.accessToken").isString())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn", greaterThan(7000)))
			.andExpect(jsonPath("$.expiresIn", lessThanOrEqualTo(7200)))
			.andExpect(jsonPath("$.user.id").isString())
			.andExpect(jsonPath("$.user.name").value("Kaio"))
			.andExpect(jsonPath("$.user.email").value(EMAIL));

		assertThat(accounts.findByEmail(EMAIL)).get()
			.satisfies(account -> assertThat(account.getPasswordHash()).startsWith("$2a$").doesNotContain("secret123"));
	}

	@Test
	void rejectsAnEmailAlreadyRegisteredInAnyCase() throws Exception {
		accounts.save(AccountFixtures.account(EMAIL));

		register("Other", "KAIO@example.com", "secret123").andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/email-already-registered"))
			.andExpect(jsonPath("$.detail").value("An account with this email already exists."));
		assertThat(accounts.count()).isEqualTo(1);
	}

	@ParameterizedTest
	@ValueSource(strings = { "short1", "onlyletters", "1234567890", "a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a1a" })
	void rejectsAWeakOrTooLongPassword(String password) throws Exception {
		register("Kaio", EMAIL, password).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.type").value("/problems/validation"))
			.andExpect(jsonPath("$.errors[0].field").value("password"));
		assertThat(accounts.count()).isZero();
	}

	// "ana@localhost" passes a plain @Email; the domain needs a dot, as the frontend requires
	@ParameterizedTest
	@ValueSource(strings = { "not-an-email", "ana@localhost", "ana@example.c" })
	void rejectsAnInvalidEmail(String email) throws Exception {
		register("Kaio", email, "secret123").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("email"));
	}

	@Test
	void logsInWithTheRightPasswordAndTheTokenOpensProtectedRoutes() throws Exception {
		accounts.save(AccountFixtures.account(EMAIL));

		String body = login("KAIO@example.com", AccountFixtures.PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.user.email").value(EMAIL))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String token = JsonMapper.shared().readTree(body).get("accessToken").stringValue();

		mockMvc.perform(get("/api/me/land-lots").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.features").isEmpty());
	}

	@Test
	void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
		accounts.save(AccountFixtures.account(EMAIL));

		expectInvalidCredentials(login(EMAIL, "wrong-password1"));
		expectInvalidCredentials(login("nobody@example.com", AccountFixtures.PASSWORD));
	}

	@Test
	void protectedRouteWithoutTokenAsksForOne() throws Exception {
		mockMvc.perform(get("/api/me/land-lots"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/unauthorized"))
			.andExpect(jsonPath("$.detail").value(containsString("Authentication is required")))
			.andExpect(jsonPath("$.instance").value("/api/me/land-lots"));
	}

	@Test
	void rejectsAnExpiredToken() throws Exception {
		// Beyond the 60 seconds of clock skew the decoder tolerates
		Instant issuedAt = Instant.now().minus(3, ChronoUnit.HOURS);
		String token = sign(encoder, properties.issuer(), issuedAt, issuedAt.plus(1, ChronoUnit.HOURS));

		expectInvalidToken(token);
	}

	@Test
	void rejectsATokenSignedWithAnotherKey() throws Exception {
		JwtEncoder forger = NimbusJwtEncoder
			.withSecretKey(new SecretKeySpec("another-secret-of-at-least-32-bytes!!".getBytes(StandardCharsets.UTF_8),
					"HmacSHA256"))
			.algorithm(MacAlgorithm.HS256)
			.build();

		expectInvalidToken(sign(forger, properties.issuer(), Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS)));
	}

	@Test
	void rejectsATokenFromAnotherIssuer() throws Exception {
		expectInvalidToken(sign(encoder, "someone-else", Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS)));
	}

	@Test
	void rejectsAMalformedToken() throws Exception {
		expectInvalidToken("not-a-jwt");
	}

	private void expectInvalidToken(String token) throws Exception {
		mockMvc.perform(get("/api/me/land-lots").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("The access token is invalid or expired. Please log in again."));
	}

	private static void expectInvalidCredentials(ResultActions result) throws Exception {
		result.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.type").value("/problems/unauthorized"))
			.andExpect(jsonPath("$.detail").value("Invalid email or password."))
			.andExpect(jsonPath("$.accessToken").doesNotExist());
	}

	private static String sign(JwtEncoder encoder, String issuer, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(issuer)
			.subject("b0000000-0000-4000-8000-000000000001")
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	private ResultActions register(String name, String email, String password) throws Exception {
		return postJson("/api/auth/register", """
				{"name": "%s", "email": "%s", "password": "%s"}""".formatted(name, email, password));
	}

	private ResultActions login(String email, String password) throws Exception {
		return postJson("/api/auth/login", """
				{"email": "%s", "password": "%s"}""".formatted(email, password));
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
	}

}
