package io.github.eduardokaio.landmarketplace.auth.application;

import java.util.Optional;

import io.github.eduardokaio.landmarketplace.auth.AccountFixtures;
import io.github.eduardokaio.landmarketplace.auth.domain.AppUser;
import io.github.eduardokaio.landmarketplace.auth.domain.EmailAlreadyRegisteredException;
import io.github.eduardokaio.landmarketplace.auth.domain.InvalidCredentialsException;
import io.github.eduardokaio.landmarketplace.auth.infrastructure.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The two branches the HTTP tests cannot reach on purpose: a registration race and the timing of
 * a login with an unknown email.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AppUserRepository repository;

	@Mock
	private TokenService tokenService;

	// Cost 4: the real encoder, as fast as BCrypt allows
	private final PasswordEncoder passwordEncoder = spy(new BCryptPasswordEncoder(4));

	private AuthService service;

	@BeforeEach
	void setUp() {
		service = new AuthService(repository, passwordEncoder, tokenService);
	}

	@Test
	void concurrentRegistrationOfTheSameEmailIsStillAConflict() {
		when(repository.existsByEmail("kaio@example.com")).thenReturn(false);
		when(repository.saveAndFlush(any(AppUser.class))).thenThrow(new DataIntegrityViolationException("unique"));

		assertThatThrownBy(() -> service.register("Kaio", "kaio@example.com", "secret123"))
			.isInstanceOf(EmailAlreadyRegisteredException.class);
		verify(tokenService, never()).issue(any());
	}

	@Test
	void unknownEmailStillRunsAPasswordCheck() {
		when(repository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login("nobody@example.com", "secret123"))
			.isInstanceOf(InvalidCredentialsException.class);
		// Same work as a wrong password, so the response time does not reveal unknown emails
		verify(passwordEncoder).matches(anyString(), anyString());
	}

	@Test
	void wrongPasswordIsRejected() {
		when(repository.findByEmail("kaio@example.com"))
			.thenReturn(Optional.of(AccountFixtures.account("kaio@example.com")));

		assertThatThrownBy(() -> service.login("kaio@example.com", "wrong-password1"))
			.isInstanceOf(InvalidCredentialsException.class);
		verify(tokenService, never()).issue(any());
	}

}
