package io.github.eduardokaio.landmarketplace.landlot.api;

import io.github.eduardokaio.landmarketplace.auth.api.CurrentUser;
import io.github.eduardokaio.landmarketplace.landlot.application.LandLotQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lots of the logged in account. Apart from LandLotController because it lives under /api/me. */
@RestController
@Tag(name = "Land lots")
public class MyLandLotController {

	private final LandLotQueryService queryService;

	private final LandLotGeoJsonAssembler assembler;

	public MyLandLotController(LandLotQueryService queryService, LandLotGeoJsonAssembler assembler) {
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@Operation(summary = "List my land lots", description = "The lots created by the logged in account, newest first.")
	@SecurityRequirement(name = "bearer")
	@GetMapping("/api/me/land-lots")
	public LandLotFeatureCollection findMine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
		return assembler.toCollection(queryService.findByOwner(CurrentUser.idOf(jwt)));
	}

}
