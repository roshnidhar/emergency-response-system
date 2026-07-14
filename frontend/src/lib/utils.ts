import type { Severity, IncidentStatus, AmbulanceStatus } from '../types'

export const severityLabel: Record<Severity, string> = {
  P1_CRITICAL: 'P1 Critical',
  P2_SERIOUS:  'P2 Serious',
  P3_MINOR:    'P3 Minor',
  P4_DECEASED: 'P4 Deceased',
}

export const severityBg: Record<Severity, string> = {
  P1_CRITICAL: 'bg-red-600 text-white',
  P2_SERIOUS:  'bg-orange-500 text-white',
  P3_MINOR:    'bg-yellow-400 text-black',
  P4_DECEASED: 'bg-slate-600 text-white',
}

export const severityDot: Record<Severity, string> = {
  P1_CRITICAL: '#dc2626',
  P2_SERIOUS:  '#f97316',
  P3_MINOR:    '#eab308',
  P4_DECEASED: '#64748b',
}

export const statusBadge: Record<IncidentStatus, string> = {
  PENDING:      'bg-yellow-500/20 text-yellow-300',
  DISPATCHING:  'bg-blue-500/20 text-blue-300',
  DISPATCHED:   'bg-blue-600/20 text-blue-200',
  ON_SCENE:     'bg-purple-500/20 text-purple-300',
  TRANSPORTING: 'bg-cyan-500/20 text-cyan-300',
  RESOLVED:     'bg-green-500/20 text-green-400',
  CANCELLED:    'bg-slate-500/20 text-slate-400',
}

export const ambulanceStatusColor: Record<AmbulanceStatus, string> = {
  AVAILABLE:    'text-green-400',
  DISPATCHED:   'text-blue-400',
  EN_ROUTE:     'text-blue-300',
  ON_SCENE:     'text-purple-400',
  TRANSPORTING: 'text-cyan-400',
  AT_HOSPITAL:  'text-teal-400',
  MAINTENANCE:  'text-yellow-400',
  OFFLINE:      'text-slate-500',
}

export const fmtTime = (iso: string) =>
  new Date(iso).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })

export const fmtEta = (s: number) =>
  s < 60 ? `${Math.round(s)}s` : `${Math.round(s / 60)}min`
