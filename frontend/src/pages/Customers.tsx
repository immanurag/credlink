import React, { useEffect, useState } from 'react'
import { useNavigate, useSearchParams, useOutletContext } from 'react-router-dom'
import { customerService } from '../services/customer'
import { formatCurrency } from '../components/formatters'
import { TableSkeleton } from '../components/SkeletonLoader'
import type { Customer } from '../types'
import {
  Search,
  UserPlus,
  ArrowUpRight,
  ArrowDownLeft,
  Users,
  Wallet,
  CheckCircle2,
  Phone,
  Tag,
  ChevronRight,
  AlertCircle,
  FileSpreadsheet
} from 'lucide-react'

interface LayoutContextType {
  openAddCustomer: () => void
  openTransaction: () => void
  openPayment: () => void
}

export default function Customers() {
  const [params] = useSearchParams()
  const [customers, setCustomers] = useState<Customer[]>([])
  const [search, setSearch] = useState(params.get('search') || '')
  const [filter, setFilter] = useState<'all' | 'due' | 'cleared'>('all')
  const [loading, setLoading] = useState(true)

  const { openAddCustomer, openPayment, openTransaction } = useOutletContext<LayoutContextType>()
  const navigate = useNavigate()

  const load = async (q?: string) => {
    setLoading(true)
    try {
      setCustomers(await customerService.list(q))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(search)

    const handleDataUpdated = () => load(search)
    window.addEventListener('credlink:data-updated', handleDataUpdated)
    return () => window.removeEventListener('credlink:data-updated', handleDataUpdated)
  }, [])

  const onSearchSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    await load(search)
  }

  const visible = customers.filter((c) => {
    if (filter === 'due') return c.currentBalance > 0
    if (filter === 'cleared') return c.currentBalance === 0
    return true
  })

  const totalDue = customers.filter((c) => c.currentBalance > 0).reduce((s, c) => s + c.currentBalance, 0)
  const totalAdvance = Math.abs(customers.filter((c) => c.currentBalance < 0).reduce((s, c) => s + c.currentBalance, 0))
  const clearedCount = customers.filter((c) => c.currentBalance === 0).length

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display font-extrabold text-2xl md:text-3xl text-slate-900">
            Customer Directory (Khatabook)
          </h1>
          <p className="text-xs md:text-sm text-slate-500">
            Manage customer accounts, track credit balances, and record payments.
          </p>
        </div>

        <button onClick={openAddCustomer} className="btn-primary">
          <UserPlus className="w-4 h-4" />
          <span>+ Add Customer</span>
        </button>
      </div>

      {/* Top Stat Bar */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <div className="card p-4">
          <div className="text-xs text-slate-500 font-semibold uppercase">Total Customers</div>
          <div className="text-xl font-display font-bold text-slate-900 mt-1">{customers.length} Parties</div>
        </div>
        <div className="card p-4 bg-amber-50/50 border-amber-200/80">
          <div className="text-xs text-amber-700 font-semibold uppercase">Total Lena Hai (Due)</div>
          <div className="text-xl font-display font-extrabold text-amber-600 mt-1">
            {formatCurrency(totalDue)}
          </div>
        </div>
        <div className="card p-4 bg-emerald-50/50 border-emerald-200/80">
          <div className="text-xs text-emerald-700 font-semibold uppercase">Total Dena Hai (Advance)</div>
          <div className="text-xl font-display font-extrabold text-emerald-600 mt-1">
            {formatCurrency(totalAdvance)}
          </div>
        </div>
        <div className="card p-4">
          <div className="text-xs text-slate-500 font-semibold uppercase">Cleared Accounts</div>
          <div className="text-xl font-display font-bold text-slate-900 mt-1">{clearedCount}</div>
        </div>
      </div>

      {/* Main Table Card */}
      <div className="card p-0 overflow-hidden">
        {/* Search & Filter Controls */}
        <div className="p-4 sm:p-5 border-b border-slate-100 bg-slate-50/50 space-y-3">
          <div className="flex flex-col sm:flex-row gap-3">
            <form onSubmit={onSearchSubmit} className="relative flex-1">
              <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
              <input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search party by name, phone or category..."
                className="input-field pl-10 pr-20"
              />
              <button
                type="submit"
                className="absolute right-1.5 top-1.5 bottom-1.5 px-3 bg-slate-200 hover:bg-slate-300 text-slate-700 font-semibold text-xs rounded-md transition"
              >
                Search
              </button>
            </form>

            {/* Filter Pills */}
            <div className="flex items-center gap-1.5 self-start sm:self-center">
              {(
                [
                  { key: 'all', label: `All (${customers.length})` },
                  { key: 'due', label: 'High Due' },
                  { key: 'cleared', label: 'Cleared' }
                ] as const
              ).map((f) => (
                <button
                  key={f.key}
                  onClick={() => setFilter(f.key)}
                  className={`px-3 py-2 rounded-lg text-xs font-semibold transition ${
                    filter === f.key
                      ? 'bg-indigo-600 text-white shadow-sm'
                      : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-100'
                  }`}
                >
                  {f.label}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Customer List / Table */}
        {loading ? (
          <div className="p-4">
            <TableSkeleton rows={6} />
          </div>
        ) : visible.length === 0 ? (
          /* Friendly Empty State */
          <div className="py-12 px-4 text-center space-y-4">
            <div className="w-16 h-16 rounded-full bg-indigo-50 text-indigo-600 border border-indigo-100 flex items-center justify-center mx-auto">
              <Users className="w-8 h-8" />
            </div>
            <div className="max-w-xs mx-auto">
              <h3 className="font-display font-semibold text-slate-900 text-base">No customers found</h3>
              <p className="text-xs text-slate-500 mt-1">
                {search ? `No results match "${search}". Try searching another name.` : 'Add your first customer to start managing your digital ledger.'}
              </p>
            </div>
            <button onClick={openAddCustomer} className="btn-primary">
              <UserPlus className="w-4 h-4" />
              + Add First Customer
            </button>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {/* Desktop Table View */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50/80 text-xs uppercase font-semibold text-slate-500 border-b border-slate-100">
                  <tr>
                    <th className="py-3 px-4">Party Name</th>
                    <th className="py-3 px-4">Phone</th>
                    <th className="py-3 px-4">Category</th>
                    <th className="py-3 px-4 text-right">Outstanding Amount</th>
                    <th className="py-3 px-4 text-center">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {visible.map((c) => {
                    const isDue = c.currentBalance > 0
                    const isAdvance = c.currentBalance < 0
                    const isCleared = c.currentBalance === 0

                    return (
                      <tr
                        key={c.id}
                        onClick={() => navigate(`/customers/${c.id}`)}
                        className="hover:bg-slate-50/80 cursor-pointer transition-colors group"
                      >
                        <td className="py-3.5 px-4 font-semibold text-slate-900 group-hover:text-indigo-600">
                          <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-full bg-indigo-50 text-indigo-700 font-bold text-xs flex items-center justify-center border border-indigo-100 shrink-0">
                              {c.name.charAt(0).toUpperCase()}
                            </div>
                            <span className="truncate">{c.name}</span>
                          </div>
                        </td>
                        <td className="py-3.5 px-4 text-slate-500">
                          {c.phone ? (
                            <span className="inline-flex items-center gap-1">
                              <Phone className="w-3.5 h-3.5 text-slate-400" />
                              {c.phone}
                            </span>
                          ) : (
                            <span className="text-slate-300 italic">No phone</span>
                          )}
                        </td>
                        <td className="py-3.5 px-4 text-slate-500">
                          {c.category ? (
                            <span className="inline-flex items-center gap-1 text-xs bg-slate-100 text-slate-700 px-2 py-0.5 rounded font-medium">
                              <Tag className="w-3 h-3 text-slate-400" />
                              {c.category}
                            </span>
                          ) : (
                            <span className="text-slate-300">&mdash;</span>
                          )}
                        </td>
                        <td className="py-3.5 px-4 text-right font-display font-extrabold text-base">
                          <span className={isDue ? 'text-amber-600' : isAdvance ? 'text-emerald-600' : 'text-slate-400'}>
                            {formatCurrency(Math.abs(c.currentBalance))}
                          </span>
                        </td>
                        <td className="py-3.5 px-4 text-center">
                          {isDue ? (
                            <span className="pill-udhaar">Pending</span>
                          ) : isAdvance ? (
                            <span className="pill-jama">Advance</span>
                          ) : (
                            <span className="inline-flex items-center gap-1 bg-slate-100 text-slate-600 px-2.5 py-0.5 rounded-full text-xs font-semibold">
                              <CheckCircle2 className="w-3 h-3 text-slate-400" />
                              Cleared
                            </span>
                          )}
                        </td>
                        <td className="py-3.5 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => navigate(`/customers/${c.id}`)}
                              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 px-2.5 py-1 rounded hover:bg-indigo-50 transition"
                            >
                              View Khata
                            </button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            {/* Mobile Card List View */}
            <div className="md:hidden divide-y divide-slate-100">
              {visible.map((c) => (
                <div
                  key={c.id}
                  onClick={() => navigate(`/customers/${c.id}`)}
                  className="p-4 hover:bg-slate-50 active:bg-slate-100 transition cursor-pointer space-y-2"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-indigo-50 text-indigo-700 font-bold text-sm flex items-center justify-center border border-indigo-100 shrink-0">
                        {c.name.charAt(0).toUpperCase()}
                      </div>
                      <div>
                        <div className="font-semibold text-slate-900 text-base">{c.name}</div>
                        <div className="text-xs text-slate-400">{c.phone || 'No phone'}</div>
                      </div>
                    </div>

                    <div className="text-right">
                      <div className={`font-display font-extrabold text-lg ${c.currentBalance > 0 ? 'text-amber-600' : c.currentBalance < 0 ? 'text-emerald-600' : 'text-slate-400'}`}>
                        {formatCurrency(Math.abs(c.currentBalance))}
                      </div>
                      <div className="text-[11px] font-semibold text-slate-400">
                        {c.currentBalance > 0 ? 'Lena Hai (Due)' : c.currentBalance < 0 ? 'Dena Hai (Advance)' : 'Cleared'}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
