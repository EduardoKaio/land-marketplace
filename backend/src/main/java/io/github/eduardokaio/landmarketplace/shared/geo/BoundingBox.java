package io.github.eduardokaio.landmarketplace.shared.geo;

/**
 * Axis-aligned lon/lat rectangle (SRID 4326), usually the visible area of the map.
 * <p>
 * Boxes crossing the antimeridian (minLon greater than maxLon) are rejected: the client is
 * expected to send longitudes inside [-180, 180].
 */
public record BoundingBox(double minLon, double minLat, double maxLon, double maxLat) {

	private static final String FORMAT = "The bounding box must be minLon,minLat,maxLon,maxLat";

	public BoundingBox {
		requireInRange(minLon, 180, "Longitudes");
		requireInRange(maxLon, 180, "Longitudes");
		requireInRange(minLat, 90, "Latitudes");
		requireInRange(maxLat, 90, "Latitudes");
		if (minLon >= maxLon || minLat >= maxLat) {
			throw new InvalidBoundingBoxException("The bounding box minimums must be less than its maximums");
		}
	}

	/** Parses the "minLon,minLat,maxLon,maxLat" format of the bbox query parameter. */
	public static BoundingBox parse(String text) {
		String[] parts = text.split(",", -1);
		if (parts.length != 4) {
			throw new InvalidBoundingBoxException(FORMAT);
		}
		double[] values = new double[4];
		for (int i = 0; i < 4; i++) {
			values[i] = parseNumber(parts[i]);
		}
		return new BoundingBox(values[0], values[1], values[2], values[3]);
	}

	private static double parseNumber(String part) {
		try {
			double value = Double.parseDouble(part.strip());
			// NaN and Infinity parse fine but would fail the range checks with a confusing message
			if (!Double.isFinite(value)) {
				throw new InvalidBoundingBoxException(FORMAT);
			}
			return value;
		}
		catch (NumberFormatException ex) {
			throw new InvalidBoundingBoxException(FORMAT);
		}
	}

	private static void requireInRange(double value, double limit, String axis) {
		if (value < -limit || value > limit) {
			throw new InvalidBoundingBoxException(axis + " must be between " + (int) -limit + " and " + (int) limit);
		}
	}

}
