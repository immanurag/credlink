import React from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  LayoutDashboard,
  Users,
  Receipt,
  Wallet,
  Mic,
  BarChart3,
  Settings,
  LogOut,
  X,
  ShieldCheck,
  Store
} from 'lucide-react'

interface SidebarProps {
  isOpen?: boolean
  onClose?: () => void
  onOpenAddCustomer?: () => void
  onOpenTransaction?: () => void
  onOpenPayment?: () => void
}

const navItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/customers', label: 'Customers (Khatabook)', icon: Users },
  { to: '/voice', label: 'Voice AI Entry', icon: Mic, badge: 'AI' },
  { to: '/reports', label: 'Reports & Statements', icon: BarChart3 },
  { to: '/profile', label: 'Store Settings', icon: Settings }
]

export default function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { merchant, logout } = useAuth()

  const sidebarContent = (
    <div className="flex flex-col h-full bg-white border-r border-slate-200/80">
      {/* Brand Header */}
      <div className="p-5 border-b border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-display font-extrabold text-xl shadow-md shadow-indigo-600/20">
            CL
          </div>
          <div>
            <div className="font-display font-bold text-slate-900 text-lg leading-tight flex items-center gap-1.5">
              CredLink
              <ShieldCheck className="w-4 h-4 text-indigo-600" />
            </div>
            <div className="text-[11px] font-medium text-slate-400 uppercase tracking-wider">
              Digital Bahi-Khata
            </div>
          </div>
        </div>

        {/* Mobile close button */}
        {onClose && (
          <button
            onClick={onClose}
            className="md:hidden p-1.5 text-slate-400 hover:text-slate-600 rounded-lg"
            aria-label="Close sidebar"
          >
            <X className="w-5 h-5" />
          </button>
        )}
      </div>

      {/* Merchant Store Info Card */}
      <div className="p-4 mx-3 my-3 bg-slate-50 border border-slate-200/80 rounded-xl flex items-center gap-3">
        <div className="w-9 h-9 rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-600 flex items-center justify-center shrink-0">
          <Store className="w-5 h-5" />
        </div>
        <div className="min-w-0 flex-1">
          <div className="text-xs font-semibold text-slate-900 truncate">
            {merchant?.storeName || 'My Store'}
          </div>
          <div className="text-[11px] text-slate-500 truncate">{merchant?.email || 'Merchant Logged In'}</div>
        </div>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 px-3 py-2 space-y-1 overflow-y-auto">
        <div className="px-3 py-1.5 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
          Main Menu
        </div>
        {navItems.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-indigo-50 text-indigo-700 font-semibold shadow-sm'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                }`
              }
            >
              <Icon className="w-5 h-5 shrink-0" />
              <span className="flex-1 truncate">{item.label}</span>
              {item.badge && (
                <span className="px-2 py-0.5 text-[10px] font-bold bg-indigo-600 text-white rounded-full uppercase tracking-wider animate-pulse">
                  {item.badge}
                </span>
              )}
            </NavLink>
          )
        })}
      </nav>

      {/* Bottom Footer */}
      <div className="p-4 border-t border-slate-100 space-y-3">
        <div className="p-3 bg-indigo-50/60 rounded-xl text-xs space-y-1">
          <div className="font-semibold text-indigo-900 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            Live Cloud Ledger Sync
          </div>
          <p className="text-slate-500 text-[11px]">All financial records are encrypted &amp; backed up safely.</p>
        </div>

        <button
          onClick={logout}
          className="w-full flex items-center justify-center gap-2 px-3 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 rounded-lg transition"
        >
          <LogOut className="w-4 h-4" />
          Sign Out of Account
        </button>
      </div>
    </div>
  )

  return (
    <>
      {/* Desktop Sticky Sidebar */}
      <aside className="hidden md:block w-64 shrink-0 h-screen sticky top-0 z-20">
        {sidebarContent}
      </aside>

      {/* Mobile Drawer Backdrop */}
      {isOpen && (
        <div
          className="md:hidden fixed inset-0 z-40 bg-slate-900/40 backdrop-blur-sm animate-fadeIn"
          onClick={onClose}
        />
      )}

      {/* Mobile Sidebar Drawer */}
      <aside
        className={`md:hidden fixed top-0 left-0 bottom-0 z-50 w-72 bg-white transition-transform duration-300 shadow-2xl ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {sidebarContent}
      </aside>
    </>
  )
}
