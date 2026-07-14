package com.emergency.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Routing service backed by OSRM (Open Source Routing Machine).
 *
 * The public OSRM endpoint is free and requires no API key — great for development.
 * For production swap to: Google Maps Directions API, Mapbox, or self-hosted OSRM.
 *
 * Endpoint format:
 *   GET /route/v1/driving/{lng1},{lat1};{lng2},{lat2}?overview=full&geometries=polyline
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutingService {

    private final WebClient.Builder webClientBuilder;

    @Value("${app.routing.osrm-base-url}")
    private String osrmBaseUrl;

    /**
     * Get the fastest driving route between two coordinates.
     * Cached in Redis — same origin/destination pair returns instantly on repeat calls.
     */
    @Cacheable(value = "routes", key = "#fromLat + ',' + #fromLng + '-' + #toLat + ',' + #toLng")
    public RouteResult getRoute(double fromLat, double fromLng, double toLat, double toLng) {
        String url = String.format(
            "%s/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=polyline",
            osrmBaseUrl, fromLng, fromLat, toLng, toLat   // OSRM uses lng,lat order
        );

        try {
            OsrmResponse response = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(OsrmResponse.class)
                .block();

            if (response != null && !response.getRoutes().isEmpty()) {
                OsrmRoute route = response.getRoutes().get(0);
                return RouteResult.builder()
                    .durationSeconds(route.getDuration())
                    .distanceMeters(route.getDistance())
                    .polyline(route.getGeometry())
                    .etaMinutes(route.getDuration() / 60.0)
                    .build();
            }
        } catch (Exception e) {
            log.warn("OSRM routing failed for ({},{}) → ({},{}): {}", fromLat, fromLng, toLat, toLng, e.getMessage());
        }

        // Fallback: straight-line Haversine estimate if OSRM is unavailable
        return fallbackEstimate(fromLat, fromLng, toLat, toLng);
    }

    /**
     * Haversine straight-line distance used as routing fallback.
     * Applies a 1.3x road factor multiplier (standard EMS estimate).
     */
    private RouteResult fallbackEstimate(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371_000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double straightLine = R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double roadDistance  = straightLine * 1.3;
        double durationSecs  = (roadDistance / 60.0) * (60.0 / 1000.0) * 60; // ~60 km/h

        return RouteResult.builder()
            .durationSeconds(durationSecs)
            .distanceMeters(roadDistance)
            .polyline("")
            .etaMinutes(durationSecs / 60.0)
            .build();
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RouteResult implements java.io.Serializable {
        private double durationSeconds;
        private double distanceMeters;
        private String polyline;
        private double etaMinutes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OsrmResponse {
        private List<OsrmRoute> routes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OsrmRoute {
        private double duration;   // seconds
        private double distance;   // meters
        private String geometry;   // encoded polyline
    }
}
