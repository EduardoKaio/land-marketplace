package io.github.eduardokaio.landmarketplace.landlot.api;

import java.net.URI;

import io.github.eduardokaio.landmarketplace.landlot.application.LandLotService;
import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import io.github.eduardokaio.landmarketplace.shared.geo.GeoJsonGeometryMapper;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/land-lots")
public class LandLotController {

	private final LandLotService service;

	private final GeoJsonGeometryMapper geometryMapper;

	private final LandLotGeoJsonAssembler assembler;

	public LandLotController(LandLotService service, GeoJsonGeometryMapper geometryMapper,
			LandLotGeoJsonAssembler assembler) {
		this.service = service;
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

}
