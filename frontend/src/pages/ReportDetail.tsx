import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { reportService } from '../services/report'
import { formatCurrency, formatDate } from '../components/formatters'
import { CardSkeleton } from '../components/SkeletonLoader'
import { FileDown, ArrowLeft, BarChart3, FileSpreadsheet } from 'lucide-react'

export default function ReportDetail() {
  const { customerId } = useParams()
  const [statement, setStatement] = useState<any>(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    reportService
      .statement(Number(customerId))
      .then(setStatement)
      .finally(() => setLoading(false))
  }, [customerId])

  if (loading || !statement) {
    return (
      <div className="space-y-4">
        <CardSkeleton />
        <CardSkeleton />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <button
        onClick={() => navigate('/reports')}
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition"
      >
        <ArrowLeft className="w-4 h-4" />
        Back to Reports
      </button>

      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="font-display font-extrabold text-2xl md:text-3xl text-slate-900">
            Statement &mdash; {statement.customerName}
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Complete transaction ledger audit history &amp; closing balance summary.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <a
            href={reportService.exportUrl(Number(customerId), 'pdf')}
            target="_blank"
            rel="noreferrer"
            className="btn-primary text-xs py-2.5 px-4"
          >
            <FileDown className="w-4 h-4" />
            Export PDF
          </a>
          <a
            href={reportService.exportUrl(Number(customerId), 'excel')}
            target="_blank"
            rel="noreferrer"
            className="btn-secondary text-xs py-2.5 px-4"
          >
            <FileSpreadsheet className="w-4 h-4 text-emerald-600" />
            Export Excel
          </a>
        </div>
      </div>

      <div className="grid sm:grid-cols-3 gap-4">
        <div className="card">
          <div className="text-xs font-semibold text-slate-500 uppercase">Opening Balance</div>
          <div className="text-2xl font-display font-bold text-slate-900 mt-1">
            {formatCurrency(statement.openingBalance)}
          </div>
        </div>
        <div className="card">
          <div className="text-xs font-semibold text-slate-500 uppercase">Total Udhaar / Jama</div>
          <div className="text-2xl font-display font-bold mt-1">
            <span className="text-amber-600">{formatCurrency(statement.currentOutstanding ?? statement.closingBalance)}</span> /{' '}
            <span className="text-emerald-600">{formatCurrency(statement.totalJama)}</span>
          </div>
        </div>
        <div className="card bg-indigo-50/50 border-indigo-200/80">
          <div className="text-xs font-semibold text-indigo-700 uppercase">Closing Balance</div>
          <div className="text-2xl font-display font-extrabold text-indigo-700 mt-1">
            {formatCurrency(statement.closingBalance)}
          </div>
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase font-semibold text-slate-500 border-b border-slate-100">
              <tr>
                <th className="py-3 px-4">Date</th>
                <th className="py-3 px-4">Description</th>
                <th className="py-3 px-4">Type</th>
                <th className="py-3 px-4 text-right">Amount</th>
                <th className="py-3 px-4 text-right">Balance After</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {statement.transactions.map((t: any) => (
                <tr key={t.id} className="hover:bg-slate-50 transition">
                  <td className="py-3.5 px-4 text-slate-500 text-xs">{formatDate(t.createdAt)}</td>
                  <td className="py-3.5 px-4 font-medium text-slate-900">{t.description}</td>
                  <td className="py-3.5 px-4">
                    {t.type === 'UDHAAR' ? (
                      <span className="pill-udhaar">Udhaar</span>
                    ) : (
                      <span className="pill-jama">Jama</span>
                    )}
                  </td>
                  <td className="py-3.5 px-4 text-right font-display font-bold">
                    <span className={t.type === 'UDHAAR' ? 'text-amber-600' : 'text-emerald-600'}>
                      {formatCurrency(t.amount)}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 text-right font-display font-bold text-slate-700">
                    {formatCurrency(t.balanceAfter)}
                  </td>
                </tr>
              ))}

              {statement.transactions.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400 text-xs">
                    No transactions found for this period.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
