import React from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Mic } from 'lucide-react'

export default function VoiceFAB() {
  const navigate = useNavigate()
  const location = useLocation()

  if (location.pathname === '/voice') return null

  return (
    <div className="fixed bottom-6 right-6 z-40 group">
      <button
        onClick={() => navigate('/voice')}
        className="relative h-14 w-14 sm:h-16 sm:w-16 rounded-2xl bg-indigo-600 text-white shadow-xl shadow-indigo-600/30 flex items-center justify-center hover:scale-105 active:scale-95 transition-all duration-200 group-hover:bg-indigo-700"
        aria-label="Start voice transaction entry"
      >
        <div className="absolute inset-0 rounded-2xl bg-indigo-400 opacity-30 animate-pulseWave pointer-events-none" />
        <Mic className="w-7 h-7 relative z-10" />
      </button>

      {/* Tooltip on hover */}
      <div className="absolute bottom-full right-0 mb-2 hidden group-hover:block whitespace-nowrap bg-slate-900 text-white text-xs font-semibold px-3 py-1.5 rounded-lg shadow-lg">
        Bol Kar Khata Likhein (Voice AI)
      </div>
    </div>
  )
}
