package io.github.eduardokaio.landmarketplace.landlot.infrastructure;

import java.util.UUID;

import io.github.eduardokaio.landmarketplace.landlot.domain.LandLot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LandLotRepository extends JpaRepository<LandLot, UUID> {

}
