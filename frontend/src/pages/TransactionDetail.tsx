import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { transactionService } from '../services/transaction'
import { formatCurrency, formatDate } from '../components/formatters'
import { CardSkeleton } from '../components/SkeletonLoader'
import type { Transaction } from '../types'
import { ArrowLeft, Receipt, Mic, CheckCircle2, ShieldCheck, ArrowUpRight, ArrowDownLeft } from 'lucide-react'

export default function TransactionDetail() {
  const { id } = useParams()
  const [tx, setTx] = useState<Transaction | null>(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    transactionService
      .get(Number(id))
      .then(setTx)
      .finally(() => setLoading(false))
  }, [id])

  if (loading || !tx) {
    return (
      <div className="max-w-xl mx-auto space-y-4">
        <CardSkeleton />
      </div>
    )
  }

  const isUdhaar = tx.type === 'UDHAAR'

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition"
      >
        <ArrowLeft className="w-4 h-4" />
        Back
      </button>

      <div className="card space-y-6 p-6">
        <div className="flex items-center justify-between border-b border-slate-100 pb-4">
          <div className="flex items-center gap-3">
            <div
              className={`w-12 h-12 rounded-2xl flex items-center justify-center font-bold ${
                isUdhaar ? 'bg-amber-50 text-amber-600 border border-amber-100' : 'bg-emerald-50 text-emerald-600 border border-emerald-100'
              }`}
            >
              {isUdhaar ? <ArrowUpRight className="w-6 h-6" /> : <ArrowDownLeft className="w-6 h-6" />}
            </div>
            <div>
              <h1 className="font-display font-extrabold text-xl text-slate-900">
                Transaction #{tx.id}
              </h1>
              <div className="text-xs text-slate-500 font-medium">Logged on {formatDate(tx.createdAt)}</div>
            </div>
          </div>

          <div className="text-right">
            <div className={`font-display font-extrabold text-2xl ${isUdhaar ? 'text-amber-600' : 'text-emerald-600'}`}>
              {isUdhaar ? '+' : '-'}{formatCurrency(tx.amount)}
            </div>
            <div className="text-xs text-slate-400 font-medium">Bal after: {formatCurrency(tx.balanceAfter)}</div>
          </div>
        </div>

        <div className="space-y-3 text-sm">
          <div className="flex justify-between py-2 border-b border-slate-100">
            <span className="text-slate-500 font-semibold">Type</span>
            <span className="font-bold">{tx.type === 'UDHAAR' ? 'Udhaar (Credit Given)' : 'Jama (Payment Received)'}</span>
          </div>

          <div className="flex justify-between py-2 border-b border-slate-100">
            <span className="text-slate-500 font-semibold">Description</span>
            <span className="font-bold text-slate-900">{tx.description || 'N/A'}</span>
          </div>

          <div className="flex justify-between py-2 border-b border-slate-100">
            <span className="text-slate-500 font-semibold">Source</span>
            <span className="font-bold flex items-center gap-1.5">
              {tx.source === 'VOICE' ? (
                <span className="inline-flex items-center gap-1 text-indigo-700 bg-indigo-50 border border-indigo-100 px-2 py-0.5 rounded-full text-xs">
                  <Mic className="w-3 h-3 text-indigo-600" />
                  Voice AI ({Math.round((tx.confidenceScore || 0) * 100)}%)
                </span>
              ) : (
                <span className="text-slate-700 bg-slate-100 px-2 py-0.5 rounded text-xs">Manual Input</span>
              )}
            </span>
          </div>

          <div className="flex justify-between py-2 border-b border-slate-100">
            <span className="text-slate-500 font-semibold">Status</span>
            <span className="font-bold text-emerald-600 flex items-center gap-1">
              <CheckCircle2 className="w-4 h-4" />
              {tx.status}
            </span>
          </div>

          {tx.voiceTranscript && (
            <div className="p-3.5 bg-slate-50 border border-slate-200 rounded-xl space-y-1">
              <div className="text-xs font-semibold text-slate-500 flex items-center gap-1">
                <Mic className="w-3.5 h-3.5 text-indigo-600" />
                Original Voice Transcript:
              </div>
              <div className="text-xs text-slate-800 italic font-medium">"{tx.voiceTranscript}"</div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
