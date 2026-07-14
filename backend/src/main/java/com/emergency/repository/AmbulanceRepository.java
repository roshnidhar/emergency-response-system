package com.emergency.repository;

import com.emergency.model.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AmbulanceRepository extends JpaRepository<Ambulance, UUID> {

    List<Ambulance> findByStatusAndIsActiveTrue(Ambulance.AmbulanceStatus status);

    /**
     * Haversine formula in JPQL — finds available ambulances within radius.
     * This is the core geospatial query; in a PostGIS setup you'd use ST_DWithin.
     */
    @Query("""
        SELECT a FROM Ambulance a
        WHERE a.status = 'AVAILABLE'
          AND a.isActive = true
          AND a.currentLat IS NOT NULL
          AND (6371 * acos(
                cos(radians(:lat)) * cos(radians(a.currentLat))
                * cos(radians(a.currentLng) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(a.currentLat))
              )) <= :radiusKm
        ORDER BY (6371 * acos(
                cos(radians(:lat)) * cos(radians(a.currentLat))
                * cos(radians(a.currentLng) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(a.currentLat))
              )) ASC
        """)
    List<Ambulance> findAvailableWithinRadius(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radiusKm") double radiusKm
    );
}
