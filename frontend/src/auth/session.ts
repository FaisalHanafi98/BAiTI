import axios, { type InternalAxiosRequestConfig } from 'axios'
import { createStore } from 'zustand/vanilla'
import type { components } from '../api/types.gen'

type User = components['schemas']['UserIdentity']
type AuthResponse = components['schemas']['AuthResponse']
type Login = components['schemas']['LoginRequest']
type PasswordChange = components['schemas']['PasswordChangeRequest']
type Authenticated = {
  kind: 'signed-in' | 'password-required'
  slug: string
  user: User
  accessToken: string
}
type Session = Authenticated | { kind: 'bootstrapping' | 'signed-out' }
type SessionRequest = InternalAxiosRequestConfig & { sessionRevision?: number; retried?: boolean }
const slugKey = 'baiti.last-community'

function remember(slug?: string) {
  try {
    if (slug) sessionStorage.setItem(slugKey, slug)
    else sessionStorage.removeItem(slugKey)
  } catch {
    // Sign-in works when storage is unavailable.
  }
}

function rememberedSlug() {
  try { return sessionStorage.getItem(slugKey) || undefined } catch { return undefined }
}

async function withCookieLock<T>(action: () => Promise<T>): Promise<T> {
  return navigator.locks ? navigator.locks.request('baiti-session-cookie', action) : action()
}

function authenticated(session: Session): session is Authenticated {
  return session.kind === 'signed-in' || session.kind === 'password-required'
}

export function createSession(baseURL = window.location.origin) {
  const store = createStore<{ session: Session }>(() => ({ session: { kind: 'bootstrapping' } }))
  const transport = axios.create({ baseURL, withCredentials: true, timeout: 12_000 })
  const api = axios.create({ baseURL, withCredentials: true, timeout: 12_000 })
  const tenant = (slug: string) => ({ 'X-Tenant-Slug': slug })
  const current = () => store.getState().session
  let revision = 0
  let refreshFlight: Promise<void> | undefined

  function signedOut(forgetSlug = false) {
    revision++
    if (forgetSlug) remember()
    store.setState({ session: { kind: 'signed-out' } })
  }

  function install(slug: string, data: AuthResponse) {
    if (!data.user?.userId || !data.user.tenantId || !data.accessToken || typeof data.user.mustChangePassword !== 'boolean') {
      throw new Error('Invalid authentication response')
    }
    remember(slug)
    store.setState({
      session: {
        kind: data.user.mustChangePassword ? 'password-required' : 'signed-in',
        slug, user: data.user, accessToken: data.accessToken,
      },
    })
  }

  function refresh(slug: string) {
    if (!refreshFlight) {
      const started = revision
      refreshFlight = withCookieLock(async () => {
        if (revision !== started) return
        const { data } = await transport.post<AuthResponse>('/api/auth/refresh', undefined, {
          headers: { ...tenant(slug), 'X-BAiTI-Refresh': 'true' },
        })
        if (revision !== started) return
        const prior = current()
        if (authenticated(prior) && (prior.user.userId !== data.user?.userId || prior.user.tenantId !== data.user?.tenantId)) {
          throw new Error('The browser session changed')
        }
        install(slug, data)
      }).catch((error: unknown) => {
        if (revision === started) signedOut()
        throw error
      }).finally(() => { refreshFlight = undefined })
    }
    return refreshFlight
  }

  api.interceptors.request.use((config: SessionRequest) => {
    const session = current()
    if (!authenticated(session)) throw new axios.CanceledError('No active session')
    if (config.sessionRevision !== undefined && config.sessionRevision !== revision) {
      throw new axios.CanceledError('The browser session changed')
    }
    config.sessionRevision = revision
    config.headers.set(tenant(session.slug))
    config.headers.set('Authorization', 'Bearer ' + session.accessToken)
    return config
  })

  api.interceptors.response.use(undefined, async (error: unknown) => {
    if (!axios.isAxiosError(error) || error.response?.status !== 401) throw error
    const config: SessionRequest | undefined = error.config
    const session = current()
    if (!config || config.signal?.aborted || config.sessionRevision !== revision || session.kind !== 'signed-in') throw error
    if (config.retried) {
      if (config.headers.get('Authorization') === 'Bearer ' + session.accessToken) signedOut()
      throw error
    }
    config.retried = true
    if (config.headers.get('Authorization') === 'Bearer ' + session.accessToken) await refresh(session.slug)
    return api.request(config)
  })

  return {
    store, api,
    can: (permission: string) => {
      const session = current()
      return session.kind === 'signed-in' && !!session.user.permissions?.includes(permission)
    },
    async bootstrap(slug = rememberedSlug()) {
      if (current().kind !== 'bootstrapping') return
      if (!slug) { signedOut(); return }
      try { await refresh(slug) } catch { /* The refresh transition already cleared the session. */ }
    },
    async signIn(slug: string, credentials: Login) {
      if (current().kind === 'bootstrapping') signedOut()
      const started = ++revision
      await withCookieLock(async () => {
        if (started !== revision) return
        const { data } = await transport.post<AuthResponse>('/api/auth/login', credentials, { headers: tenant(slug) })
        if (started === revision) install(slug, data)
      })
    },
    async replacePassword(credentials: PasswordChange) {
      const session = current()
      if (session.kind !== 'password-required') throw new Error('No first-use session')
      const started = revision
      await withCookieLock(async () => {
        if (started !== revision) return
        await transport.post('/api/auth/password', credentials, {
          headers: { ...tenant(session.slug), Authorization: 'Bearer ' + session.accessToken },
        })
        if (started === revision) signedOut()
      })
    },
    async signOut() {
      const started = revision
      try {
        await withCookieLock(async () => {
          const session = current()
          if (started !== revision || !authenticated(session)) return
          const sendLogout = (accessToken: string) => transport.post('/api/auth/logout', undefined, {
            headers: { ...tenant(session.slug), Authorization: 'Bearer ' + accessToken },
          })
          try {
            await sendLogout(session.accessToken)
          } catch (error) {
            if (!axios.isAxiosError(error) || error.response?.status !== 401 || session.kind !== 'signed-in') throw error
            const { data } = await transport.post<AuthResponse>('/api/auth/refresh', undefined, {
              headers: { ...tenant(session.slug), 'X-BAiTI-Refresh': 'true' },
            })
            if (!data.accessToken || data.user?.userId !== session.user.userId || data.user?.tenantId !== session.user.tenantId) {
              throw new Error('The browser session changed')
            }
            await sendLogout(data.accessToken)
          }
        })
      } finally {
        if (started === revision) signedOut(true)
      }
    },
  }
}
