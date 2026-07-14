import { useState, useEffect } from 'react'
import { Plus, RefreshCw } from 'lucide-react'
import { api } from '../lib/api'
import { ambulanceStatusColor } from '../lib/utils'
import type { Ambulance } from '../types'

const VEHICLE_TYPES = ['BLS','ALS','CRITICAL','FIRE','HAZMAT']

export default function AmbulancesPage() {
  const [ambulances, setAmbulances] = useState<Ambulance[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ unitNumber: '', vehicleType: 'ALS' })
  const [locForm, setLocForm] = useState<Record<string, {lat:string;lng:string}>>({})

  const load = async () => {
    setLoading(true)
    setAmbulances(await api.getAmbulances())
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  const create = async () => {
    if (!form.unitNumber) return
    await api.createAmbulance(form)
    setForm({ unitNumber: '', vehicleType: 'ALS' })
    setShowForm(false)
    load()
  }

  const updateLoc = async (id: string) => {
    const loc = locForm[id]
    if (!loc?.lat || !loc?.lng) return
    await api.updateLocation(id, parseFloat(loc.lat), parseFloat(loc.lng))
    load()
  }

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold font-mono text-white">Ambulance Fleet</h1>
        <div className="flex gap-2">
          <button onClick={load} className="p-2 text-slate-400 hover:text-white transition-colors">
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button onClick={() => setShowForm(s => !s)}
            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-colors">
            <Plus className="w-4 h-4" /> Add Unit
          </button>
        </div>
      </div>

      {showForm && (
        <div className="bg-slate-900 border border-slate-700 rounded-xl p-4 flex gap-3 flex-wrap">
          <input value={form.unitNumber} onChange={e => setForm(f => ({...f, unitNumber: e.target.value}))}
            placeholder="Unit number e.g. AMB-001"
            className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white flex-1 min-w-40 focus:outline-none focus:border-blue-500" />
          <select value={form.vehicleType} onChange={e => setForm(f => ({...f, vehicleType: e.target.value}))}
            className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500">
            {VEHICLE_TYPES.map(t => <option key={t}>{t}</option>)}
          </select>
          <button onClick={create} className="bg-green-600 hover:bg-green-500 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-colors">
            Save
          </button>
        </div>
      )}

      <div className="space-y-3">
        {ambulances.map(amb => (
          <div key={amb.id} className="bg-slate-900 border border-slate-800 rounded-xl p-4">
            <div className="flex items-center justify-between flex-wrap gap-3">
              <div className="flex items-center gap-3">
                <span className="text-2xl">🚑</span>
                <div>
                  <p className="font-bold font-mono text-white">{amb.unitNumber}</p>
                  <p className="text-xs text-slate-500">{amb.vehicleType}</p>
                </div>
              </div>
              <span className={`text-sm font-semibold font-mono ${ambulanceStatusColor[amb.status]}`}>
                {amb.status.replace(/_/g,' ')}
              </span>
            </div>

            {/* Location update row */}
            <div className="mt-3 flex gap-2 flex-wrap">
              <input
                placeholder="Lat e.g. 28.6139"
                value={locForm[amb.id]?.lat ?? ''}
                onChange={e => setLocForm(f => ({...f, [amb.id]: {...f[amb.id], lat: e.target.value}}))}
                className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white w-36 focus:outline-none focus:border-blue-500"
              />
              <input
                placeholder="Lng e.g. 77.2090"
                value={locForm[amb.id]?.lng ?? ''}
                onChange={e => setLocForm(f => ({...f, [amb.id]: {...f[amb.id], lng: e.target.value}}))}
                className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white w-36 focus:outline-none focus:border-blue-500"
              />
              <button onClick={() => updateLoc(amb.id)}
                className="bg-slate-700 hover:bg-slate-600 text-white px-3 py-1.5 rounded-lg text-xs transition-colors">
                Update GPS
              </button>
              {amb.currentLat && (
                <span className="text-xs text-slate-500 font-mono self-center">
                  📍 {amb.currentLat.toFixed(4)}, {amb.currentLng?.toFixed(4)}
                </span>
              )}
            </div>
          </div>
        ))}
        {!loading && ambulances.length === 0 && (
          <p className="text-center text-slate-500 py-12">No ambulances yet. Add one above.</p>
        )}
      </div>
    </div>
  )
}
