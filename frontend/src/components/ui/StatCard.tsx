interface Props {
  label: string
  value: string | number
  sub?: string
  accent?: string
  icon?: React.ReactNode
}

export default function StatCard({ label, value, sub, accent = 'border-slate-700', icon }: Props) {
  return (
    <div className={`bg-slate-900 border ${accent} rounded-xl p-4 flex flex-col gap-1`}>
      <div className="flex items-center justify-between">
        <span className="text-xs text-slate-400 uppercase tracking-widest font-mono">{label}</span>
        {icon && <span className="text-slate-500">{icon}</span>}
      </div>
      <span className="text-3xl font-bold font-mono text-white">{value}</span>
      {sub && <span className="text-xs text-slate-500">{sub}</span>}
    </div>
  )
}
