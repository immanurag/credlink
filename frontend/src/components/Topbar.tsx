import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search, Mic, Plus, Menu, UserPlus, ArrowDownLeft, Store } from 'lucide-react'
import { useAuth } from '../context/AuthContext'

interface TopbarProps {
  onToggleSidebar?: () => void
  onOpenAddCustomer?: () => void
  onOpenPayment?: () => void
  onOpenTransaction?: () => void
}

export default function Topbar({
  onToggleSidebar,
  onOpenAddCustomer,
  onOpenPayment,
  onOpenTransaction
}: TopbarProps) {
  const [search, setSearch] = useState('')
  const navigate = useNavigate()
  const { merchant } = useAuth()

  const onSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (search.trim()) {
      navigate(`/customers?search=${encodeURIComponent(search.trim())}`)
    }
  }

  return (
    <header className="h-16 border-b border-slate-200/80 bg-white flex items-center justify-between gap-3 px-4 md:px-6 sticky top-0 z-30 shadow-sm">
      {/* Left: Mobile Menu Toggle & Brand title for mobile */}
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleSidebar}
          className="md:hidden p-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition"
          aria-label="Toggle navigation menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <form onSubmit={onSearch} className="relative flex-1 max-w-md w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search party by name or phone..."
            className="w-full bg-slate-50 border border-slate-200 rounded-xl pl-10 pr-4 py-2 text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:border-indigo-500 focus:bg-white focus:ring-2 focus:ring-indigo-500/20 transition-all"
          />
        </form>
      </div>

      {/* Right: Quick Action Buttons & Voice Launcher */}
      <div className="flex items-center gap-2 sm:gap-3">
        {/* Voice AI Trigger Button */}
        <button
          onClick={() => navigate('/voice')}
          className="inline-flex items-center gap-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 border border-indigo-200 rounded-xl px-3 py-2 text-xs font-semibold transition-all shadow-sm"
          title="Voice Ledger - Bol kar entry karein"
        >
          <div className="w-2 h-2 rounded-full bg-indigo-600 animate-pulse" />
          <Mic className="w-4 h-4 text-indigo-600" />
          <span className="hidden sm:inline">Voice Entry</span>
        </button>

        {/* Quick Add Customer */}
        {onOpenAddCustomer && (
          <button
            onClick={onOpenAddCustomer}
            className="hidden md:inline-flex items-center gap-1.5 btn-secondary text-xs py-2 px-3"
          >
            <UserPlus className="w-4 h-4 text-slate-500" />
            <span>+ Customer</span>
          </button>
        )}

        {/* Quick Record Payment */}
        {onOpenPayment && (
          <button
            onClick={onOpenPayment}
            className="hidden lg:inline-flex items-center gap-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 rounded-xl px-3 py-2 text-xs font-semibold transition"
          >
            <ArrowDownLeft className="w-4 h-4 text-emerald-600" />
            <span>Record Payment</span>
          </button>
        )}

        {/* Quick New Transaction */}
        {onOpenTransaction && (
          <button
            onClick={onOpenTransaction}
            className="btn-primary text-xs py-2 px-3 font-semibold"
          >
            <Plus className="w-4 h-4" />
            <span className="hidden sm:inline">New Entry</span>
          </button>
        )}

        {/* Merchant Avatar Badge */}
        <div className="pl-2 border-l border-slate-200 flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs uppercase border border-indigo-200">
            {merchant?.storeName ? merchant.storeName.charAt(0) : 'M'}
          </div>
        </div>
      </div>
    </header>
  )
}
