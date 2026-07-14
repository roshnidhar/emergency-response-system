package com.emergency.repository;

import com.emergency.model.FireStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FireStationRepository extends JpaRepository<FireStation, UUID> {}
