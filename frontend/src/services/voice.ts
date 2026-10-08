import { api } from './api'

export interface VoiceConfig {
  mode: string
  sarvamConfigured: boolean
  provider: string
}

export interface TranscribeResponse {
  transcript: string
  provider: string
}

export const voiceService = {
  getConfig: async (): Promise<VoiceConfig> => {
    const res = await api.get<{ data: VoiceConfig }>('/voice/config')
    return res.data.data
  },

  transcribeAudio: async (audioBlob: Blob, languageCode = 'hi-IN'): Promise<TranscribeResponse> => {
    if (!audioBlob || audioBlob.size === 0) {
      throw new Error('Voice recording is empty or invalid. Please record again.')
    }

    const rawType = audioBlob.type || 'audio/webm'
    let ext = 'webm'
    let cleanMime = 'audio/webm'

    if (rawType.includes('wav')) {
      ext = 'wav'
      cleanMime = 'audio/wav'
    } else if (rawType.includes('mp4') || rawType.includes('m4a')) {
      ext = 'mp4'
      cleanMime = 'audio/mp4'
    } else if (rawType.includes('ogg')) {
      ext = 'ogg'
      cleanMime = 'audio/ogg'
    } else if (rawType.includes('mp3') || rawType.includes('mpeg')) {
      ext = 'mp3'
      cleanMime = 'audio/mpeg'
    } else {
      ext = 'webm'
      cleanMime = 'audio/webm'
    }

    const filename = `recording.${ext}`
    const audioFile = new File([audioBlob], filename, { type: cleanMime })

    console.log('VOICE RECORDING DEBUG', {
      browserMimeType: audioBlob.type,
      blobSize: audioBlob.size,
      fileName: filename,
      fileType: audioFile.type,
      finalUploadMimeType: cleanMime
    })

    const formData = new FormData()
    formData.append('file', audioFile, filename)
    formData.append('languageCode', languageCode)

    const res = await api.post<{ data: TranscribeResponse }>('/voice/transcribe', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return res.data.data
  },

  transcribeText: async (transcript: string): Promise<TranscribeResponse> => {
    const res = await api.post<{ data: TranscribeResponse }>('/voice/transcribe-json', { transcript })
    return res.data.data
  },
}
