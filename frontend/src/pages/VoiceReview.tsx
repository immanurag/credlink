import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAudioRecorder } from '../hooks/useAudioRecorder'
import { voiceService, VoiceConfig } from '../services/voice'
import { nlpService } from '../services/nlp'
import { customerService } from '../services/customer'
import { transactionService } from '../services/transaction'
import VoiceHUD from '../components/VoiceHUD'
import { formatCurrency } from '../components/formatters'
import { extractApiError } from '../services/api'
import { useToast } from '../context/ToastContext'
import type { Customer, Transaction, TransactionType, VoiceState } from '../types'
import {
  Mic,
  MicOff,
  Square,
  Search,
  UserCheck,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  ArrowRight,
  RefreshCw,
  Edit3,
  XCircle,
  HelpCircle,
  ShieldCheck,
  Volume2
} from 'lucide-react'

type StepStage = 'SEARCH_CUSTOMER' | 'SELECT_CUSTOMER' | 'RECORD_STATEMENT' | 'REVIEW_TRANSACTION' | 'CONFIRMED'

export default function VoiceReview() {
  const audioRecorder = useAudioRecorder()
  const toast = useToast()
  const navigate = useNavigate()

  const [voiceConfig, setVoiceConfig] = useState<VoiceConfig | null>(null)
  const [transcript, setTranscript] = useState('')
  const [lang, setLang] = useState<'hi-IN' | 'en-IN'>('hi-IN')

  const [state, setState] = useState<VoiceState>('IDLE')
  const [activeStep, setActiveStep] = useState<StepStage>('SEARCH_CUSTOMER')
  const [activeMicStep, setActiveMicStep] = useState<'CUSTOMER_SEARCH' | 'STATEMENT' | null>(null)
  const [errorMsg, setErrorMsg] = useState('')
  const [confidence, setConfidence] = useState(0)

  // Customer search & selection
  const [customerSearchQuery, setCustomerSearchQuery] = useState('')
  const [searchMatches, setSearchMatches] = useState<Customer[]>([])
  const [allCustomers, setAllCustomers] = useState<Customer[]>([])
  const [hasSearched, setHasSearched] = useState(false)
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null)

  // Statement & transaction details
  const [statementText, setStatementText] = useState('')
  const [editType, setEditType] = useState<TransactionType>('UDHAAR')
  const [editAmount, setEditAmount] = useState('')
  const [extractedOutstanding, setExtractedOutstanding] = useState<number | null>(null)
  const [preview, setPreview] = useState<Transaction | null>(null)
  const [queryResult, setQueryResult] = useState<NlpExtractionResult | null>(null)

  // Save/Confirm loading & confirmed data
  const [isSaving, setIsSaving] = useState(false)
  const [confirmedTransaction, setConfirmedTransaction] = useState<{
    customerName: string
    description?: string
    amount: number
    balanceAfter: number
  } | null>(null)

  useEffect(() => {
    voiceService.getConfig().then(setVoiceConfig).catch(() => {})
    customerService.list().then(setAllCustomers).catch(() => {})
  }, [])

  // Start Audio Recording for Sarvam AI STT
  const handleStartMic = (micStep: 'CUSTOMER_SEARCH' | 'STATEMENT') => {
    if (micStep === 'STATEMENT' && !selectedCustomer) {
      toast.warning('Please select and confirm a customer first.')
      setErrorMsg('Please select a customer before recording the transaction statement.')
      return
    }

    if (!audioRecorder.supported) {
      toast.error('Microphone recording is not supported by your browser.')
      setErrorMsg('Microphone recording is not supported by your browser.')
      return
    }

    setActiveMicStep(micStep)
    setState('LISTENING')
    setErrorMsg('')
    setTranscript('')
    audioRecorder.startRecording()
  }

  // Stop Recording & Send Audio to Sarvam AI STT
  const handleStopMicAndTranscribe = async () => {
    if (!activeMicStep) return
    const currentStep = activeMicStep

    setState('TRANSCRIBING')
    const blob = await audioRecorder.stopRecording()
    if (!blob || blob.size === 0) {
      toast.error('No speech detected. Please try recording again.')
      setErrorMsg('No audio detected. Please try recording again.')
      setState('FAILED')
      setActiveMicStep(null)
      return
    }

    try {
      const result = await voiceService.transcribeAudio(blob, lang)
      const text = result.transcript
      setTranscript(text)
      handleSpeechCompleted(currentStep, text)
    } catch (err: any) {
      const errorText = extractApiError(err)
      toast.error('Sarvam STT failed. Please try manual entry.')
      setErrorMsg(`Sarvam STT Error: ${errorText}`)
      setState('FAILED')
      setActiveMicStep(null)
    }
  }

  const cleanSearchQuery = (raw: string): string => {
    if (!raw) return ''
    let cleaned = raw.replace(/[.,?!'"]/g, '').trim()
    cleaned = cleaned.replace(/\b(?:find|search|show|check|get|tell|batao|dikhao|khojo|dhoondo|dhundho|balance|dues|udhaar|udhar|jama|history|statement|transaction|transactions|account|khata|details|kitna|kitne|kitni|paise|rupaye|rs|rupees|hai|hain|ho|karo|karna|dena|lene|dene)\b/gi, '')
    cleaned = cleaned.replace(/(?:से|को|का|की|ने|में|पर|के|लिए|खोजो|ढूंढो|ढूंढें|बताओ|दिखाओ|चेक|बैलेंस|हिस्ट्री|खाता|उधार|जमा|लेनदेन|डिटेल्स|कितना|कितने|कितनी|पैसे|रुपये|है|हैं|हो|करो|करना|देना|लेने|देने)/g, '')
    cleaned = cleaned.replace(/\s+/g, ' ').trim()
    return cleaned || raw
  }

  const handleSpeechCompleted = (micStep: 'CUSTOMER_SEARCH' | 'STATEMENT', text: string) => {
    setActiveMicStep(null)
    if (!text.trim()) {
      setErrorMsg('No clear speech was recognized by Sarvam AI. Please try recording again.')
      setState('FAILED')
      return
    }

    if (micStep === 'CUSTOMER_SEARCH') {
      const cleaned = cleanSearchQuery(text)
      setCustomerSearchQuery(cleaned || text)
      performCustomerSearch(cleaned || text, text)
    } else if (micStep === 'STATEMENT') {
      setStatementText(text)
      runStatementExtraction(text)
    }
  }

  const performCustomerSearch = async (query: string, rawText?: string) => {
    if (!query.trim()) return
    setState('EXTRACTING')
    setErrorMsg('')
    try {
      // Check if raw speech was actually a balance query
      if (rawText && rawText.trim()) {
        const nlpResult = await nlpService.extract(rawText.trim()).catch(() => null)
        if (nlpResult && nlpResult.intent === 'OUTSTANDING_BALANCE_QUERY') {
          setConfidence(nlpResult.confidence)
          setQueryResult(nlpResult)
          setPreview(null)
          setExtractedOutstanding(nlpResult.outstandingAmount ?? null)
          setActiveStep('REVIEW_TRANSACTION')
          setState('REVIEW')
          return
        }
      }

      const cleaned = cleanSearchQuery(query)
      let results = await customerService.list(cleaned)
      if (results.length === 0) {
        const fallbackList = await customerService.list()
        setAllCustomers(fallbackList)
      }
      setSearchMatches(results)
      setHasSearched(true)
      setActiveStep('SELECT_CUSTOMER')
      setState('IDLE')
    } catch (err) {
      setErrorMsg(extractApiError(err))
      setState('FAILED')
    }
  }

  const handleManualSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    performCustomerSearch(customerSearchQuery, customerSearchQuery)
  }

  const handleSelectCustomer = (customer: Customer) => {
    setSelectedCustomer(customer)
    setActiveStep('RECORD_STATEMENT')
    setErrorMsg('')
    toast.info(`Selected customer: ${customer.name}`)
  }

  const handleDeselectCustomer = () => {
    setSelectedCustomer(null)
    setPreview(null)
    setQueryResult(null)
    setExtractedOutstanding(null)
    setActiveStep('SELECT_CUSTOMER')
  }

  const runStatementExtraction = async (text: string) => {
    setState('TRANSCRIBING')
    setErrorMsg('')
    try {
      await new Promise((r) => setTimeout(r, 150))
      setState('EXTRACTING')
      const result = await nlpService.extract(text)
      setConfidence(result.confidence)

      if (result.intent === 'OUTSTANDING_BALANCE_QUERY') {
        setQueryResult(result)
        setPreview(null)
        setExtractedOutstanding(result.outstandingAmount ?? null)
        if (result.customerName && result.customerName !== 'Customer') {
          const match = allCustomers.find(
            (c) => c.name.toLowerCase() === result.customerName.toLowerCase()
          )
          if (match) setSelectedCustomer(match)
        }
        setActiveStep('REVIEW_TRANSACTION')
        setState('REVIEW')
        return
      }

      setQueryResult(null)
      if (!selectedCustomer) {
        setErrorMsg('Please select a customer first.')
        setState('FAILED')
        return
      }

      setEditType(result.type || 'UDHAAR')
      setEditAmount(String(result.amount || 0))
      setExtractedOutstanding(result.outstandingAmount ?? null)

      const englishDesc = result.englishDescription || ((result.type || 'UDHAAR') === 'UDHAAR'
        ? `Credit of ${result.amount} rupees given to ${selectedCustomer.name}.`
        : `Payment of ${result.amount} rupees received from ${selectedCustomer.name}.`)

      const p = await transactionService.preview({
        customerId: selectedCustomer.id,
        type: result.type || 'UDHAAR',
        amount: result.amount || 0,
        source: 'VOICE',
        voiceTranscript: text,
        confidenceScore: result.confidence,
        description: englishDesc,
        explicitOutstanding: result.outstandingAmount ?? undefined
      })
      setPreview(p)
      setActiveStep('REVIEW_TRANSACTION')
      setState('REVIEW')
    } catch (err) {
      setErrorMsg(extractApiError(err))
      setState('FAILED')
    }
  }

  const handleManualStatementSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (statementText.trim()) {
      runStatementExtraction(statementText.trim())
    }
  }

  const refreshPreviewForEdits = async () => {
    if (!selectedCustomer) return
    try {
      const defaultDesc = editType === 'UDHAAR'
        ? `Credit of ${editAmount} rupees given to ${selectedCustomer.name}.`
        : `Payment of ${editAmount} rupees received from ${selectedCustomer.name}.`

      const p = await transactionService.preview({
        customerId: selectedCustomer.id,
        type: editType,
        amount: Number(editAmount || 0),
        source: 'VOICE',
        voiceTranscript: statementText || customerSearchQuery,
        confidenceScore: confidence,
        description: defaultDesc,
        explicitOutstanding: extractedOutstanding ?? undefined
      })
      setPreview(p)
    } catch (err) {
      setErrorMsg(extractApiError(err))
    }
  }

  const onConfirm = async () => {
    if (!preview || isSaving) return
    setIsSaving(true)
    setErrorMsg('')

    try {
      const saved = await transactionService.confirm(preview.id, {
        type: editType,
        amount: Number(editAmount || 0),
        explicitOutstanding: extractedOutstanding ?? undefined
      })

      setConfirmedTransaction({
        customerName: selectedCustomer ? selectedCustomer.name : 'Customer',
        description: preview.description,
        amount: Number(editAmount || preview.amount),
        balanceAfter: saved.balanceAfter
      })

      setState('CONFIRMED')
      setActiveStep('CONFIRMED')
      toast.success(editType === 'UDHAAR' ? 'Transaction added successfully.' : 'Payment recorded successfully.')
    } catch (err) {
      const msg = extractApiError(err) || 'Unable to save transaction. Please try again.'
      setErrorMsg(msg)
      toast.error(msg)
    } finally {
      setIsSaving(false)
    }
  }

  const onDiscard = async () => {
    if (preview) await transactionService.discard(preview.id).catch(() => {})
    setPreview(null)
    setQueryResult(null)
    setExtractedOutstanding(null)
    setStatementText('')
    setActiveStep('RECORD_STATEMENT')
    setState('REVIEW')
  }

  const resetAll = () => {
    setState('IDLE')
    setActiveStep('SEARCH_CUSTOMER')
    setActiveMicStep(null)
    setTranscript('')
    setCustomerSearchQuery('')
    setSearchMatches([])
    setHasSearched(false)
    setSelectedCustomer(null)
    setStatementText('')
    setExtractedOutstanding(null)
    setPreview(null)
    setQueryResult(null)
    setConfirmedTransaction(null)
    setErrorMsg('')
    setIsSaving(false)
  }

  const isRecordingActive = audioRecorder.recording

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-200 pb-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
            Sarvam AI Speech Engine Active
          </div>
          <h1 className="font-display font-extrabold text-2xl md:text-3xl text-slate-900 mt-1">
            Hindi &amp; Hinglish Voice AI Ledger
          </h1>
          <p className="text-xs md:text-sm text-slate-500">
            Dictate transactions naturally in Hindi or English: Search Customer &rarr; Select Customer &rarr; Speak Statement &rarr; Confirm
          </p>
        </div>

        <button
          onClick={() => setLang((l) => (l === 'hi-IN' ? 'en-IN' : 'hi-IN'))}
          className="btn-secondary text-xs px-3 py-2 shrink-0"
        >
          <Volume2 className="w-4 h-4 text-indigo-600" />
          Language: <span className="font-bold text-slate-900">{lang === 'hi-IN' ? 'Hindi (hi-IN)' : 'English (en-IN)'}</span>
        </button>
      </div>

      {/* Voice Progress HUD */}
      <VoiceHUD state={state} />

      {/* Error Banner */}
      {errorMsg && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-900 text-xs rounded-xl flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-rose-600 shrink-0" />
            <span>{errorMsg}</span>
          </div>
          <button onClick={() => setErrorMsg('')} className="font-bold text-rose-700 hover:underline">
            Dismiss
          </button>
        </div>
      )}

      {/* STEP 1: Speak Customer Name */}
      <div className="card space-y-4 border-l-4 border-l-indigo-600">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-full bg-indigo-600 text-white font-bold text-xs flex items-center justify-center">
              1
            </div>
            <h2 className="font-display font-bold text-lg text-slate-900">
              Step 1: Speak Customer Name
            </h2>
          </div>
          {activeStep === 'SEARCH_CUSTOMER' && (
            <span className="text-[11px] bg-indigo-50 border border-indigo-100 text-indigo-700 font-bold px-2.5 py-0.5 rounded-full">
              ACTIVE STEP
            </span>
          )}
        </div>
        <p className="text-xs text-slate-500">
          Click mic and speak the customer's name to locate their khata.
        </p>

        {/* Large Mic Button & Search Bar */}
        <div className="flex flex-col sm:flex-row items-center gap-4 pt-2">
          {isRecordingActive && activeMicStep === 'CUSTOMER_SEARCH' ? (
            <button
              onClick={handleStopMicAndTranscribe}
              className="btn-danger w-full sm:w-auto px-6 py-3 text-sm font-semibold animate-pulse shadow-lg"
            >
              <Square className="w-5 h-5 fill-current" />
              Stop Recording &amp; Process
            </button>
          ) : (
            <button
              onClick={() => handleStartMic('CUSTOMER_SEARCH')}
              className="btn-primary w-full sm:w-auto px-6 py-3 text-sm font-semibold shadow-md"
            >
              <Mic className="w-5 h-5 animate-pulse" />
              Record Customer Name (Mic)
            </button>
          )}

          <form onSubmit={handleManualSearchSubmit} className="relative flex-1 w-full">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
            <input
              value={customerSearchQuery}
              onChange={(e) => setCustomerSearchQuery(e.target.value)}
              placeholder="Or type customer name manually..."
              className="input-field pl-10 pr-20"
            />
            <button
              type="submit"
              className="absolute right-1.5 top-1.5 bottom-1.5 px-3 bg-slate-200 hover:bg-slate-300 text-slate-700 font-semibold text-xs rounded-md transition"
            >
              Search
            </button>
          </form>
        </div>

        {/* Visual Recording Waves State */}
        {isRecordingActive && activeMicStep === 'CUSTOMER_SEARCH' && (
          <div className="p-4 bg-indigo-50 border border-indigo-200 rounded-xl text-center space-y-2">
            <div className="flex items-center justify-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-rose-600 animate-ping" />
              <span className="text-xs font-bold text-indigo-900">Listening to customer name...</span>
            </div>
            <p className="text-[11px] text-indigo-700">Speak clearly into microphone, then click Stop.</p>
          </div>
        )}

        {state === 'TRANSCRIBING' && activeMicStep === 'CUSTOMER_SEARCH' && (
          <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl text-center">
            <div className="text-xs font-semibold text-slate-700 animate-pulse">
              ⚙️ Transcribing speech with Sarvam AI STT...
            </div>
          </div>
        )}
      </div>

      {/* STEP 2: Customer Selection */}
      <div className="card space-y-4 border-l-4 border-l-slate-700">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-full bg-slate-700 text-white font-bold text-xs flex items-center justify-center">
              2
            </div>
            <h2 className="font-display font-bold text-lg text-slate-900">
              Step 2: Confirm / Select Customer
            </h2>
          </div>
          {activeStep === 'SELECT_CUSTOMER' && (
            <span className="text-[11px] bg-slate-100 text-slate-700 font-bold px-2.5 py-0.5 rounded-full">
              ACTIVE STEP
            </span>
          )}
        </div>

        {!selectedCustomer ? (
          <div className="space-y-4">
            {customerSearchQuery && (
              <div className="p-3.5 bg-indigo-50 border border-indigo-200 rounded-xl flex items-center justify-between gap-3 text-xs">
                <div>
                  <span className="text-slate-600">Transcribed Name: </span>
                  <span className="font-extrabold text-indigo-900 text-sm">"{customerSearchQuery}"</span>
                </div>
                {hasSearched && searchMatches.length === 1 && (
                  <button
                    onClick={() => handleSelectCustomer(searchMatches[0])}
                    className="btn-primary text-xs py-1.5 px-3"
                  >
                    Confirm {searchMatches[0].name} &rarr;
                  </button>
                )}
              </div>
            )}

            {hasSearched && searchMatches.length === 0 ? (
              <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-3">
                <div className="text-xs font-bold text-rose-600">
                  No party found matching "{customerSearchQuery}".
                </div>
                <div className="text-xs text-slate-600">
                  Please select from your existing customer list below or add a new customer:
                </div>

                {allCustomers.length > 0 && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2 pt-1">
                    {allCustomers.map((c) => (
                      <div
                        key={c.id}
                        onClick={() => handleSelectCustomer(c)}
                        className="p-3 border border-slate-200 rounded-xl bg-white hover:border-indigo-400 cursor-pointer transition flex items-center justify-between"
                      >
                        <div>
                          <div className="font-bold text-sm text-slate-900">{c.name}</div>
                          <div className="text-[11px] text-slate-400">{c.phone || 'No phone'}</div>
                        </div>
                        <button className="btn-secondary text-[10px] py-1 px-2">Select</button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ) : (searchMatches.length > 0 ? searchMatches : allCustomers).length > 0 ? (
              <div className="space-y-2">
                <div className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                  {searchMatches.length > 0 ? `Matching Parties (${searchMatches.length}):` : 'Select Party from Khata:'}
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                  {(searchMatches.length > 0 ? searchMatches : allCustomers).map((c) => (
                    <div
                      key={c.id}
                      onClick={() => handleSelectCustomer(c)}
                      className="p-3.5 border border-slate-200 rounded-xl bg-white hover:border-indigo-500 hover:shadow-md cursor-pointer transition flex items-center justify-between group"
                    >
                      <div>
                        <div className="font-bold text-sm text-slate-900 group-hover:text-indigo-600 transition">
                          {c.name}
                        </div>
                        <div className="text-xs text-slate-400">{c.phone || 'No phone'}</div>
                      </div>
                      <div className="text-right">
                        <div className={`text-xs font-bold ${c.currentBalance > 0 ? 'text-amber-600' : 'text-emerald-600'}`}>
                          {formatCurrency(c.currentBalance)}
                        </div>
                        <span className="text-[10px] bg-slate-100 text-slate-700 px-2 py-0.5 rounded font-semibold group-hover:bg-indigo-600 group-hover:text-white transition mt-1 inline-block">
                          Confirm &rarr;
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ) : (
              <div className="text-xs text-slate-400 italic p-4 bg-slate-50 rounded-xl text-center">
                Speak or type a customer name in Step 1 to display options here.
              </div>
            )}
          </div>
        ) : (
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-emerald-600 text-white font-bold flex items-center justify-center">
                <UserCheck className="w-5 h-5" />
              </div>
              <div>
                <div className="text-[10px] font-bold text-emerald-700 uppercase tracking-wider">CONFIRMED CUSTOMER</div>
                <div className="font-display font-extrabold text-lg text-emerald-900">{selectedCustomer.name}</div>
                <div className="text-xs text-emerald-700">
                  Current Khata Balance: {formatCurrency(selectedCustomer.currentBalance)}
                </div>
              </div>
            </div>
            <button onClick={handleDeselectCustomer} className="btn-secondary text-xs">
              Change Party
            </button>
          </div>
        )}
      </div>

      {/* STEP 3: Voice Statement */}
      <div className="card space-y-4 border-l-4 border-l-amber-500">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-full bg-amber-500 text-white font-bold text-xs flex items-center justify-center">
              3
            </div>
            <h2 className="font-display font-bold text-lg text-slate-900">
              Step 3: Voice Transaction Statement
            </h2>
          </div>
          {activeStep === 'RECORD_STATEMENT' && (
            <span className="text-[11px] bg-amber-50 text-amber-700 font-bold px-2.5 py-0.5 rounded-full">
              ACTIVE STEP
            </span>
          )}
        </div>
        <p className="text-xs text-slate-500">
          Dictate transaction amount and type (e.g. <em>"500 rupaye udhaar"</em> or <em>"500 rupaye jama, 200 rupaye baki"</em>).
        </p>

        {!selectedCustomer ? (
          <div className="text-xs text-slate-400 italic p-4 bg-slate-50 rounded-xl text-center">
            Please complete Step 2 (Select Customer) to record statement.
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex flex-col sm:flex-row items-center gap-4">
              {isRecordingActive && activeMicStep === 'STATEMENT' ? (
                <button
                  onClick={handleStopMicAndTranscribe}
                  className="btn-danger w-full sm:w-auto px-6 py-3 text-sm font-semibold animate-pulse shadow-lg"
                >
                  <Square className="w-5 h-5 fill-current" />
                  Stop Recording &amp; Extract
                </button>
              ) : (
                <button
                  onClick={() => handleStartMic('STATEMENT')}
                  className="btn-primary bg-amber-600 hover:bg-amber-700 w-full sm:w-auto px-6 py-3 text-sm font-semibold shadow-md"
                >
                  <Mic className="w-5 h-5 animate-pulse" />
                  Record Statement (Mic)
                </button>
              )}

              <form onSubmit={handleManualStatementSubmit} className="relative flex-1 w-full">
                <input
                  value={statementText}
                  onChange={(e) => setStatementText(e.target.value)}
                  placeholder="Or type statement manually (e.g. 500 rupaye jama, 200 rupaye baki)..."
                  className="input-field pr-20"
                />
                <button
                  type="submit"
                  className="absolute right-1.5 top-1.5 bottom-1.5 px-3 bg-slate-200 hover:bg-slate-300 text-slate-700 font-semibold text-xs rounded-md transition"
                >
                  Extract
                </button>
              </form>
            </div>

            {isRecordingActive && activeMicStep === 'STATEMENT' && (
              <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl text-center space-y-2">
                <div className="flex items-center justify-center gap-1.5">
                  <span className="w-2.5 h-2.5 rounded-full bg-amber-600 animate-ping" />
                  <span className="text-xs font-bold text-amber-900">Recording statement audio...</span>
                </div>
                <p className="text-[11px] text-amber-700">Speak transaction amount and type clearly.</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* READ-ONLY KHATA BALANCE INQUIRY RESPONSE CARD */}
      {queryResult && queryResult.intent === 'OUTSTANDING_BALANCE_QUERY' && state !== 'CONFIRMED' && (
        <div className="card space-y-6 border-2 border-indigo-600 bg-white shadow-xl rounded-2xl animate-scaleUp">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <HelpCircle className="w-6 h-6 text-indigo-600" />
              <h3 className="font-display font-extrabold text-xl text-slate-900">
                Khata Balance Inquiry Response
              </h3>
            </div>
            {confidence > 0 && (
              <span className="text-xs text-indigo-700 font-bold bg-indigo-50 px-2.5 py-1 rounded-full border border-indigo-100">
                AI Confidence: {Math.round(confidence * 100)}%
              </span>
            )}
          </div>

          {/* Natural Language Response Banner */}
          <div className="p-6 bg-gradient-to-r from-indigo-50 via-slate-50 to-amber-50 border border-indigo-200 rounded-2xl text-center space-y-2">
            <div className="text-xs font-bold text-indigo-700 uppercase tracking-wider">
              Outstanding Balance Query Result
            </div>
            <div className="font-display font-extrabold text-2xl md:text-3xl text-slate-900">
              {queryResult.responseMessage ||
                (queryResult.outstandingAmount !== undefined && queryResult.outstandingAmount !== null
                  ? `${queryResult.customerName} ka baki ${formatCurrency(queryResult.outstandingAmount)} hai.`
                  : `${queryResult.customerName} not found.`)}
            </div>
            <div className="text-xs text-slate-500 italic pt-1">
              Voice Query: "{statementText || customerSearchQuery}"
            </div>
          </div>

          {/* Structured Details */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs">
            <div>
              <div className="text-slate-500 font-semibold">Customer</div>
              <div className="font-extrabold text-slate-900">{queryResult.customerName}</div>
            </div>
            <div>
              <div className="text-slate-500 font-semibold">Query Intent</div>
              <div className="font-extrabold text-indigo-700">OUTSTANDING_BALANCE_QUERY</div>
            </div>
            <div>
              <div className="text-slate-500 font-semibold">Current Baki</div>
              <div className="font-extrabold text-amber-700 text-sm">
                {queryResult.outstandingAmount !== undefined && queryResult.outstandingAmount !== null
                  ? formatCurrency(queryResult.outstandingAmount)
                  : 'Customer Not Found'}
              </div>
            </div>
          </div>

          {/* Action Button */}
          <div className="flex items-center justify-end gap-3 pt-2 border-t border-slate-100">
            <button onClick={resetAll} className="btn-primary text-sm py-2.5 px-6 shadow-md">
              Done / New Query
            </button>
          </div>
        </div>
      )}

      {/* STEP 4: MANDATORY FINANCIAL CONFIRMATION BOX */}
      {selectedCustomer && preview && state !== 'CONFIRMED' && (
        <div className="card space-y-6 border-2 border-indigo-600 bg-white shadow-xl rounded-2xl animate-scaleUp">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-6 h-6 text-indigo-600" />
              <h3 className="font-display font-extrabold text-xl text-slate-900">
                Confirm Transaction Details
              </h3>
            </div>
            {confidence > 0 && (
              <span className="text-xs text-indigo-700 font-bold bg-indigo-50 px-2.5 py-1 rounded-full border border-indigo-100">
                AI Confidence: {Math.round(confidence * 100)}%
              </span>
            )}
          </div>

          {/* Prominent Confirmation Banner */}
          <div className="p-5 bg-gradient-to-r from-indigo-50 to-slate-50 border border-indigo-200/80 rounded-2xl text-center space-y-2">
            <div className="text-xs font-bold text-indigo-700 uppercase tracking-wider">
              Financial Action Confirmation
            </div>
            <div className="font-display font-extrabold text-2xl text-slate-900">
              {editType === 'UDHAAR'
                ? `Add ${formatCurrency(Number(editAmount))} credit to ${selectedCustomer.name}?`
                : `Record ${formatCurrency(Number(editAmount))} payment from ${selectedCustomer.name}?`}
            </div>
            {extractedOutstanding !== null && (
              <div className="inline-block mt-1 px-3.5 py-1 bg-amber-100 border border-amber-300 text-amber-900 text-xs font-extrabold rounded-full">
                Outstanding balance: {formatCurrency(extractedOutstanding)}
              </div>
            )}
            <div className="text-xs text-slate-500 italic pt-1">
              Transcribed Voice Text: "{statementText || customerSearchQuery}"
            </div>
          </div>


          {/* Structured Key-Value Details */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs">
            <div>
              <div className="text-slate-500 font-semibold">Customer</div>
              <div className="font-extrabold text-slate-900">{selectedCustomer.name}</div>
              <div className="text-[11px] text-slate-400">Current: {formatCurrency(selectedCustomer.currentBalance)}</div>
            </div>
            <div>
              <div className="text-slate-500 font-semibold">
                {editType === 'JAMA' ? 'Payment Received' : 'Credit Given'}
              </div>
              <div className="font-extrabold text-indigo-700 text-sm">{formatCurrency(Number(editAmount))}</div>
            </div>
            <div>
              <div className="text-slate-500 font-semibold">Outstanding Dues</div>
              <div className="font-extrabold text-amber-700 text-sm">
                {extractedOutstanding !== null ? formatCurrency(extractedOutstanding) : 'Not Specified'}
              </div>
            </div>
            <div>
              <div className="text-slate-500 font-semibold">Transaction Type</div>
              <div className="font-extrabold text-slate-900">
                {editType === 'JAMA' ? 'JAMA (Payment received)' : 'UDHAAR (Credit given)'}
              </div>
            </div>
          </div>

          {/* Editable Override Inputs */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Override Type</label>
              <select
                value={editType}
                onChange={(e) => {
                  setEditType(e.target.value as TransactionType)
                  setTimeout(refreshPreviewForEdits, 100)
                }}
                className="input-field bg-white"
              >
                <option value="UDHAAR">UDHAAR (Credit given to customer)</option>
                <option value="JAMA">JAMA (Payment received from customer)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Override Amount (₹)</label>
              <input
                type="number"
                value={editAmount}
                onChange={(e) => setEditAmount(e.target.value)}
                onBlur={refreshPreviewForEdits}
                className="input-field bg-white font-semibold"
              />
            </div>
          </div>

          {/* Confirm / Cancel Buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100">
            <button onClick={onDiscard} disabled={isSaving} className="btn-secondary">
              Cancel / Discard
            </button>
            <button
              onClick={onConfirm}
              disabled={isSaving}
              className="btn-primary text-base py-3 px-8 shadow-md"
            >
              {isSaving ? 'Saving...' : 'Confirm & Save Transaction'}
            </button>
          </div>
        </div>
      )}

      {/* CONFIRMED SUCCESS STATE */}
      {state === 'CONFIRMED' && (
        <div className="card bg-emerald-50/50 border-2 border-emerald-500 text-center space-y-5 p-8 rounded-2xl shadow-xl animate-scaleUp">
          <div className="w-16 h-16 rounded-full bg-emerald-600 text-white flex items-center justify-center mx-auto shadow-lg shadow-emerald-600/20">
            <CheckCircle2 className="w-10 h-10" />
          </div>

          <div>
            <h2 className="font-display font-extrabold text-3xl text-emerald-900">Transaction Saved</h2>
            <p className="text-sm font-semibold text-slate-600 mt-1">
              Ledger account updated successfully.
            </p>
          </div>

          {confirmedTransaction && (
            <div className="max-w-md mx-auto bg-white border border-emerald-200 p-5 rounded-2xl text-left space-y-2.5 shadow-sm text-xs">
              <div className="flex justify-between border-b border-slate-100 pb-2">
                <span className="text-slate-500 font-semibold">Party Name:</span>
                <span className="font-bold text-slate-900 text-sm">{confirmedTransaction.customerName}</span>
              </div>
              <div className="flex justify-between border-b border-slate-100 pb-2">
                <span className="text-slate-500 font-semibold">Description:</span>
                <span className="font-bold text-indigo-700">{confirmedTransaction.description}</span>
              </div>
              <div className="flex justify-between pt-1">
                <span className="text-slate-500 font-semibold">New Khata Balance:</span>
                <span className="font-bold text-emerald-700 text-base">
                  {formatCurrency(confirmedTransaction.balanceAfter)}
                </span>
              </div>
            </div>
          )}

          <div className="flex flex-wrap justify-center gap-3 pt-2">
            <button onClick={resetAll} className="btn-primary py-2.5 px-6">
              + Start Next Voice Entry
            </button>
            <button onClick={() => navigate('/customers')} className="btn-secondary py-2.5 px-5">
              View Customer Ledger
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
