package com.emergency.websocket;

import com.emergency.model.Dispatch;
import com.emergency.model.Incident;
import com.emergency.service.RoutingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler — pushes real-time events to all connected dashboard clients.
 *
 * Message types broadcast:
 *   - AMBULANCE_LOCATION  → GPS ping every few seconds
 *   - DISPATCH_CREATED    → new unit assigned to incident
 *   - INCIDENT_UPDATE     → status change (on scene, resolved, etc.)
 *   - HOSPITAL_BEDS       → bed availability changed
 *
 * All clients subscribe at: ws://localhost:8080/api/ws/emergency
 * Clients can filter by sending a subscription message: {"type":"SUBSCRIBE","incidentId":"..."}
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmergencyWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    // Thread-safe session registry
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WS client connected: {} | total={}", session.getId(), sessions.size());

        // Send current system state on connect
        sendToSession(session, Map.of(
            "type", "CONNECTED",
            "message", "Emergency Response System connected",
            "sessionId", session.getId()
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WS client disconnected: {} | total={}", session.getId(), sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Handle client → server messages (e.g. subscribe to specific incident)
        try {
            Map<?, ?> payload = objectMapper.readValue(message.getPayload(), Map.class);
            log.debug("WS message from {}: {}", session.getId(), payload);
            // Future: topic-based subscriptions per incident/ambulance
        } catch (IOException e) {
            log.warn("Invalid WS message: {}", e.getMessage());
        }
    }

    // ─── Broadcast Methods ─────────────────────────────────────────────────────

    public void broadcastAmbulanceLocation(String ambulanceId, double lat, double lng) {
        broadcast(Map.of(
            "type", "AMBULANCE_LOCATION",
            "ambulanceId", ambulanceId,
            "lat", lat,
            "lng", lng,
            "timestamp", System.currentTimeMillis()
        ));
    }

    public void broadcastDispatch(Dispatch dispatch, RoutingService.RouteResult route) {
        broadcast(Map.of(
            "type", "DISPATCH_CREATED",
            "dispatchId", dispatch.getId().toString(),
            "incidentId", dispatch.getIncident().getId().toString(),
            "incidentNumber", dispatch.getIncident().getIncidentNumber(),
            "ambulanceId", dispatch.getAmbulance().getId().toString(),
            "ambulanceUnit", dispatch.getAmbulance().getUnitNumber(),
            "etaSeconds", route.getDurationSeconds(),
            "etaMinutes", route.getEtaMinutes(),
            "polyline", route.getPolyline(),
            "severity", dispatch.getIncident().getSeverity().name()
        ));
    }

    public void broadcastIncidentUpdate(Incident incident, String event) {
        broadcast(Map.of(
            "type", "INCIDENT_UPDATE",
            "incidentId", incident.getId().toString(),
            "incidentNumber", incident.getIncidentNumber(),
            "status", incident.getStatus().name(),
            "severity", incident.getSeverity().name(),
            "event", event,
            "timestamp", System.currentTimeMillis()
        ));
    }

    public void broadcastHospitalBedUpdate(String hospitalId, String hospitalName, int available, int icu) {
        broadcast(Map.of(
            "type", "HOSPITAL_BEDS",
            "hospitalId", hospitalId,
            "hospitalName", hospitalName,
            "availableBeds", available,
            "icuAvailable", icu,
            "timestamp", System.currentTimeMillis()
        ));
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void broadcast(Map<String, Object> payload) {
        if (sessions.isEmpty()) return;
        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);
            sessions.removeIf(session -> {
                if (!session.isOpen()) return true;
                sendToSession(session, message);
                return false;
            });
        } catch (IOException e) {
            log.error("WS broadcast failed: {}", e.getMessage());
        }
    }

    private void sendToSession(WebSocketSession session, Map<String, Object> payload) {
        try {
            sendToSession(session, new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException e) {
            log.warn("Failed to serialize WS message: {}", e.getMessage());
        }
    }

    private void sendToSession(WebSocketSession session, TextMessage message) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to send to session {}: {}", session.getId(), e.getMessage());
        }
    }

    public int getConnectedClientCount() {
        return sessions.size();
    }
}
