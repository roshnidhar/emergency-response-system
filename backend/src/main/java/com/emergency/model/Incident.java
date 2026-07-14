package com.emergency.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "incidents", indexes = {
    @Index(name = "idx_incident_status", columnList = "status"),
    @Index(name = "idx_incident_severity", columnList = "severity"),
    @Index(name = "idx_incident_reported_at", columnList = "reportedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String incidentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.PENDING;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    private String address;
    private String locationNotes;

    private String callerName;
    private String callerPhone;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer patientCount = 1;

    @CreationTimestamp
    private LocalDateTime reportedAt;

    private LocalDateTime dispatchedAt;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
	
	@JsonIgnore
    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Dispatch> dispatches = new ArrayList<>();

    // ─── Enums ────────────────────────────────────────────────────────────────

    public enum IncidentType {
        CARDIAC_ARREST, TRAUMA, FIRE, ACCIDENT, STROKE,
        RESPIRATORY, HAZMAT, PSYCHIATRIC, OTHER
    }

    /**
     * Mirrors START triage protocol used by real EMS.
     * P1 = immediate life threat → dispatched first, always.
     */
    public enum Severity {
        P1_CRITICAL, P2_SERIOUS, P3_MINOR, P4_DECEASED;

        public int priorityScore() {
            return switch (this) {
                case P1_CRITICAL -> 4;
                case P2_SERIOUS  -> 3;
                case P3_MINOR    -> 2;
                case P4_DECEASED -> 1;
            };
        }
    }

    public enum IncidentStatus {
        PENDING, DISPATCHING, DISPATCHED, ON_SCENE, TRANSPORTING, RESOLVED, CANCELLED
    }
}
