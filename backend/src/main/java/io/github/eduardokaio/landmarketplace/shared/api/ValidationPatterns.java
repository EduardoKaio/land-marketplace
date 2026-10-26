package io.github.eduardokaio.landmarketplace.shared.api;

/**
 * Regular expressions shared by request DTOs. The frontend mirrors them (src/shared/phone.ts and
 * the zod email check), so a value it accepts is accepted here too.
 */
public final class ValidationPatterns {

	/**
	 * Brazilian phone as the frontend mask writes it: "(84) 3333-4444" or "(84) 99999-8888". The area
	 * code (DDD) has two digits from 1 to 9, since no DDD has a zero. A landline has 8 digits and
	 * starts with 2 to 5; a mobile has 9 digits and starts with 9.
	 */
	public static final String BRAZILIAN_PHONE = "\\([1-9]{2}\\) (?:[2-5]\\d{3}|9\\d{4})-\\d{4}";

	/**
	 * Added to @Email, which on its own accepts "ana@localhost": requires a dot in the domain and a
	 * top level domain of at least two letters, like the zod email check of the frontend.
	 */
	public static final String EMAIL_WITH_DOMAIN = ".+@.+\\.[A-Za-z]{2,}";

	public static final String BRAZILIAN_PHONE_MESSAGE = "must be a phone like (84) 99999-8888";

	private ValidationPatterns() {
	}

}
