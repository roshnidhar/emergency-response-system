import { useState, useEffect } from 'react'
import { Plus, RefreshCw } from 'lucide-react'
import { api } from '../lib/api'
import type { Hospital } from '../types'

export default function HospitalsPage() {
  const [hospitals, setHospitals] = useState<Hospital[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [bedForms, setBedForms] = useState<Record<string, {avail:string;icu:string}>>({})
  const [form, setForm] = useState({
    name:'', address:'', lat:'', lng:'',
    totalBeds:'', icuTotal:'', traumaCenter: false, specializations:''
  })

  const load = async () => {
    setLoading(true)
    setHospitals(await api.getHospitals())
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  const create = async () => {
    if (!form.name || !form.lat || !form.lng) return
    await api.createHospital({
      ...form,
      lat: parseFloat(form.lat), lng: parseFloat(form.lng),
      totalBeds: parseInt(form.totalBeds) || 0,
      icuTotal: parseInt(form.icuTotal) || 0,
      specializations: form.specializations ? form.specializations.split(',').map(s => s.trim()) : [],
    })
    setShowForm(false)
    load()
  }

  const updateBeds = async (id: string) => {
    const bf = bedForms[id]
    if (!bf?.avail) return
    await api.updateBeds(id, parseInt(bf.avail), parseInt(bf.icu || '0'))
    load()
  }

  return (
    <div className="p-6 max-w-5xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold font-mono text-white">Hospitals</h1>
        <div className="flex gap-2">
          <button onClick={load} className="p-2 text-slate-400 hover:text-white transition-colors">
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
          <button onClick={() => setShowForm(s => !s)}
            className="flex items-center gap-2 bg-cyan-700 hover:bg-cyan-600 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-colors">
            <Plus className="w-4 h-4" /> Add Hospital
          </button>
        </div>
      </div>

      {showForm && (
        <div className="bg-slate-900 border border-slate-700 rounded-xl p-4 space-y-3">
          <div className="grid grid-cols-2 gap-3">
            {[
              ['name','Hospital Name','City Hospital'],
              ['address','Address','MG Road, Mumbai'],
            ].map(([k,l,p]) => (
              <div key={k}>
                <label className="text-xs text-slate-400 block mb-1">{l}</label>
                <input value={(form as any)[k]} onChange={e => setForm(f=>({...f,[k]:e.target.value}))}
                  placeholder={p}
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-cyan-500" />
              </div>
            ))}
          </div>
          <div className="grid grid-cols-4 gap-3">
            {[['lat','Latitude','19.0760'],['lng','Longitude','72.8777'],['totalBeds','Total Beds','100'],['icuTotal','ICU Total','20']].map(([k,l,p]) => (
              <div key={k}>
                <label className="text-xs text-slate-400 block mb-1">{l}</label>
                <input value={(form as any)[k]} onChange={e => setForm(f=>({...f,[k]:e.target.value}))}
                  placeholder={p}
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-cyan-500" />
              </div>
            ))}
          </div>
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-2 text-sm text-slate-300 cursor-pointer">
              <input type="checkbox" checked={form.traumaCenter} onChange={e => setForm(f=>({...f,traumaCenter:e.target.checked}))}
                className="rounded" />
              Trauma Center
            </label>
            <div className="flex-1">
              <input value={form.specializations} onChange={e => setForm(f=>({...f,specializations:e.target.value}))}
                placeholder="Specializations (comma-separated): cardiac, stroke, burn"
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-cyan-500" />
            </div>
          </div>
          <button onClick={create} className="bg-cyan-700 hover:bg-cyan-600 text-white px-5 py-2 rounded-lg text-sm font-semibold transition-colors">
            Save Hospital
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {hospitals.map(h => {
          const pct = h.totalBeds > 0 ? (h.availableBeds / h.totalBeds) * 100 : 0
          const bar = pct > 50 ? 'bg-green-500' : pct > 20 ? 'bg-yellow-500' : 'bg-red-500'
          return (
            <div key={h.id} className="bg-slate-900 border border-slate-800 rounded-xl p-4 space-y-3">
              <div className="flex justify-between items-start">
                <div>
                  <p className="font-semibold text-white">🏥 {h.name}</p>
                  <p className="text-xs text-slate-500 mt-0.5">{h.address}</p>
                  {h.traumaCenter && <span className="text-xs bg-red-900/40 text-red-400 px-1.5 py-0.5 rounded mt-1 inline-block">Trauma Center</span>}
                </div>
              </div>
              <div className="space-y-1">
                <div className="flex justify-between text-xs"><span className="text-slate-400">General Beds</span><span className="font-mono text-white">{h.availableBeds}/{h.totalBeds}</span></div>
                <div className="h-1.5 bg-slate-800 rounded-full"><div className={`h-full ${bar} rounded-full transition-all`} style={{width:`${pct}%`}}/></div>
                <div className="flex justify-between text-xs mt-1"><span className="text-slate-400">ICU</span><span className="font-mono text-white">{h.icuAvailable}/{h.icuTotal}</span></div>
              </div>
              <div className="flex gap-2">
                <input placeholder="Avail beds" value={bedForms[h.id]?.avail ?? ''}
                  onChange={e => setBedForms(f=>({...f,[h.id]:{...f[h.id],avail:e.target.value}}))}
                  className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white w-24 focus:outline-none focus:border-cyan-500" />
                <input placeholder="Avail ICU" value={bedForms[h.id]?.icu ?? ''}
                  onChange={e => setBedForms(f=>({...f,[h.id]:{...f[h.id],icu:e.target.value}}))}
                  className="bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white w-24 focus:outline-none focus:border-cyan-500" />
                <button onClick={() => updateBeds(h.id)}
                  className="bg-slate-700 hover:bg-slate-600 text-white px-3 py-1.5 rounded-lg text-xs transition-colors">
                  Update Beds
                </button>
              </div>
            </div>
          )
        })}
        {!loading && hospitals.length === 0 && (
          <p className="text-slate-500 text-center py-12 col-span-2">No hospitals yet. Add one above.</p>
        )}
      </div>
    </div>
  )
}
