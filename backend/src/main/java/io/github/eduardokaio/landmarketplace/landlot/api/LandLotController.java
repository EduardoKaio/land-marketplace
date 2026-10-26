package io.github.eduardokaio.landmarketplace.landlot.api;

import java.net.URI;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.auth.api.CurrentUser;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotQueryService;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotService;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotSlice;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.shared.geo.BoundingBox;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/land-lots")
public class LandLotController implements LandLotApi {

	private final LandLotService service;

	private final LandLotQueryService queryService;

	private final GeoJsonGeometryMapper geometryMapper;

	private final LandLotGeoJsonAssembler assembler;

	public LandLotController(LandLotService service, LandLotQueryService queryService,
			GeoJsonGeometryMapper geometryMapper, LandLotGeoJsonAssembler assembler) {
		this.service = service;
		this.queryService = queryService;
		this.geometryMapper = geometryMapper;
		this.assembler = assembler;
	}

	@Override
	@PostMapping
	public ResponseEntity<LandLotFeature> create(@Valid @RequestBody CreateLandLotRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		Polygon polygon = geometryMapper.toPolygon(request.geometry());
		LandLot lot = service.create(request.toCommand(polygon), CurrentUser.idOf(jwt));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(lot.getId())
			.toUri();
		return ResponseEntity.created(location).body(assembler.toFeature(lot));
	}

	/** Lots in the visible map area, given as bbox=minLon,minLat,maxLon,maxLat, optionally filtered. */
	@Override
	@GetMapping
	public LandLotFeatureCollection findInBoundingBox(@RequestParam String bbox,
			@Valid LandLotFilterRequest filter) {
		LandLotSlice slice = queryService.findInBoundingBox(BoundingBox.parse(bbox), filter.toFilter());
		return assembler.toCollection(slice.lots(), slice.truncated());
	}

	/**
	 * Lots intersecting the circle drawn by the user, optionally filtered. The circle and the
	 * filters bind from the same query string into two objects, each validated on its own.
	 */
	@Override
	@GetMapping("/search")
	public LandLotFeatureCollection search(@Valid LandLotSearchRequest request, @Valid LandLotFilterRequest filter) {
		return assembler
			.toCollection(queryService.search(request.lon(), request.lat(), request.radius(), filter.toFilter()));
	}

	@Override
	@GetMapping("/{id}")
	public LandLotFeature findById(@PathVariable UUID id) {
		return assembler.toFeature(queryService.getById(id));
	}

	@Override
	@PutMapping("/{id}")
	public LandLotFeature update(@PathVariable UUID id, @Valid @RequestBody UpdateLandLotRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return assembler.toFeature(service.update(id, request.toCommand(), CurrentUser.idOf(jwt)));
	}

	@Override
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		service.delete(id, CurrentUser.idOf(jwt));
	}

}
