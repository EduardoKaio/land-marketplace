package io.github.eduardokaio.landmarketplace.auth.domain;

/**
 * Raised when the email is unknown or the password is wrong. One exception and one message for
 * both cases, so the API does not reveal which emails have an account.
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid email or password.");
	}

}
