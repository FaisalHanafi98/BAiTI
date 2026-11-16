import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { browserSession } from './auth/browser'
import { App } from './App'
import './i18n'
import './styles.css'

const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } })
browserSession.store.subscribe(({ session }, { session: prior }) => {
  if ((prior.kind === 'signed-in' || prior.kind === 'password-required') &&
      (session.kind !== prior.kind || !('user' in session) || session.user.userId !== prior.user.userId || session.user.tenantId !== prior.user.tenantId)) {
    queries.clear()
  }
})

createRoot(document.getElementById('root')!).render(
  <QueryClientProvider client={queries}><BrowserRouter><App /></BrowserRouter></QueryClientProvider>,
)
void browserSession.bootstrap()
