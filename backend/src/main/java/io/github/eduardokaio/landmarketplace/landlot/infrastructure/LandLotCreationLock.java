package io.github.eduardokaio.landmarketplace.landlot.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serializes land lot creation with a PostgreSQL transaction-level advisory lock.
 * <p>
 * Why: creation is check-then-insert (overlap query, then INSERT). Under READ COMMITTED two
 * concurrent transactions can both run the overlap query, neither sees the other's uncommitted row,
 * and both insert overlapping polygons. No table constraint can express "interiors must not cross"
 * (an EXCLUDE constraint with && compares bounding boxes and would also reject neighbors).
 * <p>
 * How: pg_advisory_xact_lock blocks until no other transaction holds the same key, and PostgreSQL
 * releases it automatically on commit or rollback, so it can never leak. The second creator waits,
 * then its overlap query sees the committed lot and fails. Reads never take this lock. One global
 * key serializes only creations, which is enough for the expected write volume.
 * <p>
 * JdbcTemplate is used because the function returns void, which JPA native queries map poorly. It
 * joins the current JPA transaction because JpaTransactionManager exposes the same JDBC connection.
 */
@Component
public class LandLotCreationLock {

	/** Arbitrary constant shared by every creation. */
	static final long LAND_LOT_CREATION_LOCK_KEY = 4_126_950_001L;

	private final JdbcTemplate jdbcTemplate;

	public LandLotCreationLock(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/** Must run inside the creation transaction; outside of one the lock would be released at once. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void acquire() {
		jdbcTemplate.query("SELECT pg_advisory_xact_lock(?)", resultSet -> null, LAND_LOT_CREATION_LOCK_KEY);
	}

}
