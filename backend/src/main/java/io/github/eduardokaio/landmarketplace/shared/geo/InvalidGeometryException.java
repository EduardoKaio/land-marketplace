package io.github.eduardokaio.landmarketplace.shared.geo;

/**
 * Raised when a GeoJSON geometry sent by a client cannot be turned into a valid polygon.
 */
public class InvalidGeometryException extends RuntimeException {

	public InvalidGeometryException(String message) {
		super(message);
	}

	public InvalidGeometryException(String message, Throwable cause) {
		super(message, cause);
	}

}
