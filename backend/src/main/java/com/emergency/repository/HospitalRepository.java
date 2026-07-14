package com.emergency.repository;

import com.emergency.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    List<Hospital> findByIsActiveTrueAndAvailableBedsGreaterThan(int minBeds);

    /**
     * Nearest hospitals with available beds, sorted by straight-line distance.
     * In production you'd also call the routing API to get real drive time.
     */
    @Query("""
        SELECT h FROM Hospital h
        WHERE h.isActive = true
          AND h.availableBeds > 0
          AND (6371 * acos(
                cos(radians(:lat)) * cos(radians(h.lat))
                * cos(radians(h.lng) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(h.lat))
              )) <= :radiusKm
        ORDER BY (6371 * acos(
                cos(radians(:lat)) * cos(radians(h.lat))
                * cos(radians(h.lng) - radians(:lng))
                + sin(radians(:lat)) * sin(radians(h.lat))
              )) ASC
        """)
    List<Hospital> findNearestWithBeds(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radiusKm") double radiusKm
    );
}
