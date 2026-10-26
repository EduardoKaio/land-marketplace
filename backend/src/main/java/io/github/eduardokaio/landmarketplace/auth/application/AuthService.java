package io.github.eduardokaio.landmarketplace.auth.application;

import java.util.UUID;

import io.github.eduardokaio.landmarketplace.auth.domain.AppUser;
import io.github.eduardokaio.landmarketplace.auth.domain.EmailAlreadyRegisteredException;
import io.github.eduardokaio.landmarketplace.auth.domain.InvalidCredentialsException;
import io.github.eduardokaio.landmarketplace.auth.infrastructure.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final AppUserRepository repository;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokenService;

	/**
	 * Hash compared when the email is unknown. Without it an unknown email would answer at once
	 * and a wrong password after a BCrypt check (about 100 ms), and that difference would tell
	 * which emails have an account.
	 */
	private final String unknownUserHash;

	public AuthService(AppUserRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.unknownUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	/** Creates the account and logs it in, so the client does not need a second request. */
	@Transactional
	public AccessToken register(String name, String email, String password) {
		if (repository.existsByEmail(AppUser.normalizeEmail(email))) {
			throw new EmailAlreadyRegisteredException();
		}
		AppUser user = AppUser.register(name, email, passwordEncoder.encode(password));
		try {
			repository.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			// Two registrations of the same email at once both pass the check above; the UNIQUE
			// constraint stops the second one, which gets the same answer as a plain duplicate
			throw new EmailAlreadyRegisteredException();
		}
		return tokenService.issue(user);
	}

	@Transactional(readOnly = true)
	public AccessToken login(String email, String password) {
		AppUser user = repository.findByEmail(AppUser.normalizeEmail(email)).orElse(null);
		String hash = user == null ? unknownUserHash : user.getPasswordHash();
		if (!passwordEncoder.matches(password, hash) || user == null) {
			throw new InvalidCredentialsException();
		}
		return tokenService.issue(user);
	}

}
