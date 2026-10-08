import React from 'react'
import { LucideIcon } from 'lucide-react'

interface Props {
  label: string
  value: string
  sublabel?: string
  icon?: LucideIcon
  tone?: 'primary' | 'tertiary' | 'success' | 'warning' | 'neutral'
  badgeText?: string
}

export default function BalanceCard({
  label,
  value,
  sublabel,
  icon: Icon,
  tone = 'neutral',
  badgeText
}: Props) {
  const styles = {
    primary: {
      text: 'text-indigo-600',
      bgIcon: 'bg-indigo-50 text-indigo-600 border-indigo-100',
      border: 'hover:border-indigo-300'
    },
    tertiary: {
      text: 'text-amber-600',
      bgIcon: 'bg-amber-50 text-amber-600 border-amber-100',
      border: 'hover:border-amber-300'
    },
    success: {
      text: 'text-emerald-600',
      bgIcon: 'bg-emerald-50 text-emerald-600 border-emerald-100',
      border: 'hover:border-emerald-300'
    },
    warning: {
      text: 'text-rose-600',
      bgIcon: 'bg-rose-50 text-rose-600 border-rose-100',
      border: 'hover:border-rose-300'
    },
    neutral: {
      text: 'text-slate-900',
      bgIcon: 'bg-slate-100 text-slate-600 border-slate-200',
      border: 'hover:border-slate-300'
    }
  }[tone]

  return (
    <div className={`card card-interactive flex flex-col justify-between ${styles.border}`}>
      <div className="flex items-start justify-between gap-2">
        <div className="text-xs uppercase tracking-wide text-slate-500 font-bold">{label}</div>
        {Icon && (
          <div className={`p-2.5 rounded-xl border shrink-0 ${styles.bgIcon}`}>
            <Icon className="w-5 h-5" />
          </div>
        )}
      </div>

      <div className="mt-3">
        <div className={`text-2xl sm:text-3xl font-display font-extrabold tracking-tight ${styles.text}`}>
          {value}
        </div>
        {sublabel && (
          <div className="text-xs text-slate-500 font-medium mt-1.5 flex items-center gap-1">
            {badgeText && (
              <span className="px-1.5 py-0.5 rounded bg-slate-100 font-bold text-slate-700 text-[10px]">
                {badgeText}
              </span>
            )}
            <span>{sublabel}</span>
          </div>
        )}
      </div>
    </div>
  )
}
