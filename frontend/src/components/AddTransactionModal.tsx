import React, { useState, useEffect } from 'react'
import Modal from './Modal'
import { transactionService } from '../services/transaction'
import { customerService } from '../services/customer'
import { useToast } from '../context/ToastContext'
import { extractApiError } from '../services/api'
import { formatCurrency } from './formatters'
import type { Customer, TransactionType } from '../types'
import { IndianRupee, Calendar, FileText, ArrowUpRight, ArrowDownLeft, Loader2 } from 'lucide-react'

interface AddTransactionModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
  defaultCustomerId?: number
  defaultType?: TransactionType
}

export default function AddTransactionModal({
  isOpen,
  onClose,
  onSuccess,
  defaultCustomerId,
  defaultType = 'UDHAAR'
}: AddTransactionModalProps) {
  const [customers, setCustomers] = useState<Customer[]>([])
  const [selectedCustomerId, setSelectedCustomerId] = useState<number | ''>(defaultCustomerId || '')
  const [type, setType] = useState<TransactionType>(defaultType)
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const toast = useToast()

  useEffect(() => {
    if (isOpen) {
      customerService.list().then(setCustomers).catch(() => {})
      if (defaultCustomerId) setSelectedCustomerId(defaultCustomerId)
      if (defaultType) setType(defaultType)
    }
  }, [isOpen, defaultCustomerId, defaultType])

  const selectedCustomer = customers.find((c) => c.id === Number(selectedCustomerId))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedCustomerId) {
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
      const defaultDesc = description || (type === 'UDHAAR' ? 'Credit item purchase' : 'Payment received')
      const preview = await transactionService.preview({
        customerId: Number(selectedCustomerId),
        type,
        amount: numAmount,
        description: defaultDesc,
        source: 'MANUAL'
      })
      await transactionService.confirm(preview.id)
      toast.success('Transaction added successfully.')
      setAmount('')
      setDescription('')
      onSuccess()
      onClose()
    } catch (err) {
      setError(extractApiError(err))
      toast.error('Unable to save the transaction. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={type === 'UDHAAR' ? 'Give Credit (Udhaar Diya)' : 'Record Payment (Jama)'}
      subtitle="Log a new transaction entry in party ledger"
    >
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
                {c.name} {c.phone ? `(${c.phone})` : ''} — Current: {formatCurrency(c.currentBalance)}
              </option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Transaction Type</label>
            <div className="grid grid-cols-2 gap-1.5 p-1 bg-slate-100 rounded-lg">
              <button
                type="button"
                onClick={() => setType('UDHAAR')}
                className={`flex items-center justify-center gap-1 py-1.5 px-2 rounded-md text-xs font-semibold transition ${
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
                className={`flex items-center justify-center gap-1 py-1.5 px-2 rounded-md text-xs font-semibold transition ${
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
          <label className="block text-xs font-semibold text-slate-700 mb-1">Date</label>
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
          <label className="block text-xs font-semibold text-slate-700 mb-1">Item Description / Note</label>
          <div className="relative">
            <FileText className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={type === 'UDHAAR' ? 'e.g. 2 Groceries & Oil packets' : 'e.g. Cash payment'}
              className="input-field pl-9"
            />
          </div>
        </div>

        {selectedCustomer && Number(amount) > 0 && (
          <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-xs space-y-1">
            <div className="text-slate-600">Balance Preview:</div>
            <div className="flex justify-between font-medium">
              <span>After this transaction:</span>
              <span className="font-bold text-indigo-700">
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
          <button type="button" onClick={onClose} disabled={loading} className="btn-secondary">
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading}
            className={type === 'UDHAAR' ? 'btn-primary bg-amber-600 hover:bg-amber-700' : 'btn-success'}
          >
            {loading && <Loader2 className="w-4 h-4 animate-spin" />}
            {loading ? 'Saving...' : 'Save Transaction'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
