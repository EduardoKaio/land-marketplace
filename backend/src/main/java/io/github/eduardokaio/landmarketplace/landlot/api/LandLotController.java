package io.github.eduardokaio.landmarketplace.landlot.api;

import java.net.URI;
import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.application.LandLotQueryService;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotService;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotSlice;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.shared.geo.BoundingBox;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/land-lots")
public class LandLotController {

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

	@PostMapping
	public ResponseEntity<LandLotFeature> create(@Valid @RequestBody CreateLandLotRequest request) {
		Polygon polygon = geometryMapper.toPolygon(request.geometry());
		LandLot lot = service.create(request.toCommand(polygon));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(lot.getId())
			.toUri();
		return ResponseEntity.created(location).body(assembler.toFeature(lot));
	}

	/** Lots in the visible map area, given as bbox=minLon,minLat,maxLon,maxLat. */
	@GetMapping
	public LandLotFeatureCollection findInBoundingBox(@RequestParam String bbox) {
		LandLotSlice slice = queryService.findInBoundingBox(BoundingBox.parse(bbox));
		return assembler.toCollection(slice.lots(), slice.truncated());
	}

	/** Lots intersecting the circle drawn by the user. */
	@GetMapping("/search")
	public LandLotFeatureCollection search(@Valid LandLotSearchRequest request) {
		return assembler.toCollection(queryService.search(request.lon(), request.lat(), request.radius()));
	}

	@GetMapping("/{id}")
	public LandLotFeature findById(@PathVariable UUID id) {
		return assembler.toFeature(queryService.getById(id));
	}

}
