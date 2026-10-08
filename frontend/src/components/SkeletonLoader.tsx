import React from 'react'

export function CardSkeleton() {
  return (
    <div className="card space-y-3 animate-pulse">
      <div className="flex items-center justify-between">
        <div className="h-4 bg-slate-200 rounded w-1/3" />
        <div className="h-8 w-8 bg-slate-200 rounded-full" />
      </div>
      <div className="h-7 bg-slate-200 rounded w-1/2" />
      <div className="h-3 bg-slate-200 rounded w-2/3" />
    </div>
  )
}

export function TableSkeleton({ rows = 5 }: { rows?: number }) {
  return (
    <div className="space-y-3 animate-pulse">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex items-center justify-between p-3.5 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-slate-200 shrink-0" />
            <div className="space-y-1.5">
              <div className="h-4 bg-slate-200 rounded w-32" />
              <div className="h-3 bg-slate-200 rounded w-24" />
            </div>
          </div>
          <div className="space-y-1.5 text-right">
            <div className="h-4 bg-slate-200 rounded w-20 ml-auto" />
            <div className="h-3 bg-slate-200 rounded w-12 ml-auto" />
          </div>
        </div>
      ))}
    </div>
  )
}

export function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <div className="space-y-2 animate-pulse">
        <div className="h-4 bg-slate-200 rounded w-36" />
        <div className="h-8 bg-slate-200 rounded w-64" />
        <div className="h-4 bg-slate-200 rounded w-80" />
      </div>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <CardSkeleton />
        <CardSkeleton />
        <CardSkeleton />
        <CardSkeleton />
      </div>
      <div className="grid lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 card h-64 animate-pulse bg-slate-100" />
        <div className="card h-64 animate-pulse bg-slate-100" />
      </div>
    </div>
  )
}
