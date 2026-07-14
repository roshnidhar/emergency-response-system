package com.emergency.repository;

import com.emergency.model.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DispatchRepository extends JpaRepository<Dispatch, UUID> {
    List<Dispatch> findByIncidentId(UUID incidentId);
    List<Dispatch> findByAmbulanceIdAndCompletedAtIsNull(UUID ambulanceId);
}
