import { useState } from 'react'
import { X } from 'lucide-react'
import { api } from '../../lib/api'

interface Props { onClose: () => void; onCreated: () => void }

const INCIDENT_TYPES = ['CARDIAC_ARREST','TRAUMA','FIRE','ACCIDENT','STROKE','RESPIRATORY','HAZMAT','PSYCHIATRIC','OTHER']
const SEVERITIES = [
  { value: 'P1_CRITICAL', label: 'P1 — Critical (Life threatening)' },
  { value: 'P2_SERIOUS',  label: 'P2 — Serious' },
  { value: 'P3_MINOR',    label: 'P3 — Minor' },
]

export default function ReportModal({ onClose, onCreated }: Props) {
  const [form, setForm] = useState({
    type: 'CARDIAC_ARREST', severity: 'P1_CRITICAL',
    lat: '', lng: '', address: '', callerName: '', callerPhone: '',
    description: '', patientCount: 1,
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const set = (k: string, v: any) => setForm(f => ({ ...f, [k]: v }))

  const submit = async () => {
    if (!form.lat || !form.lng) { setError('Latitude and Longitude are required'); return }
    setLoading(true); setError('')
    try {
      await api.createIncident({ ...form, lat: parseFloat(form.lat), lng: parseFloat(form.lng) })
      onCreated(); onClose()
    } catch (e: any) {
      setError(e.message ?? 'Failed to create incident')
    } finally { setLoading(false) }
  }

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-lg shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-800">
          <h2 className="font-bold text-white font-mono">🚨 Report Incident</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-5 space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-slate-400 uppercase tracking-wider font-mono block mb-1">Type</label>
              <select value={form.type} onChange={e => set('type', e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500">
                {INCIDENT_TYPES.map(t => <option key={t} value={t}>{t.replace(/_/g,' ')}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs text-slate-400 uppercase tracking-wider font-mono block mb-1">Severity</label>
              <select value={form.severity} onChange={e => set('severity', e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500">
                {SEVERITIES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            {(['lat','lng'] as const).map(k => (
              <div key={k}>
                <label className="text-xs text-slate-400 uppercase tracking-wider font-mono block mb-1">{k === 'lat' ? 'Latitude' : 'Longitude'}</label>
                <input type="number" step="0.0001" value={form[k]} onChange={e => set(k, e.target.value)}
                  placeholder={k === 'lat' ? '28.6139' : '77.2090'}
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500" />
              </div>
            ))}
          </div>

          <div>
            <label className="text-xs text-slate-400 uppercase tracking-wider font-mono block mb-1">Address</label>
            <input value={form.address} onChange={e => set('address', e.target.value)}
              placeholder="Street address or landmark"
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500" />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs text-slate-400 uppercase tracking-wider font-mono block mb-1">Caller Name</label>
              <input value={form.callerName} onChange={e => set('callerName', e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500" />
            </div>
            <div>
              <label className="text-xs text-slate-400 uppercase tracking-wider font-mono block mb-1">Phone</label>
              <input value={form.callerPhone} onChange={e => set('callerPhone', e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500" />
            </div>
          </div>

          <div>
            <label className="text-xs text-slate-400 uppercase tracking-wider font-mono block mb-1">Patients</label>
            <input type="number" min={1} max={100} value={form.patientCount} onChange={e => set('patientCount', parseInt(e.target.value))}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-blue-500" />
          </div>

          {error && <p className="text-red-400 text-sm bg-red-950/40 border border-red-900 rounded-lg px-3 py-2">{error}</p>}
        </div>

        {/* Footer */}
        <div className="flex gap-3 p-5 border-t border-slate-800">
          <button onClick={onClose} className="flex-1 bg-slate-800 hover:bg-slate-700 text-white rounded-lg py-2.5 text-sm transition-colors">
            Cancel
          </button>
          <button onClick={submit} disabled={loading}
            className="flex-1 bg-red-600 hover:bg-red-500 disabled:opacity-50 text-white rounded-lg py-2.5 text-sm font-semibold transition-colors">
            {loading ? 'Reporting…' : '🚨 Report Emergency'}
          </button>
        </div>
      </div>
    </div>
  )
}
