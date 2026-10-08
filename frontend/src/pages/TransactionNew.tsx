import React, { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { customerService } from '../services/customer'
import { transactionService } from '../services/transaction'
import { extractApiError } from '../services/api'
import { useToast } from '../context/ToastContext'
import { formatCurrency } from '../components/formatters'
import type { Customer, TransactionType } from '../types'
import {
  IndianRupee,
  Calendar,
  FileText,
  ArrowUpRight,
  ArrowDownLeft,
  ArrowLeft,
  Loader2,
  CheckCircle2
} from 'lucide-react'

export default function TransactionNew() {
  const [params] = useSearchParams()
  const [customers, setCustomers] = useState<Customer[]>([])
  const [customerId, setCustomerId] = useState<number | ''>(
    params.get('customerId') ? Number(params.get('customerId')) : ''
  )
  const [type, setType] = useState<TransactionType>(
    (params.get('type') as TransactionType) || 'UDHAAR'
  )
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const toast = useToast()
  const navigate = useNavigate()

  useEffect(() => {
    customerService.list().then(setCustomers).catch(() => {})
  }, [])

  const selectedCustomer = customers.find((c) => c.id === Number(customerId))

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!customerId) {
      setError('Please select a customer')
      return
    }
    const numAmount = Number(amount)
    if (!numAmount || numAmount <= 0) {
      toast.warning('Please enter a valid amount.')
      return
    }

    setLoading(true)
    setError('')

    try {
      const defaultDesc = description || (type === 'UDHAAR' ? 'Credit entry' : 'Jama entry')
      const preview = await transactionService.preview({
        customerId: Number(customerId),
        type,
        amount: numAmount,
        description: defaultDesc,
        source: 'MANUAL'
      })
      await transactionService.confirm(preview.id)
      toast.success(type === 'UDHAAR' ? 'Transaction added successfully.' : 'Payment recorded successfully.')
      navigate(`/customers/${customerId}`)
    } catch (err) {
      setError(extractApiError(err))
      toast.error('Unable to save transaction. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition"
      >
        <ArrowLeft className="w-4 h-4" />
        Back
      </button>

      <div className="card space-y-6">
        <div>
          <h1 className="font-display font-extrabold text-2xl text-slate-900">
            {type === 'UDHAAR' ? 'New Udhaar Entry (Credit Given)' : 'New Jama Entry (Payment Received)'}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Log manual transaction details to update party ledger balances.
          </p>
        </div>

        {error && (
          <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-lg">
            {error}
          </div>
        )}

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Select Customer <span className="text-rose-500">*</span>
            </label>
            <select
              value={customerId}
              onChange={(e) => setCustomerId(e.target.value ? Number(e.target.value) : '')}
              className="input-field"
              required
            >
              <option value="">-- Choose Customer --</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} {c.phone ? `(${c.phone})` : ''} — Current: {formatCurrency(c.currentBalance)}
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Type</label>
              <div className="grid grid-cols-2 gap-1.5 p-1 bg-slate-100 rounded-lg">
                <button
                  type="button"
                  onClick={() => setType('UDHAAR')}
                  className={`flex items-center justify-center gap-1 py-2 px-2 rounded-md text-xs font-semibold transition ${
                    type === 'UDHAAR'
                      ? 'bg-amber-500 text-white shadow-sm'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  <ArrowUpRight className="w-3.5 h-3.5" />
                  Udhaar
                </button>
                <button
                  type="button"
                  onClick={() => setType('JAMA')}
                  className={`flex items-center justify-center gap-1 py-2 px-2 rounded-md text-xs font-semibold transition ${
                    type === 'JAMA'
                      ? 'bg-emerald-600 text-white shadow-sm'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  <ArrowDownLeft className="w-3.5 h-3.5" />
                  Jama
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Amount (₹) <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <IndianRupee className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
                <input
                  type="number"
                  min="0.01"
                  step="0.01"
                  required
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="e.g. 500"
                  className={`input-field pl-9 font-semibold ${
                    type === 'UDHAAR' ? 'text-amber-700' : 'text-emerald-700'
                  }`}
                />
              </div>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Transaction Date</label>
            <div className="relative">
              <Calendar className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="input-field pl-9"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Description / Notes (Optional)</label>
            <div className="relative">
              <FileText className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder={type === 'UDHAAR' ? 'e.g. 2 Groceries & Soap' : 'e.g. Cash payment'}
                className="input-field pl-9"
              />
            </div>
          </div>

          {selectedCustomer && Number(amount) > 0 && (
            <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-1">
              <div className="text-xs text-slate-500 font-medium">Ledger Impact Preview:</div>
              <div className="flex justify-between items-center text-sm font-semibold">
                <span>New Balance:</span>
                <span className="font-display font-bold text-lg text-indigo-700">
                  {formatCurrency(
                    type === 'UDHAAR'
                      ? selectedCustomer.currentBalance + Number(amount)
                      : selectedCustomer.currentBalance - Number(amount)
                  )}
                </span>
              </div>
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
            <button
              type="button"
              onClick={() => navigate(-1)}
              disabled={loading}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className={type === 'UDHAAR' ? 'btn-primary bg-amber-600 hover:bg-amber-700' : 'btn-success'}
            >
              {loading && <Loader2 className="w-4 h-4 animate-spin" />}
              {loading ? 'Saving Entry...' : 'Save Transaction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
