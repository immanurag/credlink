import React from 'react'
import type { Transaction } from '../types'
import Pill from './Pill'
import { formatCurrency, formatDate } from './formatters'
import { Mic, ArrowUpRight, ArrowDownLeft } from 'lucide-react'

export default function LedgerRow({ tx }: { tx: Transaction }) {
  const isUdhaar = tx.type === 'UDHAAR'

  return (
    <div className="flex items-center justify-between py-3.5 px-3 border-b border-slate-100 last:border-0 hover:bg-slate-50/80 rounded-lg transition-colors">
      <div className="flex items-center gap-3 min-w-0">
        <div
          className={`w-9 h-9 rounded-full flex items-center justify-center shrink-0 ${
            isUdhaar ? 'bg-amber-50 text-amber-600 border border-amber-100' : 'bg-emerald-50 text-emerald-600 border border-emerald-100'
          }`}
        >
          {isUdhaar ? <ArrowUpRight className="w-5 h-5" /> : <ArrowDownLeft className="w-5 h-5" />}
        </div>

        <div className="min-w-0">
          <div className="text-sm font-semibold text-slate-900 truncate">
            {tx.description || (isUdhaar ? 'Udhaar entry' : 'Jama entry')}
          </div>
          <div className="text-xs text-slate-500 flex items-center gap-2 mt-0.5 flex-wrap">
            <Pill type={tx.type} />
            <span>{formatDate(tx.createdAt)}</span>
            {tx.source === 'VOICE' && (
              <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-indigo-600 bg-indigo-50 border border-indigo-100 px-2 py-0.5 rounded-full">
                <Mic className="w-3 h-3" />
                Voice AI ({Math.round((tx.confidenceScore || 0) * 100)}%)
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="text-right shrink-0 pl-3">
        <div className={`font-display font-bold text-base ${isUdhaar ? 'text-amber-600' : 'text-emerald-600'}`}>
          {isUdhaar ? '+' : '-'}{formatCurrency(tx.amount)}
        </div>
        <div className="text-xs text-slate-400 font-medium">Bal: {formatCurrency(tx.balanceAfter)}</div>
      </div>
    </div>
  )
}
