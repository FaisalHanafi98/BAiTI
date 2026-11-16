import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom'
import { browserSession, useSession } from './auth/browser'
import { Login, Password } from './auth/Forms'
import { Property } from './property/Property'

function Layout({ onLogoutUnconfirmed }: { onLogoutUnconfirmed: () => void }) {
  const { t } = useTranslation()
  const [pending, setPending] = useState(false)
  const session = useSession()
  if (session.kind !== 'signed-in') return <Navigate to="/login" replace />
  const showProperty = browserSession.can('BLOCK_MANAGE') || browserSession.can('UNIT_READ_ALL')
  return <div className="layout">
    <header className="header">
      <Link className="brand" to="/dashboard">{t('brand')}</Link>
      <nav aria-label={t('workspace')}>
        <NavLink to="/dashboard">{t('dashboard')}</NavLink>
        {showProperty && <NavLink to="/property">{t('property')}</NavLink>}
      </nav>
      <button className="secondary" disabled={pending} onClick={async () => {
        setPending(true)
        try { await browserSession.signOut() } catch {
          onLogoutUnconfirmed()
        }
      }}>{t(pending ? 'logoutPending' : 'logout')}</button>
    </header>
    <main className="content"><Outlet /></main>
  </div>
}

function Dashboard() {
  const { t } = useTranslation()
  const session = useSession()
  const [status, setStatus] = useState<'checking' | 'sessionActive' | 'errors.unavailable'>()
  if (session.kind !== 'signed-in') return null
  return <section className="card">
    <p className="eyebrow">{t('workspace')}</p>
    <h1>{t('welcome', { name: session.user.name })}</h1>
    <p>{t('role')} <strong>{session.user.role}</strong></p>
    <button className="secondary" disabled={status === 'checking'} onClick={async () => {
      setStatus('checking')
      try {
        await browserSession.api.get('/api/auth/me')
        setStatus('sessionActive')
      } catch { setStatus('errors.unavailable') }
    }}>{t('checkSession')}</button>
    {status && <p className={status === 'errors.unavailable' ? 'callout error' : 'callout'} data-tone={status === 'errors.unavailable' ? 'danger' : 'notice'} role="status">{t(status)}</p>}
  </section>
}

export function App() {
  const { t } = useTranslation()
  const session = useSession()
  const [changed, setChanged] = useState(false)
  const [logoutUnconfirmed, setLogoutUnconfirmed] = useState(false)
  if (session.kind === 'bootstrapping') return <main className="auth-screen"><p role="status">{t('loading')}</p></main>
  if (session.kind === 'password-required') {
    return <Routes><Route path="/password" element={<Password onChanged={() => setChanged(true)} onLogoutUnconfirmed={() => setLogoutUnconfirmed(true)} />} />
      <Route path="*" element={<Navigate to="/password" replace />} /></Routes>
  }
  if (session.kind === 'signed-out') {
    return <Routes><Route path="/login" element={<Login changed={changed} logoutUnconfirmed={logoutUnconfirmed} onSubmit={() => { setChanged(false); setLogoutUnconfirmed(false) }} />} />
      <Route path="*" element={<Navigate to="/login" replace />} /></Routes>
  }
  return <Routes>
    <Route element={<Layout onLogoutUnconfirmed={() => setLogoutUnconfirmed(true)} />}>
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/property" element={
        browserSession.can('BLOCK_MANAGE') || browserSession.can('UNIT_READ_ALL')
          ? <Property /> : <Navigate to="/dashboard" replace />
      } />
    </Route>
    <Route path="*" element={<Navigate to="/dashboard" replace />} />
  </Routes>
}
