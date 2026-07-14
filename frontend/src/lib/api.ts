const BASE = '/api'  // proxied by Vite to http://localhost:8080/api

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  })
  if (!res.ok) throw new Error(`API ${res.status}: ${await res.text()}`)
  return res.json()
}

export const api = {
  getStats:             ()           => request<any>('/dashboard/stats'),
  getActiveIncidents:   ()           => request<any[]>('/incidents/active'),
  getIncidents:         (page = 0)   => request<any>(`/incidents?page=${page}`),
  createIncident:       (body: any)  => request<any>('/incidents', { method: 'POST', body: JSON.stringify(body) }),
  dispatchIncident:     (id: string) => request<any>(`/incidents/${id}/dispatch`, { method: 'POST' }),
  resolveIncident:      (id: string) => request<any>(`/incidents/${id}/resolve`, { method: 'POST' }),
  getAmbulances:        ()           => request<any[]>('/ambulances'),
  getAvailableAmbulances: ()         => request<any[]>('/ambulances/available'),
  createAmbulance:      (body: any)  => request<any>('/ambulances', { method: 'POST', body: JSON.stringify(body) }),
  updateLocation:       (id: string, lat: number, lng: number) =>
    request<any>(`/ambulances/${id}/location`, { method: 'PATCH', body: JSON.stringify({ lat, lng }) }),
  getHospitals:         ()           => request<any[]>('/hospitals'),
  createHospital:       (body: any)  => request<any>('/hospitals', { method: 'POST', body: JSON.stringify(body) }),
  updateBeds:           (id: string, availableBeds: number, icuAvailable: number) =>
    request<any>(`/hospitals/${id}/beds`, { method: 'PATCH', body: JSON.stringify({ availableBeds, icuAvailable }) }),
  getNearestHospitals:  (lat: number, lng: number, spec?: string) =>
    request<any[]>(`/hospitals/nearest?lat=${lat}&lng=${lng}${spec ? `&specialization=${spec}` : ''}`),
}
