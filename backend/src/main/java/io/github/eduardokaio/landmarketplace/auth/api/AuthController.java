package io.github.eduardokaio.landmarketplace.auth.api;

import java.time.Clock;

import io.github.eduardokaio.landmarketplace.auth.application.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

	private final AuthService service;

	private final Clock clock;

	public AuthController(AuthService service, Clock clock) {
		this.service = service;
		this.clock = clock;
	}

	@Override
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
		return TokenResponse.from(service.register(request.name(), request.email(), request.password()), clock);
	}

	@Override
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return TokenResponse.from(service.login(request.email(), request.password()), clock);
	}

}
