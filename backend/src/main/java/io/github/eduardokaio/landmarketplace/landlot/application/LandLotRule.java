package io.github.eduardokaio.landmarketplace.landlot.application;

/**
 * A business rule checked before a land lot is created. Each rule is a Spring bean; the service
 * receives all of them as an ordered list (by {@link org.springframework.core.annotation.Order}),
 * so a new rule is added without changing the service.
 */
public interface LandLotRule {

	/**
	 * Throws a domain exception describing the violation, or returns normally when the rule holds.
	 */
	void validate(CreateLandLotCommand command);

}
