package io.github.eduardokaio.landmarketplace.landlot.domain;

import java.math.BigDecimal;

/**
 * Raised when a circle search asks for a radius above the configured maximum.
 */
public class SearchRadiusTooLargeException extends RuntimeException {

	private final double maxRadiusMeters;

	public SearchRadiusTooLargeException(double maxRadiusMeters) {
		super("The search radius must be at most %s meters.".formatted(formatMeters(maxRadiusMeters)));
		this.maxRadiusMeters = maxRadiusMeters;
	}

	public double getMaxRadiusMeters() {
		return maxRadiusMeters;
	}

	// Prints 50000 instead of 50000.0
	private static String formatMeters(double meters) {
		return BigDecimal.valueOf(meters).stripTrailingZeros().toPlainString();
	}

}
