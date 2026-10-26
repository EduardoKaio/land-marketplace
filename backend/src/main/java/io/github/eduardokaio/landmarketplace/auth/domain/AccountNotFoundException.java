package io.github.eduardokaio.landmarketplace.auth.domain;

/**
 * Raised when a valid token names a user that no longer exists, for example after the database
 * was reset while the browser kept its token. Treated as an authentication failure.
 */
public class AccountNotFoundException extends RuntimeException {

	public AccountNotFoundException() {
		super("The account of this session no longer exists. Please log in again.");
	}

}
