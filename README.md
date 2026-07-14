# 🚑 Smart Emergency Response System

A full-stack real-time emergency dispatch platform that automates ambulance assignment, live GPS tracking, and hospital bed management.

---

## 🎥 What It Does

When a dispatcher reports a **P1 (Critical)** or **P2 (Serious)** incident:

1. The system instantly finds the nearest compatible ambulance within 50km using a **Haversine geospatial query** in PostgreSQL
2. Calls the **OSRM routing API** to get the real driving ETA and route
3. Assigns the unit and updates all statuses atomically in a single database transaction
4. Pushes a live update to **every connected dashboard** via WebSocket — dispatchers see the ambulance moving on the map in real time

---

## 🏗️ Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Backend | Spring Boot 3 (Java 21) | REST API, dispatch logic, WebSocket server |
| Database | PostgreSQL 16 | Relational data, geospatial Haversine queries |
| Cache | Redis 7 | GPS location caching (60s TTL), OSRM route caching |
| Real-time | WebSocket (Spring) | Live GPS tracking pushed to all dashboards |
| Routing | OSRM (OpenStreetMap) | Real driving ETA — free, no API key needed |
| Frontend | React 18 + Vite + Tailwind | Live dashboard, Leaflet interactive map |
| Container | Docker + Nginx | Single-command startup |

---

## 🚀 Quick Start

\`\`\`bash
git clone https://github.com/YOUR-USERNAME/emergency-response-system.git
cd emergency-response-system
docker-compose up --build
\`\`\`

| Service | URL |
|---|---|
| Dashboard | http://localhost:3000 |
| API Swagger | http://localhost:8080/api/swagger-ui.html |
| WebSocket | ws://localhost:8080/api/ws/emergency |

First run takes **8–15 minutes**. Ready when you see:
\`\`\`
backend | Started EmergencyResponseApplication
\`\`\`

---

## 🌱 Seed Test Data

\`\`\`bash
# 1. Add an ambulance
curl -X POST http://localhost:8080/api/ambulances \
  -H "Content-Type: application/json" \
  -d '{"unitNumber":"AMB-001","vehicleType":"ALS"}'

# 2. Set its GPS location (copy the id from above)
curl -X PATCH http://localhost:8080/api/ambulances/<ID>/location \
  -H "Content-Type: application/json" \
  -d '{"lat":19.0760,"lng":72.8777}'

# 3. Add a hospital
curl -X POST http://localhost:8080/api/hospitals \
  -H "Content-Type: application/json" \
  -d '{"name":"City Hospital","address":"MG Road","lat":19.0800,"lng":72.8800,"totalBeds":100,"icuTotal":20,"traumaCenter":true,"specializations":["cardiac","stroke"]}'

# 4. Report a P1 incident — auto-dispatches instantly
curl -X POST http://localhost:8080/api/incidents \
  -H "Content-Type: application/json" \
  -d '{"type":"CARDIAC_ARREST","severity":"P1_CRITICAL","lat":19.0750,"lng":72.8750,"address":"Andheri West","callerName":"Raj","patientCount":1}'
\`\`\`

---

## 📡 API Reference

### Incidents
| Method | Endpoint | Description |
|---|---|---|
| POST | /api/incidents | Report emergency (P1/P2 auto-dispatch) |
| GET | /api/incidents/active | Active incidents, priority sorted |
| POST | /api/incidents/{id}/dispatch | Manual dispatch |
| POST | /api/incidents/{id}/resolve | Close incident |

### Ambulances
| Method | Endpoint | Description |
|---|---|---|
| GET | /api/ambulances | All units + status |
| POST | /api/ambulances | Register new unit |
| PATCH | /api/ambulances/{id}/location | Update GPS |
| PATCH | /api/ambulances/{id}/status | Update status |

### Hospitals
| Method | Endpoint | Description |
|---|---|---|
| GET | /api/hospitals | All hospitals + bed counts |
| POST | /api/hospitals | Add hospital |
| PATCH | /api/hospitals/{id}/beds | Update bed availability |
| GET | /api/hospitals/nearest | Nearest with available beds |

### Dashboard
| Method | Endpoint | Description |
|---|---|---|
| GET | /api/dashboard/stats | Active incidents, units, beds, WS clients |

---

## 📊 WebSocket Events

Connect to ws://localhost:8080/api/ws/emergency

| Event | When it fires |
|---|---|
| AMBULANCE_LOCATION | GPS ping received |
| DISPATCH_CREATED | Unit assigned to incident |
| INCIDENT_UPDATE | Status changed |
| HOSPITAL_BEDS | Bed count updated |

---

## 🔥 Load Testing

Load tested with k6 simulating 150 concurrent dispatchers (22,000+ requests):

| Metric | Result |
|---|---|
| Sustained throughput | 106 requests/sec |
| p95 latency | 74ms |
| Error rate | 0% |
| Peak virtual users | 150 |

Bugs found and fixed during load testing:

Bug 1 - Redis cache serialization: RouteResult was annotated @Cacheable but did not implement Serializable. Fixed by adding implements java.io.Serializable. Took error rate from 70% to 0%.

Bug 2 - Incident number collision: Random 4-digit suffix (9,000 values) caused birthday-paradox collisions at 40 dispatches/sec. Fixed by switching to UUID-derived suffix.

---

## 🗂️ Project Structure

\`\`\`
emergency-response-system/
├── backend/                          Spring Boot (Java 21)
│   └── src/main/java/com/emergency/
│       ├── controller/               REST endpoints
│       ├── service/                  Dispatch logic, routing
│       ├── model/                    JPA entities
│       ├── repository/               DB queries (incl. Haversine)
│       ├── websocket/                Live tracking handler
│       └── config/                   Redis, WebSocket, Security
├── frontend/                         React 18 + Vite
│   └── src/
│       ├── pages/                    Dashboard, Ambulances, Hospitals
│       ├── components/map/           Leaflet live map
│       └── lib/                      API client, WebSocket hook
├── loadtest/                         k6 load test scripts
└── docker-compose.yml
\`\`\`

---

## 🏁 Stop

\`\`\`bash
docker-compose down        # keep data
docker-compose down -v     # wipe data
\`\`\`

---

## ⚠️ Production Checklist (not implemented - portfolio project)

- [ ] JWT authentication on all endpoints
- [ ] PostGIS for indexed geospatial queries at scale
- [ ] Self-hosted OSRM or paid routing API for SLA
- [ ] Exponential backoff on WebSocket reconnection
- [ ] Flyway migrations instead of ddl-auto: update
- [ ] Prometheus + Grafana metrics
