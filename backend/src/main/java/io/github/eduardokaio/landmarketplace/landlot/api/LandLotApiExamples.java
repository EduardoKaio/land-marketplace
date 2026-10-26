package io.github.eduardokaio.landmarketplace.landlot.api;

/**
 * JSON examples shown in the OpenAPI document. Based on the example lot of the API contract, a
 * square near Ponta Negra, Natal.
 */
final class LandLotApiExamples {

	static final String CREATE_REQUEST = """
			{
			  "title": "Corner lot near Ponta Negra",
			  "price": 250000.00,
			  "description": "Flat terrain, ready to build, two blocks from the beach.",
			  "contact": { "name": "Kaio Eduardo", "phone": "(84) 99999-0000", "email": "kaio@example.com" },
			  "geometry": {
			    "type": "Polygon",
			    "coordinates": [[[-35.196, -5.880], [-35.195, -5.880], [-35.195, -5.879],
			                     [-35.196, -5.879], [-35.196, -5.880]]]
			  }
			}""";

	static final String FEATURE = """
			{
			  "type": "Feature",
			  "id": "3f0c2a8e-5b1d-4c7a-9e2f-1a6b8d4c0e93",
			  "geometry": {
			    "type": "Polygon",
			    "coordinates": [[[-35.196, -5.88], [-35.195, -5.88], [-35.195, -5.879],
			                     [-35.196, -5.879], [-35.196, -5.88]]]
			  },
			  "properties": {
			    "title": "Corner lot near Ponta Negra",
			    "price": 250000.00,
			    "areaSquareMeters": 12246.04,
			    "pricePerSquareMeter": 20.41,
			    "description": "Flat terrain, ready to build, two blocks from the beach.",
			    "contact": { "name": "Kaio Eduardo", "phone": "(84) 99999-0000", "email": "kaio@example.com" },
			    "status": "AVAILABLE",
			    "createdAt": "2026-10-24T14:30:00Z"
			  }
			}""";

	// Concatenation, not String.formatted, because annotation values must be compile-time constants
	static final String FEATURE_COLLECTION = "{\"type\": \"FeatureCollection\", \"features\": [" + FEATURE
			+ "], \"truncated\": false}";

	static final String VALIDATION_PROBLEM = """
			{
			  "type": "/problems/validation",
			  "title": "Invalid request",
			  "status": 400,
			  "detail": "The request has 2 invalid fields.",
			  "instance": "/api/land-lots",
			  "errors": [
			    { "field": "contact.phoneOrEmailPresent", "message": "phone or email is required" },
			    { "field": "price", "message": "must be greater than 0" }
			  ]
			}""";

	static final String SEARCH_VALIDATION_PROBLEM = """
			{
			  "type": "/problems/validation",
			  "title": "Invalid request",
			  "status": 400,
			  "detail": "The request has 1 invalid field.",
			  "instance": "/api/land-lots/search",
			  "errors": [ { "field": "lat", "message": "must be less than or equal to 90" } ]
			}""";

	static final String RADIUS_TOO_LARGE_PROBLEM = """
			{
			  "type": "/problems/search-radius-too-large",
			  "title": "Search radius too large",
			  "status": 400,
			  "detail": "The search radius must be at most 50000 meters.",
			  "instance": "/api/land-lots/search",
			  "maxRadiusMeters": 50000.0
			}""";

	static final String BOUNDING_BOX_PROBLEM = """
			{
			  "type": "/problems/invalid-bounding-box",
			  "title": "Invalid bounding box",
			  "status": 400,
			  "detail": "The bounding box must be minLon,minLat,maxLon,maxLat",
			  "instance": "/api/land-lots"
			}""";

	static final String MALFORMED_ID_PROBLEM = """
			{
			  "type": "about:blank",
			  "title": "Bad Request",
			  "status": 400,
			  "detail": "Failed to convert 'id' with value: 'not-a-uuid'",
			  "instance": "/api/land-lots/not-a-uuid"
			}""";

	static final String NOT_FOUND_PROBLEM = """
			{
			  "type": "/problems/land-lot-not-found",
			  "title": "Land lot not found",
			  "status": 404,
			  "detail": "No land lot with id 3f0c2a8e-5b1d-4c7a-9e2f-1a6b8d4c0e93.",
			  "instance": "/api/land-lots/3f0c2a8e-5b1d-4c7a-9e2f-1a6b8d4c0e93"
			}""";

	static final String OVERLAP_PROBLEM = """
			{
			  "type": "/problems/land-lot-overlap",
			  "title": "Land lot overlaps existing lots",
			  "status": 409,
			  "detail": "The submitted polygon overlaps 1 existing land lot.",
			  "instance": "/api/land-lots",
			  "conflictingLotIds": ["3f0c2a8e-5b1d-4c7a-9e2f-1a6b8d4c0e93"]
			}""";

	static final String INVALID_GEOMETRY_PROBLEM = """
			{
			  "type": "/problems/invalid-geometry",
			  "title": "Invalid geometry",
			  "status": 422,
			  "detail": "Self-intersection at (-35.1955, -5.8795)",
			  "instance": "/api/land-lots"
			}""";

	private LandLotApiExamples() {
	}

}
