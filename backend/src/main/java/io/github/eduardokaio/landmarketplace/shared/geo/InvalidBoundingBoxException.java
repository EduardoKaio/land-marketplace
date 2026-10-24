package io.github.eduardokaio.landmarketplace.shared.geo;

/**
 * Raised when the bbox query parameter is not a usable minLon,minLat,maxLon,maxLat rectangle.
 */
public class InvalidBoundingBoxException extends RuntimeException {

	public InvalidBoundingBoxException(String message) {
		super(message);
	}

}
