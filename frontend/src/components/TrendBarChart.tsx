import React from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend } from 'recharts'
import type { TrendPoint } from '../types'
import { TrendingUp } from 'lucide-react'

export default function TrendBarChart({ data }: { data: TrendPoint[] }) {
  return (
    <div className="card space-y-3">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-display font-semibold text-base text-slate-900 flex items-center gap-2">
            Weekly Udhaar vs Jama Velocity
            <TrendingUp className="w-4 h-4 text-indigo-600" />
          </h3>
          <p className="text-xs text-slate-500">Daily breakdown of credit outflow vs collection inflow</p>
        </div>
      </div>

      <div className="w-full h-[240px] pt-2">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
            <XAxis dataKey="date" fontSize={11} stroke="#94a3b8" tickLine={false} />
            <YAxis fontSize={11} stroke="#94a3b8" tickLine={false} axisLine={false} />
            <Tooltip
              formatter={(v: number) => ['₹' + (v || 0).toLocaleString('en-IN'), '']}
              contentStyle={{
                backgroundColor: '#0f172a',
                borderRadius: '0.75rem',
                color: '#fff',
                border: 'none',
                boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1)',
                fontSize: '12px'
              }}
              itemStyle={{ color: '#fff' }}
            />
            <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px' }} />
            <Bar dataKey="udhaar" name="Udhaar (Credit Outflow)" fill="#f59e0b" radius={[6, 6, 0, 0]} barSize={16} />
            <Bar dataKey="jama" name="Jama (Collection Inflow)" fill="#10b981" radius={[6, 6, 0, 0]} barSize={16} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}
