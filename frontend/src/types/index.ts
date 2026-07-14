export type Severity = 'P1_CRITICAL' | 'P2_SERIOUS' | 'P3_MINOR' | 'P4_DECEASED'
export type IncidentStatus =
  | 'PENDING' | 'DISPATCHING' | 'DISPATCHED'
  | 'ON_SCENE' | 'TRANSPORTING' | 'RESOLVED' | 'CANCELLED'
export type IncidentType =
  | 'CARDIAC_ARREST' | 'TRAUMA' | 'FIRE' | 'ACCIDENT'
  | 'STROKE' | 'RESPIRATORY' | 'HAZMAT' | 'PSYCHIATRIC' | 'OTHER'
export type AmbulanceStatus =
  | 'AVAILABLE' | 'DISPATCHED' | 'EN_ROUTE' | 'ON_SCENE'
  | 'TRANSPORTING' | 'AT_HOSPITAL' | 'MAINTENANCE' | 'OFFLINE'

export interface Incident {
  id: string
  incidentNumber: string
  type: IncidentType
  severity: Severity
  status: IncidentStatus
  lat: number
  lng: number
  address?: string
  patientCount: number
  reportedAt: string
  dispatchedAt?: string
  firstResponseAt?: string
  resolvedAt?: string
}

export interface Ambulance {
  id: string
  unitNumber: string
  vehicleType: string
  status: AmbulanceStatus
  currentLat?: number
  currentLng?: number
  lastLocationUpdate?: string
  isActive: boolean
}

export interface Hospital {
  id: string
  name: string
  address: string
  lat: number
  lng: number
  totalBeds: number
  availableBeds: number
  icuTotal: number
  icuAvailable: number
  traumaCenter: boolean
  specializations: string[]
}

export interface DashboardStats {
  activeIncidents: number
  availableAmbulances: number
  totalAvailableBeds: number
  connectedClients: number
  priorityQueue: Incident[]
}

export type WSMessage =
  | { type: 'AMBULANCE_LOCATION'; ambulanceId: string; lat: number; lng: number; timestamp: number }
  | { type: 'DISPATCH_CREATED'; dispatchId: string; incidentId: string; incidentNumber: string; ambulanceId: string; ambulanceUnit: string; etaSeconds: number; etaMinutes: number; polyline: string; severity: Severity }
  | { type: 'INCIDENT_UPDATE'; incidentId: string; incidentNumber: string; status: IncidentStatus; severity: Severity; event: string; timestamp: number }
  | { type: 'HOSPITAL_BEDS'; hospitalId: string; hospitalName: string; availableBeds: number; icuAvailable: number; timestamp: number }
  | { type: 'CONNECTED'; message: string; sessionId: string }
