import React, { useEffect, useState } from 'react'
import { Link, useOutletContext } from 'react-router-dom'
import { dashboardService } from '../services/dashboard'
import { customerService } from '../services/customer'
import { notificationService } from '../services/notification'
import { extractApiError } from '../services/api'
import BalanceCard from '../components/BalanceCard'
import TrendBarChart from '../components/TrendBarChart'
import { DashboardSkeleton } from '../components/SkeletonLoader'
import { formatCurrency } from '../components/formatters'
import { useToast } from '../context/ToastContext'
import type { Customer, DashboardSummary, TrendPoint } from '../types'
import {
  Users,
  Wallet,
  CheckCircle2,
  Clock,
  Mic,
  Plus,
  ArrowUpRight,
  ArrowDownLeft,
  Send,
  AlertCircle,
  RefreshCw,
  Sparkles,
  ChevronRight
} from 'lucide-react'

interface LayoutContextType {
  openAddCustomer: () => void
  openTransaction: () => void
  openPayment: () => void
}

export default function Dashboard() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null)
  const [trend, setTrend] = useState<TrendPoint[]>([])
  const [customers, setCustomers] = useState<Customer[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [sendingBulk, setSendingBulk] = useState(false)

  const { openAddCustomer, openTransaction, openPayment } = useOutletContext<LayoutContextType>()
  const toast = useToast()

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const [s, t, c] = await Promise.all([
        dashboardService.summary(),
        dashboardService.trend(7),
        customerService.list()
      ])
      setSummary(s)
      setTrend(t || [])
      setCustomers(c || [])
    } catch (err: any) {
      console.error('Failed to load dashboard:', err)
      setError(extractApiError(err))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()

    // Listen for data refresh events from global modals
    const handleDataUpdated = () => load()
    window.addEventListener('credlink:data-updated', handleDataUpdated)
    return () => window.removeEventListener('credlink:data-updated', handleDataUpdated)
  }, [])

  const overdue = [...customers]
    .filter((c) => (c.currentBalance ?? 0) > 0)
    .sort((a, b) => (b.currentBalance ?? 0) - (a.currentBalance ?? 0))
    .slice(0, 5)

  const sendBulkReminders = async () => {
    setSendingBulk(true)
    try {
      await notificationService.remindAllOverdue()
      toast.success('WhatsApp payment reminders queued for overdue customers.')
    } catch (err) {
      toast.error('Unable to send reminders. Please try again.')
    } finally {
      setSendingBulk(false)
    }
  }

  const handleRemindOne = async (id: number, name: string) => {
    try {
      await notificationService.remindOne(id)
      toast.success(`WhatsApp reminder sent to ${name}.`)
    } catch {
      toast.error(`Could not send reminder to ${name}.`)
    }
  }

  if (loading) {
    return <DashboardSkeleton />
  }

  if (error) {
    return (
      <div className="card bg-rose-50/50 border-rose-200 text-center p-8 space-y-4 max-w-md mx-auto">
        <div className="w-12 h-12 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto">
          <AlertCircle className="w-6 h-6" />
        </div>
        <div>
          <h3 className="font-display font-semibold text-rose-900 text-lg">Failed to Load Dashboard</h3>
          <p className="text-xs text-rose-700 mt-1">{error}</p>
        </div>
        <button onClick={load} className="btn-primary bg-rose-600 hover:bg-rose-700 text-xs py-2 px-4">
          <RefreshCw className="w-4 h-4" />
          Retry Loading
        </button>
      </div>
    )
  }

  if (!summary) return null

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-indigo-50 border border-indigo-100 text-indigo-700 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
            Bahi-Khata Merchant Centre &bull; Live Sync
          </div>
          <h1 className="font-display font-extrabold text-2xl md:text-3xl text-slate-900 mt-1.5">
            Store Command Dashboard
          </h1>
          <p className="text-xs md:text-sm text-slate-500">
            Real-time ledger balance, AI voice entry pipeline &amp; automated customer WhatsApp notices.
          </p>
        </div>

        {/* Action Buttons Bar */}
        <div className="flex items-center gap-2 flex-wrap">
          <button onClick={openAddCustomer} className="btn-secondary text-xs">
            <Plus className="w-4 h-4 text-slate-500" />
            + Customer
          </button>
          <button onClick={openTransaction} className="btn-secondary text-xs">
            <ArrowUpRight className="w-4 h-4 text-amber-600" />
            + Udhaar
          </button>
          <button onClick={openPayment} className="btn-success text-xs">
            <ArrowDownLeft className="w-4 h-4" />
            Record Payment
          </button>
        </div>
      </div>

      {/* 4 Summary Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <BalanceCard
          label="Total Customers"
          value={String(summary.activeCustomerCount)}
          icon={Users}
          tone="primary"
          sublabel={`${summary.newCustomersThisWeek} added this week`}
        />
        <BalanceCard
          label="Total Outstanding (Udhaar)"
          value={formatCurrency(summary.totalLenaHai)}
          icon={Wallet}
          tone="tertiary"
          sublabel="Market dues owed to you"
        />
        <BalanceCard
          label="Today's Payment (Jama)"
          value={formatCurrency(summary.todaysJama)}
          icon={CheckCircle2}
          tone="success"
          sublabel={`${summary.todaysTransactionCount} transactions today`}
        />
        <BalanceCard
          label="Today's Credit Given"
          value={formatCurrency(summary.todaysUdhaar)}
          icon={Clock}
          tone="warning"
          sublabel="New udhaar logged today"
        />
      </div>

      {/* Voice AI Banner */}
      <Link
        to="/voice"
        className="block card bg-gradient-to-r from-indigo-600 to-indigo-700 text-white hover:shadow-xl hover:scale-[1.005] transition-all duration-200 border-none relative overflow-hidden"
      >
        <div className="flex items-center justify-between gap-4 p-1">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-2xl bg-white/10 backdrop-blur border border-white/20 text-white flex items-center justify-center shrink-0">
              <Mic className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <div className="font-display font-bold text-lg flex items-center gap-2">
                Bol Kar Khata Likhein (Voice AI Entry)
                <span className="text-[10px] bg-white/20 text-white px-2 py-0.5 rounded-full font-semibold uppercase">
                  Sarvam AI Supported
                </span>
              </div>
              <div className="text-xs text-indigo-100 mt-0.5 max-w-2xl">
                Simply dictate credit or debit settlements in Hindi or Hinglish. AI extracts customer names, amounts &amp; updates ledger balances instantly.
              </div>
            </div>
          </div>
          <div className="hidden sm:flex items-center gap-1 text-xs font-semibold bg-white/10 hover:bg-white/20 px-3.5 py-2 rounded-xl border border-white/20 transition">
            <span>Tap to Speak</span>
            <ChevronRight className="w-4 h-4" />
          </div>
        </div>
      </Link>

      {/* Grid: Chart + Top Dues */}
      <div className="grid lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <TrendBarChart data={trend} />
        </div>

        {/* Top Outstanding Dues Card */}
        <div className="card flex flex-col justify-between space-y-4">
          <div>
            <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-3">
              <div>
                <h3 className="font-display font-semibold text-sm text-slate-900">Highest Outstanding Dues</h3>
                <p className="text-[11px] text-slate-500">Parties with highest pending balance</p>
              </div>
              <Link to="/customers?filter=due" className="text-xs font-semibold text-indigo-600 hover:underline">
                View All
              </Link>
            </div>

            <div className="space-y-3">
              {overdue.length === 0 && (
                <div className="text-xs text-slate-400 py-6 text-center">
                  ✓ No outstanding dues right now. Great job!
                </div>
              )}

              {overdue.map((c) => (
                <div key={c.id} className="flex items-center justify-between gap-2 p-2 hover:bg-slate-50 rounded-lg transition">
                  <div className="min-w-0">
                    <Link to={`/customers/${c.id}`} className="text-sm font-semibold text-slate-900 hover:text-indigo-600 truncate block">
                      {c.name}
                    </Link>
                    <div className="text-xs font-bold text-amber-600">
                      {formatCurrency(c.currentBalance)} due
                    </div>
                  </div>

                  <button
                    onClick={() => handleRemindOne(c.id, c.name)}
                    className="inline-flex items-center gap-1 text-xs font-medium text-slate-700 bg-white border border-slate-200 hover:bg-slate-100 rounded-lg px-2.5 py-1 shrink-0 transition"
                  >
                    <Send className="w-3 h-3 text-emerald-600" />
                    Remind
                  </button>
                </div>
              ))}
            </div>
          </div>

          {overdue.length > 0 && (
            <button
              onClick={sendBulkReminders}
              disabled={sendingBulk}
              className="btn-secondary w-full text-xs py-2.5 justify-center mt-2 border-indigo-200 text-indigo-700 bg-indigo-50/50 hover:bg-indigo-50"
            >
              <Send className="w-3.5 h-3.5 text-indigo-600" />
              {sendingBulk ? 'Sending Notices...' : 'Send WhatsApp Payment Reminders'}
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
