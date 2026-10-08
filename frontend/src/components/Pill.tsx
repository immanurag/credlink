import React from 'react'
import type { TransactionType } from '../types'
import { ArrowUpRight, ArrowDownLeft } from 'lucide-react'

export default function Pill({ type }: { type: TransactionType }) {
  return type === 'JAMA' ? (
    <span className="pill-jama">
      <ArrowDownLeft className="w-3 h-3" />
      Jama (In)
    </span>
  ) : (
    <span className="pill-udhaar">
      <ArrowUpRight className="w-3 h-3" />
      Udhaar (Due)
    </span>
  )
}
