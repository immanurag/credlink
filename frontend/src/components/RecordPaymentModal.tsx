import React, { useState, useEffect } from 'react'
import Modal from './Modal'
import { transactionService } from '../services/transaction'
import { customerService } from '../services/customer'
import { useToast } from '../context/ToastContext'
import { extractApiError } from '../services/api'
import { formatCurrency } from './formatters'
import type { Customer } from '../types'
import { IndianRupee, Calendar, Wallet, CheckCircle2, ArrowDownLeft } from 'lucide-react'

interface RecordPaymentModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
  defaultCustomerId?: number
}

export default function RecordPaymentModal({
  isOpen,
  onClose,
  onSuccess,
  defaultCustomerId
}: RecordPaymentModalProps) {
  const [customers, setCustomers] = useState<Customer[]>([])
  const [selectedCustomerId, setSelectedCustomerId] = useState<number | ''>(defaultCustomerId || '')
  const [amount, setAmount] = useState('')
  const [method, setMethod] = useState<'CASH' | 'UPI' | 'BANK' | 'CHEQUE'>('CASH')
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])
  const [notes, setNotes] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const toast = useToast()

  useEffect(() => {
    if (isOpen) {
      customerService.list().then(setCustomers).catch(() => {})
      if (defaultCustomerId) setSelectedCustomerId(defaultCustomerId)
    }
  }, [isOpen, defaultCustomerId])

  const selectedCustomer = customers.find((c) => c.id === Number(selectedCustomerId))
  const currentBalance = selectedCustomer?.currentBalance || 0
  const paymentAmount = Number(amount) || 0
  const remainingBalance = currentBalance - paymentAmount

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedCustomerId) {
      setError('Please select a customer')
      return
    }
    if (!paymentAmount || paymentAmount <= 0) {
      toast.warning('Please enter a valid amount.')
      return
    }

    setLoading(true)
    setError('')

    try {
      const description = `Jama (Payment received via ${method})${notes ? ` - ${notes}` : ''}`
      const preview = await transactionService.preview({
        customerId: Number(selectedCustomerId),
        type: 'JAMA',
        amount: paymentAmount,
        description,
        source: 'MANUAL'
      })
      await transactionService.confirm(preview.id)
      toast.success('Payment recorded successfully.')
      setAmount('')
      setNotes('')
      onSuccess()
      onClose()
    } catch (err) {
      setError(extractApiError(err))
      toast.error('Unable to save the payment. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Record Customer Payment (Jama)" subtitle="Log a cash, UPI or bank payment received from a party">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs rounded-lg">
            {error}
          </div>
        )}

        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1">
            Customer <span className="text-rose-500">*</span>
          </label>
          <select
            value={selectedCustomerId}
            onChange={(e) => setSelectedCustomerId(e.target.value ? Number(e.target.value) : '')}
            className="input-field"
            required
          >
            <option value="">-- Select Customer --</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name} {c.phone ? `(${c.phone})` : ''} — Due: {formatCurrency(c.currentBalance)}
              </option>
            ))}
          </select>
        </div>

        {selectedCustomer && (
          <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl space-y-1.5">
            <div className="flex justify-between items-center text-xs text-slate-600">
              <span>Current Outstanding Balance:</span>
              <span className={`font-bold text-sm ${currentBalance > 0 ? 'text-amber-600' : 'text-emerald-600'}`}>
                {formatCurrency(currentBalance)}
              </span>
            </div>
          </div>
        )}

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Payment Amount (₹) <span className="text-rose-500">*</span>
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
                className="input-field pl-9 font-semibold text-emerald-700"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Payment Method</label>
            <select
              value={method}
              onChange={(e) => setMethod(e.target.value as any)}
              className="input-field"
            >
              <option value="CASH">Cash (Nokad)</option>
              <option value="UPI">UPI / PhonePe / GPay</option>
              <option value="BANK">Bank Transfer</option>
              <option value="CHEQUE">Cheque</option>
            </select>
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1">Payment Date</label>
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

        {/* Live Remaining Balance Calculation */}
        {selectedCustomer && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl space-y-1">
            <div className="text-xs text-emerald-800 font-medium">Balance Calculation Preview:</div>
            <div className="flex items-center justify-between">
              <span className="text-sm font-semibold text-emerald-900">Remaining Balance:</span>
              <span className="font-display font-bold text-lg text-emerald-700">
                {formatCurrency(remainingBalance)}
              </span>
            </div>
            <div className="text-[11px] text-emerald-700">
              {remainingBalance <= 0
                ? '✓ Account cleared / in advance after this payment!'
                : `₹${remainingBalance.toLocaleString('en-IN')} remaining due.`}
            </div>
          </div>
        )}

        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1">Notes / Bill Reference (Optional)</label>
          <input
            type="text"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="e.g. Receipt #402, paid via GPay"
            className="input-field"
          />
        </div>

        <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
          <button type="button" onClick={onClose} disabled={loading} className="btn-secondary">
            Cancel
          </button>
          <button type="submit" disabled={loading} className="btn-success">
            <CheckCircle2 className="w-4 h-4" />
            {loading ? 'Recording...' : 'Record Payment'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
