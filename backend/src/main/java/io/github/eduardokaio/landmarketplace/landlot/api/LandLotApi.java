package io.github.eduardokaio.landmarketplace.landlot.api;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * OpenAPI documentation of the land lot endpoints. Kept apart from LandLotController so the
 * controller stays short; springdoc reads the annotations from the interface.
 */
@Tag(name = "Land lots", description = "Register, list and search land lots")
interface LandLotApi {

	String JSON = "application/json";

	String PROBLEM_JSON = "application/problem+json";

	@Operation(summary = "Register a land lot",
			description = "Creates a lot from its polygon and details. The polygon must be valid and must not "
					+ "overlap the area of an existing lot; sharing a border or a vertex is allowed. "
					+ "Coordinates are rounded to 7 decimals.")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = JSON,
			examples = @ExampleObject(name = "Lot near Ponta Negra", value = LandLotApiExamples.CREATE_REQUEST)))
	@ApiResponse(responseCode = "201", description = "Lot created",
			headers = @Header(name = "Location", description = "URL of the new lot",
					schema = @Schema(type = "string", format = "uri")),
			content = @Content(mediaType = JSON, schema = @Schema(implementation = LandLotFeature.class),
					examples = @ExampleObject(value = LandLotApiExamples.FEATURE)))
	@ApiResponse(responseCode = "400", description = "Missing or invalid fields, or malformed JSON",
			content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
					examples = @ExampleObject(value = LandLotApiExamples.VALIDATION_PROBLEM)))
	@ApiResponse(responseCode = "409", description = "The polygon overlaps existing lots",
			content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
					examples = @ExampleObject(value = LandLotApiExamples.OVERLAP_PROBLEM)))
	@ApiResponse(responseCode = "422",
			description = "The geometry is not a valid polygon (wrong type, open ring, self-intersection...)",
			content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
					examples = @ExampleObject(value = LandLotApiExamples.INVALID_GEOMETRY_PROBLEM)))
	ResponseEntity<LandLotFeature> create(CreateLandLotRequest request);

	@Operation(summary = "List the lots in the visible map area",
			description = "Returns the lots whose bounding box intersects the given rectangle, largest first, up to "
					+ "a configured maximum. \"truncated\" is true when more lots matched.")
	@ApiResponse(responseCode = "200", description = "Lots in the area",
			content = @Content(mediaType = JSON, schema = @Schema(implementation = LandLotFeatureCollection.class),
					examples = @ExampleObject(value = LandLotApiExamples.FEATURE_COLLECTION)))
	@ApiResponse(responseCode = "400", description = "Missing or malformed bbox",
			content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
					examples = @ExampleObject(value = LandLotApiExamples.BOUNDING_BOX_PROBLEM)))
	LandLotFeatureCollection findInBoundingBox(@Parameter(
			description = "minLon,minLat,maxLon,maxLat in degrees; must not cross the antimeridian",
			example = "-35.21,-5.91,-35.19,-5.87") String bbox);

	@Operation(summary = "Search the lots that intersect a circle",
			description = "Returns every lot with at least one point within the radius of the center, measured in "
					+ "meters on the WGS84 spheroid.")
	@ApiResponse(responseCode = "200", description = "Lots intersecting the circle",
			content = @Content(mediaType = JSON, schema = @Schema(implementation = LandLotFeatureCollection.class),
					examples = @ExampleObject(value = LandLotApiExamples.FEATURE_COLLECTION)))
	@ApiResponse(responseCode = "400", description = "Missing or out of range parameters, or radius above the maximum",
			content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
					examples = { @ExampleObject(name = "Invalid parameter", value = LandLotApiExamples.SEARCH_VALIDATION_PROBLEM),
							@ExampleObject(name = "Radius too large",
									value = LandLotApiExamples.RADIUS_TOO_LARGE_PROBLEM) }))
	LandLotFeatureCollection search(@ParameterObject LandLotSearchRequest request);

	@Operation(summary = "Get a land lot")
	@ApiResponse(responseCode = "200", description = "The lot",
			content = @Content(mediaType = JSON, schema = @Schema(implementation = LandLotFeature.class),
					examples = @ExampleObject(value = LandLotApiExamples.FEATURE)))
	@ApiResponse(responseCode = "400", description = "The id is not a UUID",
			content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
					examples = @ExampleObject(value = LandLotApiExamples.MALFORMED_ID_PROBLEM)))
	@ApiResponse(responseCode = "404", description = "No lot with this id",
			content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
					examples = @ExampleObject(value = LandLotApiExamples.NOT_FOUND_PROBLEM)))
	LandLotFeature findById(@Parameter(description = "Lot id", example = "3f0c2a8e-5b1d-4c7a-9e2f-1a6b8d4c0e93") UUID id);

}
