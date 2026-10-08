import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { ShieldCheck, Mail, ArrowRight, Loader2, CheckCircle2, KeyRound } from 'lucide-react'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [error, setError] = useState('')
  const { requestOtp } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email || !email.includes('@')) {
      setError('Please enter a valid registered email address.')
      return
    }

    setLoading(true)
    setError('')
    try {
      await requestOtp(email)
      setSubmitted(true)
    } catch {
      setError('Could not process request. Please check your email and try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4 font-body">
      <div className="w-full max-w-md bg-white rounded-3xl border border-slate-200 shadow-xl p-8 space-y-6">
        <div className="text-center space-y-2">
          <div className="w-14 h-14 rounded-2xl bg-indigo-600 text-white font-display font-extrabold text-2xl flex items-center justify-center mx-auto shadow-lg shadow-indigo-600/20">
            <KeyRound className="w-7 h-7" />
          </div>
          <h1 className="font-display font-extrabold text-2xl text-slate-900">
            Forgot Your Password?
          </h1>
          <p className="text-xs text-slate-500 leading-relaxed">
            Enter your registered email or phone number and we'll help you reset your password with a verification code.
          </p>
        </div>

        {error && (
          <div className="p-3.5 bg-rose-50 border border-rose-200 text-rose-900 rounded-xl text-xs">
            {error}
          </div>
        )}

        {submitted ? (
          <div className="p-6 bg-emerald-50 border border-emerald-200 rounded-2xl text-center space-y-3">
            <CheckCircle2 className="w-10 h-10 text-emerald-600 mx-auto" />
            <h3 className="font-display font-bold text-emerald-900 text-lg">Reset Code Sent</h3>
            <p className="text-xs text-emerald-800">
              A 6-digit login verification code was sent to <span className="font-bold">{email}</span>.
            </p>
            <button
              onClick={() => navigate('/auth/verify-otp', { state: { email } })}
              className="btn-primary w-full text-xs py-2.5 mt-2"
            >
              Proceed to Enter Code
            </button>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Registered Email / Phone <span className="text-rose-500">*</span>
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@store.com"
                  className="input-field pl-10"
                />
              </div>
            </div>

            <button type="submit" disabled={loading} className="btn-primary w-full py-3.5 text-base font-semibold">
              {loading ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  Sending Code...
                </>
              ) : (
                <>
                  Continue
                  <ArrowRight className="w-5 h-5" />
                </>
              )}
            </button>
          </form>
        )}

        <div className="text-center text-xs text-slate-500">
          Remembered your credentials?{' '}
          <Link to="/auth/login" className="font-bold text-indigo-600 hover:underline">
            Back to Login
          </Link>
        </div>
      </div>
    </div>
  )
}
