import React, { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { extractApiError } from '../services/api'
import {
  ShieldCheck,
  Mail,
  Lock,
  Eye,
  EyeOff,
  ArrowRight,
  CheckCircle2,
  Loader2,
  Store,
  AlertCircle,
  Sparkles,
  Zap,
  Mic,
  LockKeyhole
} from 'lucide-react'

export default function Login() {
  const [searchParams] = useSearchParams()
  const sessionExpired = searchParams.get('sessionExpired') === 'true'

  const [mode, setMode] = useState<'otp' | 'password'>('otp')
  const [email, setEmail] = useState('')
  const [storeName, setStoreName] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [rememberMe, setRememberMe] = useState(true)

  const [error, setError] = useState('')
  const [isSuccess, setIsSuccess] = useState(false)

  const { requestOtp, loading } = useAuth()
  const navigate = useNavigate()

  const isValidEmail = email.includes('@') && email.includes('.')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setIsSuccess(false)

    if (!isValidEmail) {
      setError('Please enter a valid email address.')
      return
    }

    if (mode === 'password' && password.length < 8) {
      setError('Password must be at least 8 characters.')
      return
    }

    try {
      await requestOtp(email, storeName)
      setIsSuccess(true)
      setTimeout(() => {
        navigate('/auth/verify-otp', { state: { email } })
      }, 600)
    } catch (err: any) {
      setError(extractApiError(err))
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-100 via-indigo-50/40 to-slate-100 flex items-center justify-center p-4 sm:p-6 lg:p-8 font-body relative overflow-hidden">
      {/* Background Ambient Glow Orbs */}
      <div className="absolute top-1/4 left-10 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 right-10 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

      {/* Main SaaS Card */}
      <div className="w-full max-w-5xl bg-white/95 backdrop-blur-xl rounded-3xl border border-slate-200/90 shadow-2xl overflow-hidden grid lg:grid-cols-12 relative z-10">
        {/* LEFT COLUMN: Premium SaaS Branding & Feature Highlights */}
        <div className="lg:col-span-6 bg-gradient-to-br from-indigo-600 via-indigo-700 to-slate-900 p-8 sm:p-12 text-white flex flex-col justify-between relative overflow-hidden">
          {/* Subtle Background Pattern Decorative Elements */}
          <div className="absolute -top-24 -right-24 w-64 h-64 bg-white/5 rounded-full blur-2xl pointer-events-none" />

          <div className="space-y-8 relative z-10">
            <Link to="/" className="inline-flex items-center gap-3 group">
              <div className="w-11 h-11 rounded-2xl bg-white text-indigo-700 font-display font-extrabold text-2xl flex items-center justify-center shadow-lg group-hover:scale-105 transition-transform">
                CL
              </div>
              <div>
                <span className="font-display font-extrabold text-2xl text-white tracking-tight flex items-center gap-1.5">
                  CredLink
                  <ShieldCheck className="w-5 h-5 text-indigo-300" />
                </span>
                <span className="text-[10px] font-bold text-indigo-200 uppercase tracking-widest block -mt-1">
                  Digital Bahi-Khata System
                </span>
              </div>
            </Link>

            <div className="space-y-3 pt-2">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 backdrop-blur border border-white/10 text-indigo-200 text-xs font-semibold">
                <Sparkles className="w-3.5 h-3.5 text-indigo-300" />
                Trusted Merchant Platform
              </div>
              <h2 className="font-display font-extrabold text-3xl sm:text-4xl text-white leading-tight">
                Welcome Back to CredLink
              </h2>
              <p className="text-sm text-indigo-100 leading-relaxed max-w-md">
                Manage your customers, credit entries, payments, and collections seamlessly from one modern dashboard.
              </p>
            </div>

            {/* Benefit Checkmarks */}
            <div className="space-y-3.5 pt-2">
              <div className="flex items-center gap-3 text-xs font-semibold text-indigo-100">
                <div className="w-6 h-6 rounded-full bg-emerald-500/20 border border-emerald-400/30 flex items-center justify-center text-emerald-300 shrink-0">
                  <Zap className="w-3.5 h-3.5" />
                </div>
                <span>Instant ledger balance calculation &amp; credit tracking</span>
              </div>
              <div className="flex items-center gap-3 text-xs font-semibold text-indigo-100">
                <div className="w-6 h-6 rounded-full bg-emerald-500/20 border border-emerald-400/30 flex items-center justify-center text-emerald-300 shrink-0">
                  <Mic className="w-3.5 h-3.5" />
                </div>
                <span>Hindi &amp; Hinglish Voice AI transaction dictation</span>
              </div>
              <div className="flex items-center gap-3 text-xs font-semibold text-indigo-100">
                <div className="w-6 h-6 rounded-full bg-emerald-500/20 border border-emerald-400/30 flex items-center justify-center text-emerald-300 shrink-0">
                  <LockKeyhole className="w-3.5 h-3.5" />
                </div>
                <span>Bank-grade OTP login &amp; cloud data protection</span>
              </div>
            </div>
          </div>

          {/* Mini Live Status Card */}
          <div className="mt-8 pt-6 border-t border-white/15 relative z-10 hidden sm:block">
            <div className="p-4 bg-white/10 backdrop-blur-md rounded-2xl border border-white/20 text-xs flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse" />
                <span className="font-bold text-white">Live Ledger Sync Active</span>
              </div>
              <span className="text-[11px] text-indigo-200">Encrypted</span>
            </div>
          </div>
        </div>

        {/* RIGHT COLUMN: Interactive Form */}
        <div className="lg:col-span-6 p-8 sm:p-12 flex flex-col justify-between bg-white">
          <div className="space-y-6">
            <div>
              <h3 className="font-display font-extrabold text-2xl text-slate-900">
                Login to CredLink
              </h3>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Access your digital bahi-khata account
              </p>
            </div>

            {sessionExpired && (
              <div className="p-4 bg-amber-50 border border-amber-200 text-amber-900 text-xs rounded-2xl flex items-center gap-3">
                <AlertCircle className="w-5 h-5 text-amber-600 shrink-0" />
                <span>Your session expired. Please enter your email to log in again.</span>
              </div>
            )}

            {error && (
              <div className="p-4 bg-rose-50 border border-rose-200 text-rose-900 text-xs rounded-2xl flex items-center gap-3 animate-fadeIn">
                <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {isSuccess && (
              <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-900 text-xs rounded-2xl flex items-center gap-3 animate-fadeIn">
                <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                <span>Verification code sent! Redirecting to OTP page...</span>
              </div>
            )}

            {/* Mode Switcher Tabs */}
            <div className="grid grid-cols-2 gap-1 p-1.5 bg-slate-100/80 rounded-2xl text-xs font-bold border border-slate-200/60">
              <button
                type="button"
                onClick={() => {
                  setMode('otp')
                  setError('')
                }}
                className={`py-2.5 px-3 rounded-xl transition-all duration-200 ${
                  mode === 'otp'
                    ? 'bg-white text-indigo-700 shadow-md shadow-slate-200/80'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
              >
                OTP Login
              </button>
              <button
                type="button"
                onClick={() => {
                  setMode('password')
                  setError('')
                }}
                className={`py-2.5 px-3 rounded-xl transition-all duration-200 ${
                  mode === 'password'
                    ? 'bg-white text-indigo-700 shadow-md shadow-slate-200/80'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
              >
                Password Login
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Email Address / Merchant ID <span className="text-rose-500">*</span>
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5 transition-colors" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="you@store.com"
                    className="input-field pl-10 pr-10 py-3"
                  />
                  {isValidEmail && (
                    <CheckCircle2 className="w-4 h-4 text-emerald-500 absolute right-3.5 top-3.5" />
                  )}
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Store Name (First time register)
                </label>
                <div className="relative">
                  <Store className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                  <input
                    type="text"
                    value={storeName}
                    onChange={(e) => setStoreName(e.target.value)}
                    placeholder="Enter your store name"
                    className="input-field pl-10 py-3"
                  />
                </div>
              </div>

              {mode === 'password' && (
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="block text-xs font-semibold text-slate-700">Password</label>
                    <Link to="/auth/forgot-password" className="text-xs font-bold text-indigo-600 hover:underline">
                      Forgot Password?
                    </Link>
                  </div>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3.5" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="At least 8 characters"
                      className="input-field pl-10 pr-10 py-3"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3.5 top-3.5 text-slate-400 hover:text-slate-600 transition"
                    >
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>
              )}

              <div className="flex items-center justify-between text-xs pt-1">
                <label className="flex items-center gap-2 cursor-pointer text-slate-600 font-medium select-none">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 w-4 h-4"
                  />
                  Remember me on this device
                </label>
              </div>

              <button
                type="submit"
                disabled={loading || isSuccess}
                className="btn-primary w-full py-3.5 text-base font-semibold shadow-lg shadow-indigo-600/25 hover:shadow-indigo-600/35 hover:-translate-y-0.5 active:translate-y-0 transition-all duration-200"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Processing Login...
                  </>
                ) : (
                  <>
                    {mode === 'otp' ? 'Send Login OTP' : 'Login'}
                    <ArrowRight className="w-5 h-5" />
                  </>
                )}
              </button>
            </form>
          </div>

          <div className="pt-6 border-t border-slate-100 text-center text-xs text-slate-500">
            Don't have an account?{' '}
            <Link to="/auth/register" className="font-extrabold text-indigo-600 hover:underline">
              Create Account
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
