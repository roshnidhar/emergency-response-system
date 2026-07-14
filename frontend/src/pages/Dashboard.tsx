import { useState, useEffect, useCallback } from 'react'
import { Activity, BedDouble, Truck, Wifi, WifiOff, RefreshCw, Plus } from 'lucide-react'
import LiveMap from '../components/map/LiveMap'
import StatCard from '../components/ui/StatCard'
import IncidentRow from '../components/ui/IncidentRow'
import ReportModal from '../components/ui/ReportModal'
import { useEmergencyWS } from '../lib/useWebSocket'
import { api } from '../lib/api'
import { severityBg } from '../lib/utils'
import type { Incident, Ambulance, Hospital, DashboardStats, WSMessage } from '../types'

export default function Dashboard() {
  const [stats, setStats]         = useState<DashboardStats | null>(null)
  const [incidents, setIncidents] = useState<Incident[]>([])
  const [ambulances, setAmbs]     = useState<Ambulance[]>([])
  const [hospitals, setHosps]     = useState<Hospital[]>([])
  const [ambLocs, setAmbLocs]     = useState<Record<string, {lat:number;lng:number}>>({})
  const [feed, setFeed]           = useState<string[]>([])
  const [loading, setLoading]     = useState(true)
  const [showReport, setShowReport] = useState(false)

  const addFeed = (msg: string) =>
    setFeed(prev => [`[${new Date().toLocaleTimeString()}] ${msg}`, ...prev].slice(0, 40))

  const fetchAll = useCallback(async () => {
    try {
      const [s, inc, amb, hosp] = await Promise.all([
        api.getStats(), api.getActiveIncidents(), api.getAmbulances(), api.getHospitals()
      ])
      setStats(s); setIncidents(inc); setAmbs(amb); setHosps(hosp)
    } catch (e) { console.error(e) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { fetchAll() }, [fetchAll])

  const handleWS = useCallback((msg: WSMessage) => {
    switch (msg.type) {
      case 'AMBULANCE_LOCATION':
        setAmbLocs(p => ({ ...p, [msg.ambulanceId]: { lat: msg.lat, lng: msg.lng } }))
        break
      case 'DISPATCH_CREATED':
        addFeed(`🚑 ${msg.ambulanceUnit} → ${msg.incidentNumber} | ETA ${Math.round(msg.etaMinutes)}min`)
        fetchAll()
        break
      case 'INCIDENT_UPDATE':
        addFeed(`📋 ${msg.incidentNumber}: ${msg.event}`)
        fetchAll()
        break
      case 'HOSPITAL_BEDS':
        addFeed(`🏥 ${msg.hospitalName}: ${msg.availableBeds} beds`)
        setHosps(p => p.map(h => h.id === msg.hospitalId ? {...h, availableBeds: msg.availableBeds, icuAvailable: msg.icuAvailable} : h))
        break
    }
  }, [fetchAll])

  const { connected } = useEmergencyWS(handleWS)

  if (loading) return (
    <div className="flex items-center justify-center h-full text-slate-400 font-mono gap-3">
      <RefreshCw className="animate-spin w-5 h-5" /> Loading system…
    </div>
  )

  return (
    <div className="p-6 space-y-6 max-w-screen-2xl mx-auto">
      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Active Incidents" value={stats?.activeIncidents ?? 0} accent="border-red-900" icon={<Activity className="w-4 h-4"/>} />
        <StatCard label="Available Units" value={stats?.availableAmbulances ?? 0} accent="border-green-900" icon={<Truck className="w-4 h-4"/>} />
        <StatCard label="Hospital Beds" value={stats?.totalAvailableBeds ?? 0} sub="across all hospitals" accent="border-cyan-900" icon={<BedDouble className="w-4 h-4"/>} />
        <StatCard label="Connected Clients" value={stats?.connectedClients ?? 0} accent="border-slate-700" icon={<Wifi className="w-4 h-4"/>} />
      </div>

      {/* Map + sidebar */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 h-[500px] bg-slate-900 rounded-xl border border-slate-800 overflow-hidden">
          <LiveMap incidents={incidents} ambulances={ambulances} hospitals={hospitals} ambulanceLocs={ambLocs} />
        </div>
        <div className="flex flex-col gap-4">
          {/* Priority queue */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex-1">
            <h2 className="text-xs font-mono uppercase tracking-widest text-slate-400 mb-3">Priority Queue</h2>
            <div className="space-y-2 overflow-y-auto max-h-56">
              {(stats?.priorityQueue ?? []).length === 0
                ? <p className="text-xs text-slate-600 text-center py-4">No active incidents</p>
                : (stats?.priorityQueue ?? []).map(inc => (
                  <div key={inc.id} className="flex items-center gap-2 text-sm">
                    <span className={`text-xs px-2 py-0.5 rounded font-mono font-bold shrink-0 ${severityBg[inc.severity]}`}>
                      {inc.severity.split('_')[0]}
                    </span>
                    <span className="font-mono text-white truncate text-xs">{inc.incidentNumber}</span>
                    <span className="text-slate-500 text-xs shrink-0">{inc.status}</span>
                  </div>
                ))
              }
            </div>
          </div>
          {/* Live feed */}
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
            <h2 className="text-xs font-mono uppercase tracking-widest text-slate-400 mb-3">Live Feed</h2>
            <div className="space-y-1 max-h-44 overflow-y-auto">
              {feed.length === 0
                ? <p className="text-xs text-slate-600">Waiting for events…</p>
                : feed.map((e, i) => <div key={i} className="text-xs font-mono text-slate-400 leading-relaxed">{e}</div>)
              }
            </div>
          </div>
        </div>
      </div>

      {/* Incidents list */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xs font-mono uppercase tracking-widest text-slate-400">Active Incidents</h2>
          <div className="flex gap-2">
            <button onClick={fetchAll} className="p-1.5 text-slate-400 hover:text-white transition-colors">
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
            <button onClick={() => setShowReport(true)}
              className="flex items-center gap-1.5 bg-red-600 hover:bg-red-500 text-white px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors">
              <Plus className="w-3.5 h-3.5" /> Report
            </button>
          </div>
        </div>
        <div className="space-y-2">
          {incidents.length === 0
            ? <p className="text-xs text-slate-600 text-center py-8">No active incidents</p>
            : incidents.map(inc => (
              <IncidentRow key={inc.id} incident={inc}
                onDispatch={async id => { await api.dispatchIncident(id); fetchAll() }}
                onResolve={async id => { await api.resolveIncident(id); fetchAll() }} />
            ))
          }
        </div>
      </div>

      {/* Hospital beds */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
        <h2 className="text-xs font-mono uppercase tracking-widest text-slate-400 mb-4">Hospital Availability</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {hospitals.map(h => {
            const pct = h.totalBeds > 0 ? (h.availableBeds / h.totalBeds) * 100 : 0
            const bar = pct > 50 ? 'bg-green-500' : pct > 20 ? 'bg-yellow-500' : 'bg-red-500'
            return (
              <div key={h.id} className="border border-slate-800 rounded-lg p-3 bg-slate-950">
                <div className="flex justify-between items-start gap-2">
                  <div>
                    <p className="text-sm font-semibold text-white">🏥 {h.name}</p>
                    <p className="text-xs text-slate-500 mt-0.5">{h.address}</p>
                  </div>
                  {h.traumaCenter && <span className="text-xs bg-red-900/40 text-red-400 px-1.5 py-0.5 rounded shrink-0">Trauma</span>}
                </div>
                <div className="mt-2 space-y-1">
                  <div className="flex justify-between text-xs"><span className="text-slate-400">General</span><span className="font-mono">{h.availableBeds}/{h.totalBeds}</span></div>
                  <div className="h-1.5 bg-slate-800 rounded-full"><div className={`h-full ${bar} rounded-full transition-all`} style={{width:`${pct}%`}}/></div>
                  <div className="flex justify-between text-xs mt-1"><span className="text-slate-400">ICU</span><span className="font-mono">{h.icuAvailable}/{h.icuTotal}</span></div>
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {showReport && <ReportModal onClose={() => setShowReport(false)} onCreated={fetchAll} />}
    </div>
  )
}
