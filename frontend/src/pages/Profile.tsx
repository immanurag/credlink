import React from 'react'
import { useAuth } from '../context/AuthContext'
import { Store, Mail, LogOut, ShieldCheck } from 'lucide-react'

export default function Profile() {
  const { merchant, logout } = useAuth()

  return (
    <div className="space-y-6 max-w-xl">
      <div>
        <h1 className="font-display font-extrabold text-2xl md:text-3xl text-slate-900">
          Store &amp; Account Settings
        </h1>
        <p className="text-xs md:text-sm text-slate-500">
          Merchant profile, store details &amp; active session management.
        </p>
      </div>

      <div className="card space-y-6 p-6">
        <div className="flex items-center gap-4 border-b border-slate-100 pb-6">
          <div className="w-14 h-14 rounded-2xl bg-indigo-600 text-white font-display font-extrabold text-2xl flex items-center justify-center shadow-md">
            {merchant?.storeName ? merchant.storeName.charAt(0).toUpperCase() : 'M'}
          </div>
          <div>
            <h2 className="font-display font-bold text-xl text-slate-900 flex items-center gap-2">
              {merchant?.storeName || 'My Store'}
              <ShieldCheck className="w-4 h-4 text-indigo-600" />
            </h2>
            <div className="text-xs text-slate-400 font-medium">Verified Merchant Account</div>
          </div>
        </div>

        <div className="space-y-4 text-sm">
          <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl">
            <Store className="w-5 h-5 text-indigo-600 shrink-0" />
            <div>
              <div className="text-xs font-semibold text-slate-500">Store Name</div>
              <div className="font-bold text-slate-900">{merchant?.storeName}</div>
            </div>
          </div>

          <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl">
            <Mail className="w-5 h-5 text-indigo-600 shrink-0" />
            <div>
              <div className="text-xs font-semibold text-slate-500">Login Email</div>
              <div className="font-bold text-slate-900">{merchant?.email}</div>
            </div>
          </div>
        </div>

        <div className="pt-4 border-t border-slate-100">
          <button onClick={logout} className="btn-danger w-full justify-center py-2.5">
            <LogOut className="w-4 h-4" />
            Log Out of CredLink
          </button>
        </div>
      </div>
    </div>
  )
}
