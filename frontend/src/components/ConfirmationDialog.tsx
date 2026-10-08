import React from 'react'
import Modal from './Modal'
import { AlertTriangle, CheckCircle2 } from 'lucide-react'

interface ConfirmationDialogProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'warning' | 'primary' | 'success'
  loading?: boolean
}

export default function ConfirmationDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  variant = 'primary',
  loading = false
}: ConfirmationDialogProps) {
  const buttonStyle = {
    danger: 'btn-danger',
    warning: 'bg-amber-600 text-white hover:bg-amber-700 btn-primary',
    primary: 'btn-primary',
    success: 'btn-success'
  }[variant]

  const IconComponent = variant === 'danger' || variant === 'warning' ? AlertTriangle : CheckCircle2
  const iconColor = {
    danger: 'text-rose-600 bg-rose-50 border-rose-100',
    warning: 'text-amber-600 bg-amber-50 border-amber-100',
    primary: 'text-indigo-600 bg-indigo-50 border-indigo-100',
    success: 'text-emerald-600 bg-emerald-50 border-emerald-100'
  }[variant]

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} maxWidth="sm">
      <div className="space-y-4">
        <div className="flex items-start gap-4">
          <div className={`p-3 rounded-full border shrink-0 ${iconColor}`}>
            <IconComponent className="w-6 h-6" />
          </div>
          <p className="text-sm text-slate-600 leading-relaxed pt-1">{message}</p>
        </div>

        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-100">
          <button type="button" onClick={onClose} disabled={loading} className="btn-secondary">
            {cancelText}
          </button>
          <button type="button" onClick={onConfirm} disabled={loading} className={buttonStyle}>
            {loading ? 'Processing...' : confirmText}
          </button>
        </div>
      </div>
    </Modal>
  )
}
