package com.emergency.controller;

import com.emergency.service.DispatchService;
import com.emergency.websocket.EmergencyWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DispatchService dispatchService;
    private final EmergencyWebSocketHandler wsHandler;

    /** Single endpoint for dashboard — active incidents, available units, bed counts. */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = dispatchService.getDashboardStats();
        stats = new java.util.HashMap<>(stats);
        stats.put("connectedClients", wsHandler.getConnectedClientCount());
        return ResponseEntity.ok(stats);
    }
}
