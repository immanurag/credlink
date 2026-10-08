import React, { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ShieldCheck,
  Users,
  Receipt,
  Wallet,
  CheckCircle2,
  Mic,
  ArrowRight,
  Sparkles,
  Lock,
  TrendingUp,
  Store,
  ChevronRight,
  Check,
  ArrowUpRight,
  ArrowDownLeft,
  Menu,
  X
} from 'lucide-react'

export default function Landing() {
  const [scrolled, setScrolled] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20)
    }
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 font-body selection:bg-indigo-100 selection:text-indigo-900">
      {/* 1. NAVBAR */}
      <header
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          scrolled
            ? 'bg-white/90 backdrop-blur-md border-b border-slate-200/80 shadow-sm py-3'
            : 'bg-transparent py-5'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">
          {/* Logo & Name */}
          <Link to="/" className="flex items-center gap-3 group">
            <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white font-display font-extrabold text-xl flex items-center justify-center shadow-md shadow-indigo-600/20 group-hover:bg-indigo-700 transition">
              CL
            </div>
            <div>
              <span className="font-display font-extrabold text-xl text-slate-900 tracking-tight flex items-center gap-1.5">
                CredLink
                <ShieldCheck className="w-4 h-4 text-indigo-600" />
              </span>
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest block -mt-1">
                Digital Bahi-Khata
              </span>
            </div>
          </Link>

          {/* Desktop Nav Links */}
          <nav className="hidden md:flex items-center gap-8 text-sm font-medium text-slate-600">
            <a href="#home" className="hover:text-indigo-600 transition">
              Home
            </a>
            <a href="#features" className="hover:text-indigo-600 transition">
              Features
            </a>
            <a href="#how-it-works" className="hover:text-indigo-600 transition">
              How It Works
            </a>
            <a href="#voice" className="hover:text-indigo-600 transition">
              Voice AI
            </a>
            <a href="#security" className="hover:text-indigo-600 transition">
              Security
            </a>
          </nav>

          {/* Right Action Buttons */}
          <div className="hidden md:flex items-center gap-3">
            <Link to="/auth/login" className="btn-secondary text-xs px-4 py-2">
              Login
            </Link>
            <Link to="/auth/login" className="btn-primary text-xs px-4 py-2 font-semibold">
              Get Started
            </Link>
          </div>

          {/* Mobile Menu Trigger */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 text-slate-600 hover:text-slate-900 rounded-lg"
          >
            {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {/* Mobile Dropdown Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden bg-white border-b border-slate-200 p-4 space-y-3 animate-fadeIn shadow-xl">
            <a
              href="#home"
              onClick={() => setMobileMenuOpen(false)}
              className="block py-2 text-sm font-semibold text-slate-700 hover:text-indigo-600"
            >
              Home
            </a>
            <a
              href="#features"
              onClick={() => setMobileMenuOpen(false)}
              className="block py-2 text-sm font-semibold text-slate-700 hover:text-indigo-600"
            >
              Features
            </a>
            <a
              href="#how-it-works"
              onClick={() => setMobileMenuOpen(false)}
              className="block py-2 text-sm font-semibold text-slate-700 hover:text-indigo-600"
            >
              How It Works
            </a>
            <a
              href="#voice"
              onClick={() => setMobileMenuOpen(false)}
              className="block py-2 text-sm font-semibold text-slate-700 hover:text-indigo-600"
            >
              Voice AI
            </a>
            <div className="pt-3 border-t border-slate-100 flex gap-2">
              <Link to="/auth/login" className="btn-secondary text-xs flex-1 justify-center">
                Login
              </Link>
              <Link to="/auth/login" className="btn-primary text-xs flex-1 justify-center font-semibold">
                Get Started
              </Link>
            </div>
          </div>
        )}
      </header>

      {/* 2. HERO SECTION */}
      <section id="home" className="pt-32 pb-20 md:pt-40 md:pb-28 overflow-hidden">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid lg:grid-cols-12 gap-12 items-center">
            {/* Left Hero Column */}
            <div className="lg:col-span-6 space-y-6 text-center lg:text-left">
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-indigo-50 border border-indigo-100 text-indigo-700 text-xs font-semibold">
                <Sparkles className="w-4 h-4 text-indigo-600" />
                Modern Digital Ledger System
              </div>

              <h1 className="font-display font-extrabold text-4xl sm:text-5xl lg:text-6xl text-slate-900 tracking-tight leading-[1.15]">
                Manage Your Business Ledger, <span className="text-indigo-600">Smarter.</span>
              </h1>

              <p className="text-base sm:text-lg text-slate-600 leading-relaxed max-w-xl mx-auto lg:mx-0">
                CredLink helps you manage customers, credit, payments and outstanding balances — all in one simple, secure digital ledger tailored for Indian shopkeepers.
              </p>

              <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-3 pt-2">
                <Link to="/auth/login" className="btn-primary py-3.5 px-7 text-base font-semibold w-full sm:w-auto shadow-lg shadow-indigo-600/20">
                  Get Started Free
                  <ArrowRight className="w-5 h-5" />
                </Link>
                <Link to="/auth/login" className="btn-secondary py-3.5 px-6 text-base font-semibold w-full sm:w-auto">
                  Merchant Login
                </Link>
              </div>

              {/* Trust Badge Indicators */}
              <div className="pt-4 flex items-center justify-center lg:justify-start gap-6 text-xs text-slate-500 font-semibold">
                <span className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" /> Simple
                </span>
                <span className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" /> 100% Secure
                </span>
                <span className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" /> Easy to Use
                </span>
              </div>
            </div>

            {/* Right Hero Visual Mockup */}
            <div className="lg:col-span-6 relative">
              <div className="absolute -inset-4 bg-gradient-to-r from-indigo-500/10 to-emerald-500/10 rounded-3xl blur-2xl pointer-events-none" />

              {/* Realistic Dashboard Mockup Card */}
              <div className="relative bg-white border border-slate-200/90 rounded-2xl shadow-2xl p-6 space-y-5 transform hover:-translate-y-1 transition duration-300">
                {/* Mockup Header */}
                <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                  <div className="flex items-center gap-2.5">
                    <div className="w-3 h-3 rounded-full bg-rose-400" />
                    <div className="w-3 h-3 rounded-full bg-amber-400" />
                    <div className="w-3 h-3 rounded-full bg-emerald-400" />
                    <span className="text-xs font-bold text-slate-400 ml-2">CredLink Store Command</span>
                  </div>
                  <span className="text-[10px] bg-emerald-50 text-emerald-700 border border-emerald-200 font-bold px-2 py-0.5 rounded-full">
                    Live Sync
                  </span>
                </div>

                {/* Mock Metric Grid */}
                <div className="grid grid-cols-3 gap-3">
                  <div className="p-3 bg-amber-50/70 border border-amber-200/80 rounded-xl">
                    <div className="text-[10px] font-bold text-amber-700 uppercase">Outstanding</div>
                    <div className="text-base font-display font-extrabold text-amber-600 mt-0.5">₹24,500</div>
                  </div>
                  <div className="p-3 bg-emerald-50/70 border border-emerald-200/80 rounded-xl">
                    <div className="text-[10px] font-bold text-emerald-700 uppercase">Total Paid</div>
                    <div className="text-base font-display font-extrabold text-emerald-600 mt-0.5">₹18,750</div>
                  </div>
                  <div className="p-3 bg-indigo-50/70 border border-indigo-200/80 rounded-xl">
                    <div className="text-[10px] font-bold text-indigo-700 uppercase">Customers</div>
                    <div className="text-base font-display font-extrabold text-indigo-700 mt-0.5">128 Parties</div>
                  </div>
                </div>

                {/* Mock Ledger Table */}
                <div className="space-y-2 border border-slate-100 rounded-xl p-3 bg-slate-50/50">
                  <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-1">Recent Khata Activity</div>
                  
                  <div className="flex items-center justify-between p-2.5 bg-white rounded-lg border border-slate-100 shadow-sm">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-amber-100 text-amber-700 font-bold text-xs flex items-center justify-center">
                        C
                      </div>
                      <div>
                        <div className="text-xs font-bold text-slate-900">Customer Credit Entry</div>
                        <div className="text-[10px] text-slate-400">Goods Purchase</div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs font-extrabold text-amber-600">+Credit</div>
                      <div className="text-[9px] text-amber-700 font-semibold bg-amber-50 px-1.5 py-0.2 rounded">Udhaar</div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between p-2.5 bg-white rounded-lg border border-slate-100 shadow-sm">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-700 font-bold text-xs flex items-center justify-center">
                        P
                      </div>
                      <div>
                        <div className="text-xs font-bold text-slate-900">Payment Collection</div>
                        <div className="text-[10px] text-slate-400">UPI / Cash Received</div>
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-xs font-extrabold text-emerald-600">-Payment</div>
                      <div className="text-[9px] text-emerald-700 font-semibold bg-emerald-50 px-1.5 py-0.2 rounded">Jama</div>
                    </div>
                  </div>
                </div>

                {/* Floating Voice Indicator */}
                <div className="p-3 bg-indigo-600 text-white rounded-xl flex items-center justify-between shadow-lg">
                  <div className="flex items-center gap-2.5">
                    <Mic className="w-4 h-4 text-white animate-pulse" />
                    <span className="text-xs font-semibold">"Speak credit or payment entry..."</span>
                  </div>
                  <span className="text-[10px] bg-white/20 text-white font-bold px-2 py-0.5 rounded">Voice AI</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 3. FEATURES SECTION */}
      <section id="features" className="py-20 bg-white border-y border-slate-200/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          <div className="text-center max-w-3xl mx-auto space-y-3">
            <h2 className="font-display font-extrabold text-3xl sm:text-4xl text-slate-900">
              Everything You Need to Manage Your Ledger
            </h2>
            <p className="text-sm sm:text-base text-slate-500">
              Purpose-built tools designed specifically for shopkeepers to eliminate paper registers and lost collections.
            </p>
          </div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {/* Feature 1 */}
            <div className="card card-interactive p-6 space-y-3">
              <div className="w-12 h-12 rounded-xl bg-indigo-50 border border-indigo-100 text-indigo-600 flex items-center justify-center">
                <Users className="w-6 h-6" />
              </div>
              <h3 className="font-display font-bold text-lg text-slate-900">Customer Management</h3>
              <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
                Keep all your customer contact details, phone numbers, and categories organized cleanly in one place.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="card card-interactive p-6 space-y-3">
              <div className="w-12 h-12 rounded-xl bg-indigo-50 border border-indigo-100 text-indigo-600 flex items-center justify-center">
                <Receipt className="w-6 h-6" />
              </div>
              <h3 className="font-display font-bold text-lg text-slate-900">Digital Ledger</h3>
              <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
                Record credit and payment transactions without maintaining a cumbersome physical paper notebook.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="card card-interactive p-6 space-y-3">
              <div className="w-12 h-12 rounded-xl bg-amber-50 border border-amber-100 text-amber-600 flex items-center justify-center">
                <Wallet className="w-6 h-6" />
              </div>
              <h3 className="font-display font-bold text-lg text-slate-900">Outstanding Tracking</h3>
              <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
                Know exactly who owes you money and how much is pending at any moment with real-time balance calculations.
              </p>
            </div>

            {/* Feature 4 */}
            <div className="card card-interactive p-6 space-y-3">
              <div className="w-12 h-12 rounded-xl bg-emerald-50 border border-emerald-100 text-emerald-600 flex items-center justify-center">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <h3 className="font-display font-bold text-lg text-slate-900">Payment Tracking</h3>
              <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
                Log cash, UPI, or cheque payments and automatically keep customer balances updated instantly.
              </p>
            </div>

            {/* Feature 5 */}
            <div className="card card-interactive p-6 space-y-3">
              <div className="w-12 h-12 rounded-xl bg-indigo-50 border border-indigo-100 text-indigo-600 flex items-center justify-center">
                <Mic className="w-6 h-6" />
              </div>
              <h3 className="font-display font-bold text-lg text-slate-900">Voice Transactions</h3>
              <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
                Add and find transactions using natural Hindi or Hinglish voice commands powered by Sarvam AI.
              </p>
            </div>

            {/* Feature 6 */}
            <div className="card card-interactive p-6 space-y-3">
              <div className="w-12 h-12 rounded-xl bg-indigo-50 border border-indigo-100 text-indigo-600 flex items-center justify-center">
                <Lock className="w-6 h-6" />
              </div>
              <h3 className="font-display font-bold text-lg text-slate-900">Secure Access</h3>
              <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
                Keep your business data protected with secure OTP authentication and encrypted cloud backups.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 4. HOW IT WORKS */}
      <section id="how-it-works" className="py-20 bg-slate-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          <div className="text-center max-w-3xl mx-auto space-y-3">
            <h2 className="font-display font-extrabold text-3xl sm:text-4xl text-slate-900">
              How CredLink Works in 3 Simple Steps
            </h2>
            <p className="text-sm sm:text-base text-slate-500">
              Get your shop ledger up and running in under 2 minutes.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-8 relative">
            {/* Step 1 */}
            <div className="card p-6 space-y-4 text-center relative">
              <div className="w-10 h-10 rounded-full bg-indigo-600 text-white font-bold text-lg flex items-center justify-center mx-auto shadow-md">
                1
              </div>
              <h3 className="font-display font-bold text-xl text-slate-900">Add Customer</h3>
              <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
                Create a customer profile in seconds with just their name and phone number.
              </p>
            </div>

            {/* Step 2 */}
            <div className="card p-6 space-y-4 text-center relative">
              <div className="w-10 h-10 rounded-full bg-indigo-600 text-white font-bold text-lg flex items-center justify-center mx-auto shadow-md">
                2
              </div>
              <h3 className="font-display font-bold text-xl text-slate-900">Record Transactions</h3>
              <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
                Add credit, purchases and payments easily using simple forms or voice commands.
              </p>
            </div>

            {/* Step 3 */}
            <div className="card p-6 space-y-4 text-center relative">
              <div className="w-10 h-10 rounded-full bg-indigo-600 text-white font-bold text-lg flex items-center justify-center mx-auto shadow-md">
                3
              </div>
              <h3 className="font-display font-bold text-xl text-slate-900">Track Outstanding</h3>
              <p className="text-xs sm:text-sm text-slate-500 leading-relaxed">
                Always know your current balance, pending payments, and send automatic WhatsApp notices.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 5. VOICE FEATURE SECTION */}
      <section id="voice" className="py-20 bg-gradient-to-b from-indigo-900 to-slate-900 text-white relative overflow-hidden">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-10 relative z-10">
          <div className="text-center max-w-3xl mx-auto space-y-3">
            <span className="text-xs font-bold uppercase tracking-widest text-indigo-300 bg-white/10 px-3 py-1 rounded-full border border-white/10">
              Hindi &amp; Hinglish Voice AI
            </span>
            <h2 className="font-display font-extrabold text-3xl sm:text-4xl text-white">
              Your Ledger, Just One Voice Command Away.
            </h2>
            <p className="text-sm sm:text-base text-indigo-200">
              Speak naturally in Hindi or Hinglish and manage your ledger faster without typing.
            </p>
          </div>

          <div className="max-w-xl mx-auto bg-white/10 backdrop-blur-md border border-white/20 p-6 rounded-2xl space-y-6 shadow-2xl">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-full bg-indigo-600 text-white flex items-center justify-center shrink-0 animate-pulse">
                <Mic className="w-6 h-6" />
              </div>
              <div>
                <div className="text-xs text-indigo-300 font-semibold">Voice Dictation Pattern:</div>
                <div className="text-base font-bold text-white italic">"[Customer Name] ko [Amount] rupaye udhaar diya"</div>
              </div>
            </div>

            <div className="p-4 bg-white text-slate-900 rounded-xl space-y-3 shadow-lg">
              <div className="text-xs font-bold text-indigo-600 uppercase tracking-wider">CredLink AI Confirmation Box</div>
              <div className="font-display font-extrabold text-lg text-slate-900">
                Add ₹500 credit to Customer?
              </div>
              <div className="flex items-center justify-end gap-2 pt-1">
                <button className="btn-secondary text-xs py-1.5 px-3">Cancel</button>
                <button className="btn-primary text-xs py-1.5 px-4 font-semibold">Confirm &amp; Save</button>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 6. TRUST / SECURITY SECTION */}
      <section id="security" className="py-20 bg-white border-t border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          <div className="text-center max-w-3xl mx-auto space-y-3">
            <h2 className="font-display font-extrabold text-3xl sm:text-4xl text-slate-900">
              Built for Your Business
            </h2>
            <p className="text-sm sm:text-base text-slate-500">
              Reliable infrastructure ensuring your ledger is always accessible, safe, and synced.
            </p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="card p-5 text-center space-y-2">
              <ShieldCheck className="w-8 h-8 text-indigo-600 mx-auto" />
              <h3 className="font-display font-bold text-slate-900">Secure Login</h3>
              <p className="text-xs text-slate-500">Protected OTP authentication prevents unauthorized access.</p>
            </div>
            <div className="card p-5 text-center space-y-2">
              <Lock className="w-8 h-8 text-indigo-600 mx-auto" />
              <h3 className="font-display font-bold text-slate-900">Protected Data</h3>
              <p className="text-xs text-slate-500">Encrypted cloud database keeps customer records safe.</p>
            </div>
            <div className="card p-5 text-center space-y-2">
              <Receipt className="w-8 h-8 text-indigo-600 mx-auto" />
              <h3 className="font-display font-bold text-slate-900">Reliable Transactions</h3>
              <p className="text-xs text-slate-500">Every credit and payment entry generates immutable balance records.</p>
            </div>
            <div className="card p-5 text-center space-y-2">
              <Store className="w-8 h-8 text-indigo-600 mx-auto" />
              <h3 className="font-display font-bold text-slate-900">Easy Access</h3>
              <p className="text-xs text-slate-500">Access your ledger anytime from desktop, tablet, or phone.</p>
            </div>
          </div>
        </div>
      </section>

      {/* 7. CTA SECTION */}
      <section className="py-20 bg-indigo-600 text-white text-center">
        <div className="max-w-4xl mx-auto px-4 space-y-6">
          <h2 className="font-display font-extrabold text-3xl sm:text-5xl text-white">
            Ready to Simplify Your Business Ledger?
          </h2>
          <p className="text-base sm:text-lg text-indigo-100 max-w-xl mx-auto">
            Start managing your customers, payments and outstanding balances digitally today.
          </p>
          <div>
            <Link to="/auth/login" className="inline-flex items-center gap-2 bg-white text-indigo-900 font-display font-bold text-base px-8 py-4 rounded-xl shadow-xl hover:bg-slate-50 transition">
              Get Started Free
              <ArrowRight className="w-5 h-5 text-indigo-600" />
            </Link>
          </div>
        </div>
      </section>

      {/* 8. FOOTER */}
      <footer className="bg-slate-900 text-slate-400 py-12 border-t border-slate-800 text-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-indigo-600 text-white font-display font-bold flex items-center justify-center">
              CL
            </div>
            <span className="font-display font-bold text-white text-base">CredLink</span>
          </div>

          <div className="flex items-center gap-6 font-medium">
            <a href="#home" className="hover:text-white transition">
              Home
            </a>
            <a href="#features" className="hover:text-white transition">
              Features
            </a>
            <a href="#how-it-works" className="hover:text-white transition">
              How It Works
            </a>
            <Link to="/auth/login" className="hover:text-white transition">
              Login
            </Link>
          </div>

          <div>&copy; 2026 CredLink. All rights reserved.</div>
        </div>
      </footer>
    </div>
  )
}
