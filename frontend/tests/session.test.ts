import { http, HttpResponse, delay } from 'msw'
import { describe, expect, it } from 'vitest'
import { createSession } from '../src/auth/session'
import { server } from './server'

const origin = 'http://localhost:3000'
const slug = 'baiti-demo'
const user = {
  userId: 'a', tenantId: 'tenant-a', name: 'Synthetic Admin', email: 'admin@example.invalid',
  role: 'Admin', permissions: ['BLOCK_MANAGE'], mustChangePassword: false,
}

describe('browser session', () => {
  it('finishes first-use replacement by returning to sign-in', async () => {
    const session = createSession(origin)
    server.use(
      http.post(origin + '/api/auth/login', () => HttpResponse.json({
        accessToken: 'restricted', user: { ...user, mustChangePassword: true }, expiresIn: 60,
      })),
      http.post(origin + '/api/auth/password', ({ request }) => {
        expect(request.headers.get('Authorization')).toBe('Bearer restricted')
        return new HttpResponse(null, { status: 204 })
      }),
    )
    await session.signIn(slug, { email: user.email, password: 'temporary' })
    expect(session.store.getState().session.kind).toBe('password-required')
    expect(session.can('BLOCK_MANAGE')).toBe(false)
    await session.replacePassword({ currentPassword: 'temporary', newPassword: 'replacement' })
    expect(session.store.getState().session.kind).toBe('signed-out')
  })

  it('shares one refresh across requests and retries each once with the new bearer', async () => {
    const session = createSession(origin)
    let refreshes = 0
    server.use(
      http.post(origin + '/api/auth/login', () => HttpResponse.json({ accessToken: 'old', user, expiresIn: 1 })),
      http.get(origin + '/api/auth/me', ({ request }) =>
        request.headers.get('Authorization') === 'Bearer new'
          ? HttpResponse.json(user)
          : new HttpResponse(null, { status: 401 }),
      ),
      http.post(origin + '/api/auth/refresh', async ({ request }) => {
        expect(request.headers.get('X-BAiTI-Refresh')).toBe('true')
        expect(request.headers.get('Authorization')).toBeNull()
        refreshes++
        await delay(40)
        return HttpResponse.json({ accessToken: 'new', user, expiresIn: 60 })
      }),
    )
    await session.signIn(slug, { email: user.email, password: 'normal' })
    expect(session.can('BLOCK_MANAGE')).toBe(true)
    const result = await Promise.all([session.api.get('/api/auth/me'), session.api.get('/api/auth/me')])
    expect(result.map(({ data }) => data.name)).toEqual([user.name, user.name])
    expect(refreshes).toBe(1)
  })

  it('returns to sign-in after a refresh fails and never retries the refresh', async () => {
    const session = createSession(origin)
    let refreshes = 0
    server.use(
      http.post(origin + '/api/auth/login', () => HttpResponse.json({ accessToken: 'old', user, expiresIn: 1 })),
      http.get(origin + '/api/auth/me', () => new HttpResponse(null, { status: 401 })),
      http.post(origin + '/api/auth/refresh', () => {
        refreshes++
        return new HttpResponse(null, { status: 401 })
      }),
    )
    await session.signIn(slug, { email: user.email, password: 'normal' })
    await expect(session.api.get('/api/auth/me')).rejects.toMatchObject({ response: { status: 401 } })
    expect(session.store.getState().session.kind).toBe('signed-out')
    expect(refreshes).toBe(1)
  })

  it('refreshes an expired bearer once to confirm server-side logout', async () => {
    const session = createSession(origin)
    let logoutAttempts = 0
    let refreshes = 0
    server.use(
      http.post(origin + '/api/auth/login', () => HttpResponse.json({ accessToken: 'old', user, expiresIn: 1 })),
      http.post(origin + '/api/auth/refresh', () => {
        refreshes++
        return HttpResponse.json({ accessToken: 'new', user, expiresIn: 60 })
      }),
      http.post(origin + '/api/auth/logout', ({ request }) => {
        logoutAttempts++
        return new HttpResponse(null, { status: request.headers.get('Authorization') === 'Bearer new' ? 204 : 401 })
      }),
    )
    await session.signIn(slug, { email: user.email, password: 'normal' })
    await session.signOut()
    expect(session.store.getState().session.kind).toBe('signed-out')
    expect(logoutAttempts).toBe(2)
    expect(refreshes).toBe(1)
  })

  it('keeps failed sign-in local and does not refresh a 401 from login', async () => {
    const session = createSession(origin)
    server.use(http.post(origin + '/api/auth/login', () => new HttpResponse(null, { status: 401 })))
    await expect(session.signIn(slug, { email: user.email, password: 'bad' }))
      .rejects.toMatchObject({ response: { status: 401 } })
    expect(session.store.getState().session.kind).toBe('signed-out')
  })
})
