import { api } from './api'
import type { NlpExtractionResult } from '../types'

export const nlpService = {
  extract: (transcript: string) =>
    api.post<{ data: NlpExtractionResult }>('/nlp/extract', { transcript }).then((r) => r.data.data)
}
