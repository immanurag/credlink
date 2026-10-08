import React from 'react'
import type { VoiceState } from '../types'

const STATES: { key: VoiceState; label: string; sub: string }[] = [
  { key: 'IDLE', label: 'Idle', sub: 'Standby mode' },
  { key: 'LISTENING', label: 'Listening', sub: 'Audio capture' },
  { key: 'TRANSCRIBING', label: 'Transcribe', sub: 'Phonetic to text' },
  { key: 'EXTRACTING', label: 'Extracting', sub: 'NER slot fill' },
  { key: 'REVIEW', label: 'Review', sub: 'Merchant confirm' },
  { key: 'CONFIRMED', label: 'Confirmed', sub: 'Committed' }
]

export default function VoiceHUD({ state }: { state: VoiceState }) {
  const activeIndex = STATES.findIndex((s) => s.key === state)
  const isFailed = state === 'FAILED'

  return (
    <div className="flex gap-2 overflow-x-auto pb-1">
      {STATES.map((s, i) => {
        const active = s.key === state
        const past = i < activeIndex && !isFailed
        return (
          <div
            key={s.key}
            className={`shrink-0 rounded-action px-3 py-2 border text-xs min-w-[92px] ${
              active
                ? 'bg-primary text-on-primary border-primary'
                : past
                ? 'bg-surface-container-low border-outline text-secondary'
                : 'bg-white border-outline/50 text-on-surface-variant'
            }`}
          >
            <div className="font-semibold">{i + 1}. {s.label}</div>
            <div className="opacity-80">{s.sub}</div>
          </div>
        )
      })}
      {isFailed && (
        <div className="shrink-0 rounded-action px-3 py-2 border text-xs min-w-[92px] bg-error/10 border-error text-error">
          <div className="font-semibold">6. Failed</div>
          <div className="opacity-80">Fallback to manual</div>
        </div>
      )}
    </div>
  )
}
