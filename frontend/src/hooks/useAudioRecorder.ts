import { useCallback, useRef, useState } from 'react'

export function useAudioRecorder() {
  const [recording, setRecording] = useState(false)
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null)
  const [error, setError] = useState<string | null>(null)

  const mediaRecorderRef = useRef<MediaRecorder | null>(null)
  const audioChunksRef = useRef<Blob[]>([])

  const supported = typeof window !== 'undefined' && !!navigator?.mediaDevices?.getUserMedia

  const startRecording = useCallback(async () => {
    setError(null)
    setAudioBlob(null)
    audioChunksRef.current = []

    if (!supported) {
      setError('Microphone recording is not supported in this browser.')
      return
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      
      let mimeType = 'audio/webm'
      if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
        mimeType = 'audio/webm;codecs=opus'
      } else if (MediaRecorder.isTypeSupported('audio/webm')) {
        mimeType = 'audio/webm'
      } else if (MediaRecorder.isTypeSupported('audio/mp4')) {
        mimeType = 'audio/mp4'
      } else if (MediaRecorder.isTypeSupported('audio/ogg;codecs=opus')) {
        mimeType = 'audio/ogg;codecs=opus'
      } else if (MediaRecorder.isTypeSupported('audio/wav')) {
        mimeType = 'audio/wav'
      } else {
        mimeType = ''
      }

      const mediaRecorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)
      mediaRecorderRef.current = mediaRecorder

      mediaRecorder.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          audioChunksRef.current.push(event.data)
        }
      }

      mediaRecorder.start(200)
      setRecording(true)
    } catch (err: any) {
      console.error('Failed to start recording:', err)
      setError(err?.message || 'Permission denied for microphone access.')
      setRecording(false)
    }
  }, [supported])

  const stopRecording = useCallback((): Promise<Blob | null> => {
    return new Promise((resolve) => {
      const mediaRecorder = mediaRecorderRef.current
      if (!mediaRecorder || mediaRecorder.state === 'inactive') {
        setRecording(false)
        resolve(audioBlob)
        return
      }

      mediaRecorder.onstop = () => {
        const type = mediaRecorder.mimeType || 'audio/webm'
        const validChunks = audioChunksRef.current.filter((c) => c.size > 0)
        const blob = new Blob(validChunks, { type })
        console.log('VOICE RECORDING DEBUG - Recorder Stopped:', {
          mimeType: type,
          blobSize: blob.size,
          chunksCount: validChunks.length
        })
        setAudioBlob(blob)
        setRecording(false)

        // Stop all track media streams to release microphone hardware
        try {
          mediaRecorder.stream.getTracks().forEach((track) => track.stop())
        } catch {}

        resolve(blob)
      }

      try {
        if (mediaRecorder.state !== 'inactive') {
          mediaRecorder.requestData()
        }
      } catch (e) {
        console.warn('requestData call failed:', e)
      }

      mediaRecorder.stop()
    })
  }, [audioBlob])

  const clearRecording = useCallback(() => {
    setAudioBlob(null)
    setError(null)
    audioChunksRef.current = []
  }, [])

  return {
    recording,
    audioBlob,
    error,
    supported,
    startRecording,
    stopRecording,
    clearRecording,
  }
}
