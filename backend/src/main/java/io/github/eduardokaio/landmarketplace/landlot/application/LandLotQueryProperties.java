package io.github.eduardokaio.landmarketplace.landlot.application;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Limits that keep read queries cheap, set under land-lot.query in application.yml.
 *
 * @param maxSearchRadiusMeters largest radius accepted by the circle search
 * @param maxBboxResults most lots returned for one map viewport
 */
@Validated
@ConfigurationProperties("land-lot.query")
public record LandLotQueryProperties(@Positive double maxSearchRadiusMeters, @Positive int maxBboxResults) {
}
