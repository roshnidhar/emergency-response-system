package com.emergency.dto;

import com.emergency.model.Ambulance;
import com.emergency.model.Incident;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// ── Incident ──────────────────────────────────────────────────────────────────

@Data
class IncidentCreateRequest {
    @NotNull private Incident.IncidentType type;
    @NotNull private Incident.Severity severity;
    @NotNull @DecimalMin("-90") @DecimalMax("90") private Double lat;
    @NotNull @DecimalMin("-180") @DecimalMax("180") private Double lng;
    private String address;
    private String locationNotes;
    private String callerName;
    private String callerPhone;
    private String description;
    @Min(1) @Max(100) private int patientCount = 1;
}

@Data
class IncidentResponse {
    private UUID id;
    private String incidentNumber;
    private Incident.IncidentType type;
    private Incident.Severity severity;
    private Incident.IncidentStatus status;
    private Double lat;
    private Double lng;
    private String address;
    private int patientCount;
    private LocalDateTime reportedAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime firstResponseAt;
    private LocalDateTime resolvedAt;
}

// ── Ambulance ─────────────────────────────────────────────────────────────────

@Data
class AmbulanceCreateRequest {
    @NotBlank private String unitNumber;
    private Ambulance.VehicleType vehicleType = Ambulance.VehicleType.BLS;
    private UUID stationId;
}

@Data
class LocationUpdateRequest {
    @NotNull @DecimalMin("-90") @DecimalMax("90") private Double lat;
    @NotNull @DecimalMin("-180") @DecimalMax("180") private Double lng;
    private double speed = 0.0;
}

// ── Hospital ──────────────────────────────────────────────────────────────────

@Data
class HospitalCreateRequest {
    @NotBlank private String name;
    @NotBlank private String address;
    @NotNull private Double lat;
    @NotNull private Double lng;
    @Min(0) private int totalBeds;
    @Min(0) private int icuTotal;
    private boolean traumaCenter;
    private List<String> specializations = List.of();
}

@Data
class BedUpdateRequest {
    @Min(0) private int availableBeds;
    @Min(0) private int icuAvailable;
}

// ── Dispatch ──────────────────────────────────────────────────────────────────

@Data
class AssignHospitalRequest {
    @NotNull private UUID hospitalId;
}
