package com.emergency.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ambulances", indexes = {
    @Index(name = "idx_ambulance_status", columnList = "status"),
    @Index(name = "idx_ambulance_unit", columnList = "unitNumber", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ambulance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String unitNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VehicleType vehicleType = VehicleType.BLS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AmbulanceStatus status = AmbulanceStatus.AVAILABLE;

    // GPS — persisted on periodic flush; real-time state lives in Redis
    private Double currentLat;
    private Double currentLng;
    private LocalDateTime lastLocationUpdate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private FireStation station;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

	@JsonIgnore
    @OneToMany(mappedBy = "ambulance")
    @Builder.Default
    private List<Dispatch> dispatches = new ArrayList<>();

    // ─── Enums ────────────────────────────────────────────────────────────────

    public enum AmbulanceStatus {
        AVAILABLE, DISPATCHED, EN_ROUTE, ON_SCENE, TRANSPORTING, AT_HOSPITAL, MAINTENANCE, OFFLINE;

        public boolean isAvailableForDispatch() {
            return this == AVAILABLE;
        }
    }

    public enum VehicleType {
        BLS,      // Basic Life Support
        ALS,      // Advanced Life Support
        CRITICAL, // Critical Care Transport
        FIRE,
        HAZMAT
    }
}
