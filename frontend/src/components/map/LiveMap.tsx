import { useEffect, useRef } from 'react'
import { MapContainer, TileLayer, useMap } from 'react-leaflet'
import L from 'leaflet'
import type { Incident, Ambulance, Hospital } from '../../types'
import { severityDot } from '../../lib/utils'

// ── Marker updater (runs inside MapContainer context) ──────────────────────
interface UpdaterProps {
  incidents: Incident[]
  ambulances: Ambulance[]
  hospitals: Hospital[]
  ambulanceLocs: Record<string, { lat: number; lng: number }>
}

function MarkerUpdater({ incidents, ambulances, hospitals, ambulanceLocs }: UpdaterProps) {
  const map = useMap()
  const markers = useRef<Record<string, L.Marker>>({})

  // Incidents
  useEffect(() => {
    incidents.forEach((inc) => {
      const key = `inc-${inc.id}`
      const color = severityDot[inc.severity]
      const icon = L.divIcon({
        html: `<div style="width:14px;height:14px;border-radius:50%;background:${color};
               border:2px solid #fff;box-shadow:0 0 8px ${color}55"></div>`,
        className: '',
        iconSize: [14, 14],
        iconAnchor: [7, 7],
      })
      const popup = `<div style="font-family:sans-serif;min-width:160px">
        <b style="color:${color}">${inc.severity}</b><br/>
        <span style="font-size:13px">${inc.incidentNumber}</span><br/>
        <span style="font-size:12px;color:#555">${inc.type.replace(/_/g,' ')}</span><br/>
        <span style="font-size:11px">${inc.address ?? `${inc.lat.toFixed(4)}, ${inc.lng.toFixed(4)}`}</span><br/>
        <span style="font-size:11px;color:#888">Status: ${inc.status}</span>
      </div>`

      if (markers.current[key]) {
        markers.current[key].setLatLng([inc.lat, inc.lng]).setIcon(icon)
      } else {
        markers.current[key] = L.marker([inc.lat, inc.lng], { icon })
          .addTo(map).bindPopup(popup)
      }
    })
  }, [incidents, map])

  // Ambulances
  useEffect(() => {
    ambulances.forEach((amb) => {
      const loc = ambulanceLocs[amb.id] ?? (amb.currentLat ? { lat: amb.currentLat, lng: amb.currentLng } : null)
      if (!loc?.lat) return
      const key = `amb-${amb.id}`
      const color = amb.status === 'AVAILABLE' ? '#22c55e' : '#3b82f6'
      const icon = L.divIcon({
        html: `<div style="background:${color};color:#fff;border-radius:6px;padding:2px 6px;
               font-size:10px;font-weight:700;white-space:nowrap;
               box-shadow:0 2px 6px rgba(0,0,0,.4)">🚑 ${amb.unitNumber}</div>`,
        className: '',
        iconAnchor: [32, 12],
      })
      if (markers.current[key]) {
        markers.current[key].setLatLng([loc.lat, loc.lng!]).setIcon(icon)
      } else {
        markers.current[key] = L.marker([loc.lat, loc.lng!], { icon })
          .addTo(map)
          .bindPopup(`<b>${amb.unitNumber}</b><br>${amb.status}<br>${amb.vehicleType}`)
      }
    })
  }, [ambulances, ambulanceLocs, map])

  // Hospitals
  useEffect(() => {
    hospitals.forEach((h) => {
      const key = `hosp-${h.id}`
      if (markers.current[key]) return
      const icon = L.divIcon({
        html: `<div style="background:#0891b2;color:#fff;border-radius:50%;width:22px;height:22px;
               display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;
               box-shadow:0 2px 6px rgba(0,0,0,.4)">H</div>`,
        className: '',
        iconSize: [22, 22],
        iconAnchor: [11, 11],
      })
      markers.current[key] = L.marker([h.lat, h.lng], { icon })
        .addTo(map)
        .bindPopup(`<b>${h.name}</b><br>Beds: ${h.availableBeds}/${h.totalBeds}<br>ICU: ${h.icuAvailable}/${h.icuTotal}${h.traumaCenter ? '<br>✅ Trauma' : ''}`)
    })
  }, [hospitals, map])

  return null
}

// ── Main exported component ────────────────────────────────────────────────
interface Props {
  incidents: Incident[]
  ambulances: Ambulance[]
  hospitals: Hospital[]
  ambulanceLocs: Record<string, { lat: number; lng: number }>
}

export default function LiveMap({ incidents, ambulances, hospitals, ambulanceLocs }: Props) {
  return (
    <div className="relative w-full h-full rounded-xl overflow-hidden">
      <MapContainer
        center={[20.5937, 78.9629]}
        zoom={5}
        style={{ width: '100%', height: '100%' }}
        zoomControl
      >
        <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        <MarkerUpdater
          incidents={incidents}
          ambulances={ambulances}
          hospitals={hospitals}
          ambulanceLocs={ambulanceLocs}
        />
      </MapContainer>

      {/* Legend */}
      <div className="absolute bottom-3 left-3 bg-slate-900/90 backdrop-blur rounded-lg px-3 py-2 text-xs flex gap-3 z-[1000]">
        <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-full bg-red-600 inline-block"/>P1</span>
        <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-full bg-orange-500 inline-block"/>P2</span>
        <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-full bg-green-500 inline-block"/>Unit</span>
        <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-full bg-cyan-600 inline-block"/>Hospital</span>
      </div>
    </div>
  )
}
