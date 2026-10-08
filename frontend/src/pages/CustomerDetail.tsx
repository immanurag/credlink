import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { customerService } from '../services/customer'
import { notificationService } from '../services/notification'
import { reportService } from '../services/report'
import LedgerRow from '../components/LedgerRow'
import { formatCurrency } from '../components/formatters'
import { CardSkeleton } from '../components/SkeletonLoader'
import { useToast } from '../context/ToastContext'
import AddTransactionModal from '../components/AddTransactionModal'
import RecordPaymentModal from '../components/RecordPaymentModal'
import type { Customer, Transaction } from '../types'
import {
  User,
  Phone,
  MapPin,
  Send,
  FileDown,
  Plus,
  ArrowUpRight,
  ArrowDownLeft,
  ArrowLeft,
  ShieldCheck,
  Receipt,
  CheckCircle2,
  AlertTriangle
} from 'lucide-react'

export default function CustomerDetail() {
  const { id } = useParams()
  const customerId = Number(id)
  const [customer, setCustomer] = useState<Customer | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)

  // Modal triggers
  const [showUdhaarModal, setShowUdhaarModal] = useState(false)
  const [showPaymentModal, setShowPaymentModal] = useState(false)

  const toast = useToast()
  const navigate = useNavigate()

  const load = async () => {
    setLoading(true)
    try {
      const [c, tx] = await Promise.all([
        customerService.get(customerId),
        customerService.transactions(customerId)
      ])
      setCustomer(c)
      setTransactions((tx || []).slice().reverse())
    } catch {
      toast.error('Failed to load customer profile.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()

    const handleDataUpdated = () => load()
    window.addEventListener('credlink:data-updated', handleDataUpdated)
    return () => window.removeEventListener('credlink:data-updated', handleDataUpdated)
  }, [customerId])

  const handleSendReminder = async () => {
    if (!customer?.id) return
    try {
      await notificationService.remindOne(customer.id)
      toast.success(`WhatsApp reminder notice sent to ${customer.name}.`)
    } catch {
      toast.error('Could not send WhatsApp notice.')
    }
  }

  if (loading || !customer) {
    return (
      <div className="space-y-4">
        <CardSkeleton />
        <CardSkeleton />
      </div>
    )
  }

  const isDue = customer.currentBalance > 0
  const isAdvance = customer.currentBalance < 0
  const isCleared = customer.currentBalance === 0

  const thisMonth = transactions.filter((t) => {
    const d = new Date(t.createdAt)
    const now = new Date()
    return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear()
  })
  const monthUdhaar = thisMonth.filter((t) => t.type === 'UDHAAR').reduce((s, t) => s + t.amount, 0)
  const monthJama = thisMonth.filter((t) => t.type === 'JAMA').reduce((s, t) => s + t.amount, 0)

  return (
    <div className="space-y-6">
      {/* Back Button */}
      <button
        onClick={() => navigate('/customers')}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition"
      >
        <ArrowLeft className="w-4 h-4" />
        Back to Customers
      </button>

      {/* Main Profile Card */}
      <div className="card space-y-6 border-slate-200">
        <div className="flex flex-col md:flex-row md:items-start justify-between gap-6">
          {/* Customer Metadata */}
          <div className="flex items-start gap-4">
            <div className="w-14 h-14 rounded-2xl bg-indigo-600 text-white font-display font-extrabold text-2xl flex items-center justify-center shadow-lg shadow-indigo-600/20 shrink-0">
              {customer.name.charAt(0).toUpperCase()}
            </div>
            <div>
              <h1 className="font-display font-extrabold text-2xl text-slate-900 flex items-center gap-2">
                {customer.name}
                {customer.trustScore != null && (
                  <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-indigo-50 border border-indigo-100 text-indigo-700 text-xs font-semibold">
                    <ShieldCheck className="w-3.5 h-3.5 text-indigo-600" />
                    Trust: {customer.trustScore}%
                  </span>
                )}
              </h1>
              <div className="text-sm font-semibold text-slate-600 flex items-center gap-3 mt-1 flex-wrap">
                {customer.phone && (
                  <span className="inline-flex items-center gap-1 text-slate-600">
                    <Phone className="w-3.5 h-3.5 text-slate-400" />
                    {customer.phone}
                  </span>
                )}
                {customer.category && (
                  <span className="bg-slate-100 text-slate-700 text-xs px-2 py-0.5 rounded font-medium">
                    {customer.category}
                  </span>
                )}
              </div>
              {customer.address && (
                <div className="text-xs text-slate-400 flex items-center gap-1 mt-1.5">
                  <MapPin className="w-3.5 h-3.5" />
                  {customer.address}
                </div>
              )}
            </div>
          </div>

          {/* Outstanding Balance Banner */}
          <div
            className={`p-5 rounded-2xl border flex flex-col justify-between min-w-[240px] text-right ${
              isDue
                ? 'bg-amber-50/70 border-amber-200 text-amber-900'
                : isAdvance
                ? 'bg-emerald-50/70 border-emerald-200 text-emerald-900'
                : 'bg-slate-50 border-slate-200 text-slate-800'
            }`}
          >
            <div className="text-xs font-bold uppercase tracking-wider text-slate-500">
              {isDue ? 'Net Lena Hai (Due)' : isAdvance ? 'Net Dena Hai (Advance)' : 'Account Balance'}
            </div>
            <div
              className={`font-display font-extrabold text-3xl mt-1.5 ${
                isDue ? 'text-amber-600' : isAdvance ? 'text-emerald-600' : 'text-slate-800'
              }`}
            >
              {formatCurrency(Math.abs(customer.currentBalance))}
            </div>
            {customer.creditLimit != null && (
              <div className="text-xs text-slate-500 mt-1 font-medium">
                Credit Limit: {formatCurrency(customer.creditLimit)}
              </div>
            )}
          </div>
        </div>

        {/* Action Button Bar */}
        <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-slate-100">
          <div className="flex items-center gap-2 flex-wrap">
            <button
              onClick={() => setShowUdhaarModal(true)}
              className="btn-primary bg-amber-600 hover:bg-amber-700 text-xs"
            >
              <ArrowUpRight className="w-4 h-4" />
              + Give Udhaar
            </button>
            <button
              onClick={() => setShowPaymentModal(true)}
              className="btn-success text-xs"
            >
              <ArrowDownLeft className="w-4 h-4" />
              + Record Payment (Jama)
            </button>
            {isDue && (
              <button
                onClick={handleSendReminder}
                className="btn-secondary text-xs text-emerald-700 border-emerald-200 bg-emerald-50/50 hover:bg-emerald-50"
              >
                <Send className="w-3.5 h-3.5 text-emerald-600" />
                WhatsApp Notice
              </button>
            )}
          </div>

          <div className="flex items-center gap-2">
            <a
              href={reportService.exportUrl(customer.id, 'pdf')}
              target="_blank"
              rel="noreferrer"
              className="btn-secondary text-xs"
            >
              <FileDown className="w-3.5 h-3.5 text-slate-500" />
              PDF Statement
            </a>
            <a
              href={reportService.exportUrl(customer.id, 'excel')}
              target="_blank"
              rel="noreferrer"
              className="btn-secondary text-xs"
            >
              <FileDown className="w-3.5 h-3.5 text-slate-500" />
              Excel
            </a>
          </div>
        </div>
      </div>

      {/* Month Stats Grid */}
      <div className="grid sm:grid-cols-2 gap-4">
        <div className="card bg-amber-50/40 border-amber-200/60 p-4">
          <div className="text-xs text-amber-700 font-bold uppercase">This Month Udhaar Given</div>
          <div className="text-2xl font-display font-bold text-amber-600 mt-1">
            {formatCurrency(monthUdhaar)}
          </div>
        </div>
        <div className="card bg-emerald-50/40 border-emerald-200/60 p-4">
          <div className="text-xs text-emerald-700 font-bold uppercase">This Month Jama Received</div>
          <div className="text-2xl font-display font-bold text-emerald-600 mt-1">
            {formatCurrency(monthJama)}
          </div>
        </div>
      </div>

      {/* Running Ledger Section */}
      <div className="card space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <h3 className="font-display font-semibold text-base text-slate-900 flex items-center gap-2">
            <Receipt className="w-4 h-4 text-indigo-600" />
            Running Ledger History ({transactions.length})
          </h3>
        </div>

        <div className="divide-y divide-slate-100">
          {transactions.map((t) => (
            <LedgerRow key={t.id} tx={t} />
          ))}

          {transactions.length === 0 && (
            <div className="py-12 text-center text-slate-400 text-sm space-y-2">
              <p>No transactions logged for {customer.name} yet.</p>
              <button
                onClick={() => setShowUdhaarModal(true)}
                className="btn-secondary text-xs"
              >
                + Add First Transaction
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Pre-filled Modals */}
      <AddTransactionModal
        isOpen={showUdhaarModal}
        onClose={() => setShowUdhaarModal(false)}
        onSuccess={load}
        defaultCustomerId={customer.id}
        defaultType="UDHAAR"
      />

      <RecordPaymentModal
        isOpen={showPaymentModal}
        onClose={() => setShowPaymentModal(false)}
        onSuccess={load}
        defaultCustomerId={customer.id}
      />
    </div>
  )
}
