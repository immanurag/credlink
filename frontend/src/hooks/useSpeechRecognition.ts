import { useCallback, useRef, useState } from 'react'

// Thin wrapper around the browser's Web Speech API (webkitSpeechRecognition / SpeechRecognition).
// Kept isolated in one hook so the backend can later be swapped to a Whisper-based flow
// (record audio -> POST /voice/transcribe) without touching any screen that consumes this hook.
export function useSpeechRecognition() {
  const [transcript, setTranscript] = useState('')
  const [listening, setListening] = useState(false)
  const [supported] = useState(() => {
    const w = window as any
    return !!(w.SpeechRecognition || w.webkitSpeechRecognition)
  })

  const recognitionRef = useRef<any>(null)
  const latestTranscriptRef = useRef<string>('')
  const onFinalCalledRef = useRef<boolean>(false)

  const [lang, setLang] = useState<'hi-IN' | 'en-IN'>('hi-IN')

  const start = useCallback((onFinal?: (text: string) => void, overrideLang?: 'hi-IN' | 'en-IN') => {
    const w = window as any
    const SpeechRecognitionImpl = w.SpeechRecognition || w.webkitSpeechRecognition
    if (!SpeechRecognitionImpl) return

    if (recognitionRef.current) {
      try {
        recognitionRef.current.stop()
      } catch {}
    }

    const recognition = new SpeechRecognitionImpl()
    recognition.lang = overrideLang || lang
    recognition.continuous = false
    recognition.interimResults = true

    // Reset session refs and state
    latestTranscriptRef.current = ''
    onFinalCalledRef.current = false
    setTranscript('')

    recognition.onresult = (event: any) => {
      let finalText = ''
      let interimText = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const chunk = event.results[i][0].transcript
        if (event.results[i].isFinal) finalText += chunk
        else interimText += chunk
      }
      const currentText = (finalText || interimText).trim()
      if (currentText) {
        latestTranscriptRef.current = currentText
        setTranscript(currentText)
      }
      if (finalText && onFinal && !onFinalCalledRef.current) {
        onFinalCalledRef.current = true
        onFinal(finalText.trim())
      }
    }

    recognition.onerror = () => setListening(false)

    recognition.onend = () => {
      setListening(false)
      const textToDeliver = latestTranscriptRef.current.trim()
      if (textToDeliver && onFinal && !onFinalCalledRef.current) {
        onFinalCalledRef.current = true
        onFinal(textToDeliver)
      }
    }

    recognitionRef.current = recognition
    setListening(true)
    recognition.start()
  }, [lang])

  const stop = useCallback(() => {
    recognitionRef.current?.stop()
    setListening(false)
  }, [])

  return { transcript, listening, supported, start, stop, setTranscript, lang, setLang }
}
