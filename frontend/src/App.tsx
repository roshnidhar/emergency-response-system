import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import { AlertTriangle, Truck, BedDouble, LayoutDashboard } from 'lucide-react'
import Dashboard from './pages/Dashboard'
import AmbulancesPage from './pages/Ambulances'
import HospitalsPage from './pages/Hospitals'

const nav = [
  { to: '/',           label: 'Dashboard',  icon: LayoutDashboard },
  { to: '/ambulances', label: 'Ambulances', icon: Truck },
  { to: '/hospitals',  label: 'Hospitals',  icon: BedDouble },
]

export default function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen flex flex-col">
        {/* Top nav */}
        <header className="sticky top-0 z-50 bg-slate-950 border-b border-slate-800">
          <div className="max-w-screen-2xl mx-auto px-6 py-3 flex items-center gap-6">
            {/* Logo */}
            <div className="flex items-center gap-2.5 shrink-0">
              <div className="w-8 h-8 bg-red-600 rounded-lg flex items-center justify-center">
                <AlertTriangle className="w-4 h-4 text-white" />
              </div>
              <div className="hidden sm:block">
                <p className="text-xs font-bold font-mono text-white leading-none">EMERGENCY RESPONSE</p>
                <p className="text-xs text-slate-500 leading-none mt-0.5">Dispatch System</p>
              </div>
            </div>

            {/* Nav links */}
            <nav className="flex gap-1">
              {nav.map(({ to, label, icon: Icon }) => (
                <NavLink key={to} to={to} end={to === '/'}
                  className={({ isActive }) =>
                    `flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors font-medium
                    ${isActive ? 'bg-slate-800 text-white' : 'text-slate-400 hover:text-white hover:bg-slate-900'}`
                  }>
                  <Icon className="w-4 h-4" />
                  <span className="hidden sm:inline">{label}</span>
                </NavLink>
              ))}
            </nav>

            <div className="ml-auto text-xs font-mono text-slate-500">
              Spring Boot + React + PostgreSQL + Redis
            </div>
          </div>
        </header>

        {/* Page */}
        <main className="flex-1">
          <Routes>
            <Route path="/"           element={<Dashboard />} />
            <Route path="/ambulances" element={<AmbulancesPage />} />
            <Route path="/hospitals"  element={<HospitalsPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}
