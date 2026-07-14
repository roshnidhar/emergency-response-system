package com.emergency.controller;

import com.emergency.model.Hospital;
import com.emergency.repository.HospitalRepository;
import com.emergency.service.DispatchService;
import com.emergency.websocket.EmergencyWebSocketHandler;
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
@RequestMapping("/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalRepository hospitalRepo;
    private final DispatchService dispatchService;
    private final EmergencyWebSocketHandler wsHandler;

    @GetMapping
    public ResponseEntity<List<Hospital>> listAll() {
        return ResponseEntity.ok(hospitalRepo.findAll());
    }

    @GetMapping("/nearest")
    public ResponseEntity<List<Hospital>> nearest(
        @RequestParam double lat,
        @RequestParam double lng,
        @RequestParam(required = false) String specialization
    ) {
        return ResponseEntity.ok(dispatchService.findSuitableHospitals(lat, lng, specialization));
    }

    @PostMapping
    public ResponseEntity<Hospital> create(@Valid @RequestBody CreateRequest req) {
        Hospital h = Hospital.builder()
            .name(req.getName())
            .address(req.getAddress())
            .lat(req.getLat())
            .lng(req.getLng())
            .totalBeds(req.getTotalBeds())
            .availableBeds(req.getTotalBeds())
            .icuTotal(req.getIcuTotal())
            .icuAvailable(req.getIcuTotal())
            .traumaCenter(req.isTraumaCenter())
            .specializations(req.getSpecializations())
            .build();
        return ResponseEntity.ok(hospitalRepo.save(h));
    }

    /** Hospital updates bed availability — triggers WS broadcast to dashboards. */
    @PatchMapping("/{id}/beds")
    public ResponseEntity<Hospital> updateBeds(
        @PathVariable UUID id,
        @Valid @RequestBody BedRequest req
    ) {
        return hospitalRepo.findById(id).map(h -> {
            h.setAvailableBeds(req.getAvailableBeds());
            h.setIcuAvailable(req.getIcuAvailable());
            Hospital saved = hospitalRepo.save(h);

            // Push real-time update to all dashboard clients
            wsHandler.broadcastHospitalBedUpdate(
                id.toString(), h.getName(),
                req.getAvailableBeds(), req.getIcuAvailable()
            );
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{dispatchId}/assign-hospital")
    public ResponseEntity<Map<String, String>> assignHospital(
        @PathVariable UUID dispatchId,
        @RequestBody Map<String, UUID> body
    ) {
        dispatchService.assignHospital(dispatchId, body.get("hospitalId"));
        return ResponseEntity.ok(Map.of("status", "assigned"));
    }

    @Data
    static class CreateRequest {
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
    static class BedRequest {
        @Min(0) private int availableBeds;
        @Min(0) private int icuAvailable;
    }
}
