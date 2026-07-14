package com.emergency.service;

import com.emergency.model.*;
import com.emergency.repository.*;
import com.emergency.websocket.EmergencyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * DispatchService orchestrates the full emergency lifecycle:
 *
 *   1. Incident reported → auto-assigned incident number
 *   2. Find nearest available ambulance (geospatial query)
 *   3. Route via OSRM → calculate ETA
 *   4. Dispatch: mark ambulance EN_ROUTE, incident DISPATCHED
 *   5. Broadcast update over WebSocket to all connected dashboards
 *   6. Find nearest hospital with available beds for transport
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private final IncidentRepository incidentRepo;
    private final AmbulanceRepository ambulanceRepo;
    private final HospitalRepository hospitalRepo;
    private final DispatchRepository dispatchRepo;
    private final RoutingService routingService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmergencyWebSocketHandler wsHandler;

    private static final double SEARCH_RADIUS_KM = 50.0;

    // ─── Incident Creation ─────────────────────────────────────────────────────

    @Transactional
    public Incident createIncident(Incident incident) {
        incident.setIncidentNumber(generateIncidentNumber());
        incident.setStatus(Incident.IncidentStatus.PENDING);
        Incident saved = incidentRepo.save(incident);

        log.info("Incident created: {} [{}]", saved.getIncidentNumber(), saved.getSeverity());

        // Auto-dispatch P1 and P2 immediately
        if (saved.getSeverity().priorityScore() >= 3) {
            autoDispatch(saved);
        }

        return saved;
    }

    // ─── Auto Dispatch ─────────────────────────────────────────────────────────

    @Transactional
    public Dispatch autoDispatch(Incident incident) {
        incident.setStatus(Incident.IncidentStatus.DISPATCHING);
        incidentRepo.save(incident);

        // 1. Find nearest available ambulance
        List<Ambulance> candidates = ambulanceRepo.findAvailableWithinRadius(
            incident.getLat(), incident.getLng(), SEARCH_RADIUS_KM
        );

        if (candidates.isEmpty()) {
            log.warn("No available ambulances within {}km for incident {}", SEARCH_RADIUS_KM, incident.getIncidentNumber());
            incident.setStatus(Incident.IncidentStatus.PENDING);
            incidentRepo.save(incident);
            wsHandler.broadcastIncidentUpdate(incident, "NO_UNITS_AVAILABLE");
            return null;
        }

        // 2. Pick best ambulance: nearest that's also compatible with severity
        Ambulance bestUnit = selectBestUnit(candidates, incident);

        // 3. Get route + ETA from OSRM
        RoutingService.RouteResult route = routingService.getRoute(
            bestUnit.getCurrentLat(), bestUnit.getCurrentLng(),
            incident.getLat(), incident.getLng()
        );

        // 4. Create dispatch record
        Dispatch dispatch = Dispatch.builder()
            .incident(incident)
            .ambulance(bestUnit)
            .estimatedEtaSeconds((int) route.getDurationSeconds())
            .routeDistanceMeters(route.getDistanceMeters())
            .routePolyline(route.getPolyline())
            .build();
        dispatchRepo.save(dispatch);

        // 5. Update statuses
        bestUnit.setStatus(Ambulance.AmbulanceStatus.EN_ROUTE);
        ambulanceRepo.save(bestUnit);

        incident.setStatus(Incident.IncidentStatus.DISPATCHED);
        incident.setDispatchedAt(LocalDateTime.now());
        incidentRepo.save(incident);

        // 6. Cache dispatch state in Redis for fast WebSocket reads
        cacheDispatchState(dispatch, route);

        // 7. Broadcast to all dashboard clients
        wsHandler.broadcastDispatch(dispatch, route);

        log.info("Dispatched {} → incident {} | ETA: {}min",
            bestUnit.getUnitNumber(), incident.getIncidentNumber(),
            String.format("%.1f", route.getEtaMinutes()));

        return dispatch;
    }

    // ─── Hospital Selection ────────────────────────────────────────────────────

    /**
     * Find the nearest hospital with available beds, optionally filtering
     * by specialization (e.g. "cardiac" for cardiac arrest incidents).
     */
    public List<Hospital> findSuitableHospitals(double lat, double lng, String specialization) {
        List<Hospital> hospitals = hospitalRepo.findNearestWithBeds(lat, lng, 80.0);

        if (specialization != null && !specialization.isBlank()) {
            hospitals = hospitals.stream()
                .filter(h -> h.getSpecializations().contains(specialization.toLowerCase()))
                .toList();
        }

        return hospitals;
    }

    @Transactional
    public void assignHospital(UUID dispatchId, UUID hospitalId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId)
            .orElseThrow(() -> new RuntimeException("Dispatch not found: " + dispatchId));
        Hospital hospital = hospitalRepo.findById(hospitalId)
            .orElseThrow(() -> new RuntimeException("Hospital not found: " + hospitalId));

        dispatch.setHospital(hospital);
        dispatch.getIncident().setStatus(Incident.IncidentStatus.TRANSPORTING);
        dispatchRepo.save(dispatch);

        wsHandler.broadcastIncidentUpdate(dispatch.getIncident(), "TRANSPORTING_TO_" + hospital.getName());
    }

    // ─── Status Updates ────────────────────────────────────────────────────────

    @Transactional
    public void updateAmbulanceLocation(UUID ambulanceId, double lat, double lng) {
        // Write location to Redis (fast path — don't hit DB on every GPS ping)
        String key = "ambulance:location:" + ambulanceId;
        Map<String, Object> location = Map.of("lat", lat, "lng", lng, "ts", System.currentTimeMillis());
        redisTemplate.opsForValue().set(key, location, 60, TimeUnit.SECONDS);

        // Broadcast live position to WebSocket subscribers
        wsHandler.broadcastAmbulanceLocation(ambulanceId.toString(), lat, lng);

        // Periodically persist to DB (every 10th update would be production pattern)
        ambulanceRepo.findById(ambulanceId).ifPresent(a -> {
            a.setCurrentLat(lat);
            a.setCurrentLng(lng);
            a.setLastLocationUpdate(LocalDateTime.now());
            ambulanceRepo.save(a);
        });
    }

    @Transactional
    public void markOnScene(UUID dispatchId) {
        Dispatch dispatch = dispatchRepo.findById(dispatchId).orElseThrow();
        dispatch.setArrivedAt(LocalDateTime.now());
        dispatch.getAmbulance().setStatus(Ambulance.AmbulanceStatus.ON_SCENE);
        dispatch.getIncident().setStatus(Incident.IncidentStatus.ON_SCENE);
        dispatch.getIncident().setFirstResponseAt(LocalDateTime.now());
        dispatchRepo.save(dispatch);
        wsHandler.broadcastIncidentUpdate(dispatch.getIncident(), "UNIT_ON_SCENE");
    }

    @Transactional
    public void resolveIncident(UUID incidentId) {
        Incident incident = incidentRepo.findById(incidentId).orElseThrow();
        incident.setStatus(Incident.IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());

        // Free up the ambulance
        incident.getDispatches().forEach(d -> {
            d.setCompletedAt(LocalDateTime.now());
            d.getAmbulance().setStatus(Ambulance.AmbulanceStatus.AVAILABLE);
        });

        incidentRepo.save(incident);
        wsHandler.broadcastIncidentUpdate(incident, "RESOLVED");
    }

    // ─── Dashboard Stats ───────────────────────────────────────────────────────

    public Map<String, Object> getDashboardStats() {
        long activeIncidents = incidentRepo.countByStatus(Incident.IncidentStatus.DISPATCHED)
            + incidentRepo.countByStatus(Incident.IncidentStatus.ON_SCENE)
            + incidentRepo.countByStatus(Incident.IncidentStatus.PENDING);
        long availableUnits = ambulanceRepo.findByStatusAndIsActiveTrue(Ambulance.AmbulanceStatus.AVAILABLE).size();
        long totalHospitalBeds = hospitalRepo.findAll().stream().mapToLong(Hospital::getAvailableBeds).sum();

        return Map.of(
            "activeIncidents", activeIncidents,
            "availableAmbulances", availableUnits,
            "totalAvailableBeds", totalHospitalBeds,
            "priorityQueue", incidentRepo.findActiveIncidentsByPriority().stream().limit(10).toList()
        );
    }

    // ─── Private Helpers ───────────────────────────────────────────────────────

    private Ambulance selectBestUnit(List<Ambulance> candidates, Incident incident) {
        // For HAZMAT incidents prefer HAZMAT unit; for critical prefer ALS/CRITICAL
        return candidates.stream()
            .filter(a -> isCompatible(a, incident))
            .findFirst()
            .orElse(candidates.get(0)); // fallback to nearest
    }

    private boolean isCompatible(Ambulance ambulance, Incident incident) {
        return switch (incident.getType()) {
            case HAZMAT -> ambulance.getVehicleType() == Ambulance.VehicleType.HAZMAT;
            case FIRE   -> ambulance.getVehicleType() == Ambulance.VehicleType.FIRE
                           || ambulance.getVehicleType() == Ambulance.VehicleType.ALS;
            case CARDIAC_ARREST, STROKE -> ambulance.getVehicleType() == Ambulance.VehicleType.ALS
                                        || ambulance.getVehicleType() == Ambulance.VehicleType.CRITICAL;
            default -> true;
        };
    }

    private void cacheDispatchState(Dispatch dispatch, RoutingService.RouteResult route) {
        String key = "dispatch:" + dispatch.getId();
        Map<String, Object> state = Map.of(
            "incidentId", dispatch.getIncident().getId().toString(),
            "ambulanceId", dispatch.getAmbulance().getId().toString(),
            "etaSeconds", route.getDurationSeconds(),
            "polyline", route.getPolyline()
        );
        redisTemplate.opsForValue().set(key, state, 2, TimeUnit.HOURS);
    }

    private String generateIncidentNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String unique = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "INC-" + datePart + "-" + unique;
    }
}
