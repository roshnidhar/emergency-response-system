package com.emergency.controller;

import com.emergency.model.Incident;
import com.emergency.repository.IncidentRepository;
import com.emergency.service.DispatchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final DispatchService dispatchService;
    private final IncidentRepository incidentRepo;

    /** Report a new emergency incident. P1/P2 are auto-dispatched immediately. */
    @PostMapping
    public ResponseEntity<Incident> createIncident(@Valid @RequestBody CreateRequest req) {
        Incident incident = Incident.builder()
            .type(req.getType())
            .severity(req.getSeverity())
            .lat(req.getLat())
            .lng(req.getLng())
            .address(req.getAddress())
            .locationNotes(req.getLocationNotes())
            .callerName(req.getCallerName())
            .callerPhone(req.getCallerPhone())
            .description(req.getDescription())
            .patientCount(req.getPatientCount())
            .build();
        return ResponseEntity.ok(dispatchService.createIncident(incident));
    }

    /** Paginated incident history, newest first. */
    @GetMapping
    public ResponseEntity<Page<Incident>> listIncidents(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(incidentRepo.findAllByOrderByReportedAtDesc(PageRequest.of(page, size)));
    }

    /** Active incidents sorted by priority (P1 first). */
    @GetMapping("/active")
    public ResponseEntity<List<Incident>> getActiveIncidents() {
        return ResponseEntity.ok(incidentRepo.findActiveIncidentsByPriority());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getIncident(@PathVariable UUID id) {
        return incidentRepo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /** Manually trigger dispatch for a PENDING incident. */
    @PostMapping("/{id}/dispatch")
    public ResponseEntity<?> dispatchIncident(@PathVariable UUID id) {
        return incidentRepo.findById(id)
            .map(incident -> ResponseEntity.ok(dispatchService.autoDispatch(incident)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Map<String, String>> resolve(@PathVariable UUID id) {
        dispatchService.resolveIncident(id);
        return ResponseEntity.ok(Map.of("status", "resolved"));
    }

    @PatchMapping("/{id}/on-scene/{dispatchId}")
    public ResponseEntity<Map<String, String>> markOnScene(
        @PathVariable UUID id,
        @PathVariable UUID dispatchId
    ) {
        dispatchService.markOnScene(dispatchId);
        return ResponseEntity.ok(Map.of("status", "on_scene"));
    }

    // ── Inner DTO ─────────────────────────────────────────────────────────────
    @Data
    static class CreateRequest {
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
}
