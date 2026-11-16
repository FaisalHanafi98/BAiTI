import { useStore } from 'zustand'
import { createSession } from './session'

export const browserSession = createSession(import.meta.env.VITE_API_URL || window.location.origin)
export const useSession = () => useStore(browserSession.store, (state) => state.session)
