import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { customerService } from '../services/customer'
import { formatCurrency } from '../components/formatters'
import { TableSkeleton } from '../components/SkeletonLoader'
import type { Customer } from '../types'
import { BarChart3, FileSpreadsheet, FileDown, Search, ChevronRight, Users } from 'lucide-react'

export default function Reports() {
  const [customers, setCustomers] = useState<Customer[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    customerService
      .list()
      .then(setCustomers)
      .finally(() => setLoading(false))
  }, [])

  const filtered = customers.filter((c) =>
    c.name.toLowerCase().includes(search.toLowerCase()) || (c.phone && c.phone.includes(search))
  )

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display font-extrabold text-2xl md:text-3xl text-slate-900">
          Reports &amp; Account Statements
        </h1>
        <p className="text-xs md:text-sm text-slate-500">
          Generate, preview &amp; export PDF or Excel statements for individual party ledger accounts.
        </p>
      </div>

      <div className="card space-y-4 p-5">
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search party by name or phone to view statement..."
            className="input-field pl-10"
          />
        </div>

        {loading ? (
          <TableSkeleton rows={5} />
        ) : filtered.length === 0 ? (
          <div className="py-8 text-center text-slate-400 text-sm">No customers found.</div>
        ) : (
          <div className="divide-y divide-slate-100">
            {filtered.map((c) => (
              <div
                key={c.id}
                onClick={() => navigate(`/reports/${c.id}`)}
                className="flex items-center justify-between p-3.5 hover:bg-slate-50 rounded-xl cursor-pointer transition group"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-full bg-indigo-50 text-indigo-700 font-bold text-xs flex items-center justify-center border border-indigo-100">
                    {c.name.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <div className="font-semibold text-sm text-slate-900 group-hover:text-indigo-600 transition">
                      {c.name}
                    </div>
                    <div className="text-xs text-slate-400">{c.phone || 'No phone number'}</div>
                  </div>
                </div>

                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <div className={`font-display font-bold text-sm ${c.currentBalance > 0 ? 'text-amber-600' : 'text-emerald-600'}`}>
                      {formatCurrency(Math.abs(c.currentBalance))}
                    </div>
                    <div className="text-[11px] text-slate-400 font-medium">
                      {c.currentBalance > 0 ? 'Due' : 'Cleared'}
                    </div>
                  </div>
                  <ChevronRight className="w-5 h-5 text-slate-300 group-hover:text-indigo-600 transition" />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
