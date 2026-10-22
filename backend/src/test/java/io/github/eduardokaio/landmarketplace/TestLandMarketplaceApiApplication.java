package io.github.eduardokaio.landmarketplace;

import org.springframework.boot.SpringApplication;

public class TestLandMarketplaceApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(LandMarketplaceApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
