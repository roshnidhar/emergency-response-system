package com.emergency.controller;

import com.emergency.model.Ambulance;
import com.emergency.repository.AmbulanceRepository;
import com.emergency.service.DispatchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ambulances")
@RequiredArgsConstructor
public class AmbulanceController {

    private final AmbulanceRepository ambulanceRepo;
    private final DispatchService dispatchService;

    @GetMapping
    public ResponseEntity<List<Ambulance>> listAll() {
        return ResponseEntity.ok(ambulanceRepo.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Ambulance>> listAvailable() {
        return ResponseEntity.ok(
            ambulanceRepo.findByStatusAndIsActiveTrue(Ambulance.AmbulanceStatus.AVAILABLE)
        );
    }

    @PostMapping
    public ResponseEntity<Ambulance> create(@Valid @RequestBody CreateRequest req) {
        Ambulance a = Ambulance.builder()
            .unitNumber(req.getUnitNumber())
            .vehicleType(req.getVehicleType())
            .build();
        return ResponseEntity.ok(ambulanceRepo.save(a));
    }

    /** Real-time GPS location update — called by ambulance device/app. */
    @PatchMapping("/{id}/location")
    public ResponseEntity<Map<String, String>> updateLocation(
        @PathVariable UUID id,
        @Valid @RequestBody LocationRequest req
    ) {
        dispatchService.updateAmbulanceLocation(id, req.getLat(), req.getLng());
        return ResponseEntity.ok(Map.of("status", "updated"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Ambulance> updateStatus(
        @PathVariable UUID id,
        @RequestBody Map<String, String> body
    ) {
        return ambulanceRepo.findById(id).map(a -> {
            a.setStatus(Ambulance.AmbulanceStatus.valueOf(body.get("status")));
            return ResponseEntity.ok(ambulanceRepo.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    @Data
    static class CreateRequest {
        @NotBlank private String unitNumber;
        private Ambulance.VehicleType vehicleType = Ambulance.VehicleType.BLS;
    }

    @Data
    static class LocationRequest {
        @NotNull @DecimalMin("-90") @DecimalMax("90") private Double lat;
        @NotNull @DecimalMin("-180") @DecimalMax("180") private Double lng;
        private double speed = 0.0;
    }
}
