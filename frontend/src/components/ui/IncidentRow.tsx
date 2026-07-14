import { severityBg, severityLabel, statusBadge, fmtTime } from '../../lib/utils'
import type { Incident } from '../../types'

interface Props {
  incident: Incident
  onDispatch?: (id: string) => void
  onResolve?: (id: string) => void
}

export default function IncidentRow({ incident, onDispatch, onResolve }: Props) {
  const active = !['RESOLVED', 'CANCELLED'].includes(incident.status)
  return (
    <div className="flex items-center gap-3 py-3 px-4 bg-slate-900 border border-slate-800 rounded-lg hover:border-slate-600 transition-colors">
      <span className={`text-xs font-bold px-2 py-0.5 rounded font-mono shrink-0 ${severityBg[incident.severity]}`}>
        {severityLabel[incident.severity]}
      </span>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-sm font-semibold text-white font-mono">{incident.incidentNumber}</span>
          <span className={`text-xs px-1.5 py-0.5 rounded ${statusBadge[incident.status]}`}>
            {incident.status.replace(/_/g, ' ')}
          </span>
        </div>
        <div className="text-xs text-slate-400 truncate mt-0.5">
          {incident.type.replace(/_/g, ' ')} · {incident.address ?? `${incident.lat.toFixed(4)}, ${incident.lng.toFixed(4)}`}
        </div>
      </div>
      <span className="text-xs text-slate-500 font-mono shrink-0">{fmtTime(incident.reportedAt)}</span>
      {active && (
        <div className="flex gap-2 shrink-0">
          {incident.status === 'PENDING' && onDispatch && (
            <button onClick={() => onDispatch(incident.id)}
              className="text-xs bg-blue-600 hover:bg-blue-500 text-white px-3 py-1 rounded font-semibold transition-colors">
              Dispatch
            </button>
          )}
          {onResolve && (
            <button onClick={() => onResolve(incident.id)}
              className="text-xs bg-slate-700 hover:bg-slate-600 text-white px-3 py-1 rounded transition-colors">
              Resolve
            </button>
          )}
        </div>
      )}
    </div>
  )
}
