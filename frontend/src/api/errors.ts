import axios from 'axios'

export function isWriteUnconfirmed(error: unknown) {
  return axios.isAxiosError(error) && (!error.response || error.response.status >= 500)
}

export function describeError(error: unknown, context: 'signIn' | 'password' | 'property' | 'people') {
  if (!axios.isAxiosError(error)) return { key: 'errors.unavailable' }
  const status = error.response?.status
  if (status === 429) {
    const seconds = Number(error.response?.headers['retry-after'])
    return { key: 'errors.rateLimit', seconds: seconds > 0 ? seconds : 60 }
  }
  if (status === 401) {
    return { key: context === 'signIn' ? 'errors.signIn' : context === 'password' ? 'errors.password' : 'errors.session' }
  }
  if (status === 400) return { key: 'errors.invalid' }
  if (status === 403) return { key: 'errors.denied' }
  if (status === 404) return { key: 'errors.missing' }
  if (status === 409) {
    const fields = error.response?.data?.errors
    if (context === 'people' && Array.isArray(fields) && fields.some((field) => field?.field === 'email')) return { key: 'people.duplicateEmail' }
    if (Array.isArray(fields) && fields.some((field) => field?.field === 'name')) return { key: 'errors.duplicateBlock' }
    if (Array.isArray(fields) && fields.some((field) => field?.field === 'unitNo')) return { key: 'errors.duplicateUnit' }
    return { key: 'errors.conflict' }
  }
  return { key: 'errors.unavailable' }
}
