package io.github.eduardokaio.landmarketplace.auth.domain;

/**
 * Raised when registering an email that already has an account.
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

	public EmailAlreadyRegisteredException() {
		super("An account with this email already exists.");
	}

}
