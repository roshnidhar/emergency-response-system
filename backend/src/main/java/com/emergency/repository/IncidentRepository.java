package com.emergency.repository;

import com.emergency.model.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findByIncidentNumber(String incidentNumber);

    List<Incident> findByStatusIn(List<Incident.IncidentStatus> statuses);

    Page<Incident> findAllByOrderByReportedAtDesc(Pageable pageable);

    // Active incidents sorted by severity score (P1 first)
    @Query("""
        SELECT i FROM Incident i
        WHERE i.status NOT IN ('RESOLVED', 'CANCELLED')
        ORDER BY
            CASE i.severity
                WHEN 'P1_CRITICAL' THEN 4
                WHEN 'P2_SERIOUS'  THEN 3
                WHEN 'P3_MINOR'    THEN 2
                ELSE 1
            END DESC,
            i.reportedAt ASC
        """)
    List<Incident> findActiveIncidentsByPriority();

    long countByStatus(Incident.IncidentStatus status);
}
