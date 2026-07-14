package com.emergency.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dispatch = one unit assigned to one incident.
 * A multi-casualty incident has multiple Dispatch records.
 * Tracks full lifecycle: assigned → arrived → completed.
 */
@Entity
@Table(name = "dispatches", indexes = {
    @Index(name = "idx_dispatch_incident", columnList = "incident_id"),
    @Index(name = "idx_dispatch_ambulance", columnList = "ambulance_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_id", nullable = false)
    private Ambulance ambulance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    // Routing snapshot captured at dispatch time
    private Integer estimatedEtaSeconds;
    private Double routeDistanceMeters;

    @Column(columnDefinition = "TEXT")
    private String routePolyline;   // Encoded polyline for map rendering

    @CreationTimestamp
    private LocalDateTime dispatchedAt;

    private LocalDateTime arrivedAt;
    private LocalDateTime completedAt;
}
