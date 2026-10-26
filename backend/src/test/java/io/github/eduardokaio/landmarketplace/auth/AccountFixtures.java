package io.github.eduardokaio.landmarketplace.auth;

import io.github.eduardokaio.landmarketplace.auth.domain.AppUser;

/**
 * Accounts for tests that need one in the database, for example as the owner of a land lot.
 */
public final class AccountFixtures {

	public static final String PASSWORD = "secret123";

	/**
	 * BCrypt of {@link #PASSWORD} with cost 4, the minimum: the encoder reads the cost from the
	 * hash, so tests skip the 100 ms of the production cost.
	 */
	public static final String PASSWORD_HASH = "$2a$04$LhUCh2MNwGJ4Y.jVzwtDxuCCUFv5RRZc2UA.dzzW0JxLU4c3lg60.";

	private AccountFixtures() {
	}

	public static AppUser account(String email) {
		return AppUser.register("Kaio Eduardo", email, PASSWORD_HASH);
	}

}
