import { useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { describeError } from '../api/errors'
import { browserSession } from './browser'

const loginSchema = z.object({
  slug: z.string().trim().toLowerCase().regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, 'errors.community'),
  email: z.email('errors.email').max(320, 'errors.email'),
  password: z.string().min(1, 'errors.required').max(72, 'errors.passwordLength'),
})
const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'errors.required'),
  newPassword: z.string().min(12, 'errors.passwordLength').refine(
    (value) => new TextEncoder().encode(value).length <= 72, 'errors.passwordLength',
  ),
  confirmPassword: z.string(),
}).refine((value) => value.newPassword === value.confirmPassword, {
  path: ['confirmPassword'], message: 'errors.passwordMatch',
}).refine((value) => value.newPassword !== value.currentPassword, {
  path: ['newPassword'], message: 'errors.passwordDifferent',
})

export function Login({ changed, logoutUnconfirmed, onSubmit }: {
  changed: boolean; logoutUnconfirmed: boolean; onSubmit: () => void;
}) {
  const { t } = useTranslation()
  const [failure, setFailure] = useState<ReturnType<typeof describeError>>()
  const form = useForm<z.infer<typeof loginSchema>>({ resolver: zodResolver(loginSchema) })
  return <main className="auth-screen"><section className="card auth-card">
    <p className="eyebrow">{t('brand')}</p>
    <h1>{t('signIn')}</h1><p>{t('loginHelp')}</p>
    {changed && <p className="callout" data-tone="notice" role="status">{t('passwordChanged')}</p>}
    {logoutUnconfirmed && <p className="callout" data-tone="warning" role="status">{t('logoutUnconfirmed')}</p>}
    {failure && <p className="callout error" data-tone="danger" role="alert">{t(failure.key, failure)}</p>}
    <form noValidate onSubmit={form.handleSubmit(async ({ slug, ...credentials }) => {
      onSubmit()
      setFailure(undefined)
      try { await browserSession.signIn(slug, credentials) }
      catch (error) { setFailure(describeError(error, 'signIn')) }
    })}>
      {([
        ['slug', 'community', 'text', 'organization'],
        ['email', 'email', 'email', 'username'],
        ['password', 'password', 'password', 'current-password'],
      ] as const).map(([name, label, type, autocomplete]) => {
        const error = form.formState.errors[name]?.message
        return <div key={name}><label>{t(label)}
          <input type={type} autoComplete={autocomplete} aria-invalid={!!error}
            aria-describedby={error ? name + '-error' : undefined} {...form.register(name)} />
        </label>{error && <span className="error" id={name + '-error'}>{t(error)}</span>}</div>
      })}
      <button disabled={form.formState.isSubmitting}>{t(form.formState.isSubmitting ? 'signingIn' : 'signIn')}</button>
    </form>
  </section></main>
}

export function Password({ onChanged, onLogoutUnconfirmed }: {
  onChanged: () => void; onLogoutUnconfirmed: () => void;
}) {
  const { t } = useTranslation()
  const [failure, setFailure] = useState<ReturnType<typeof describeError>>()
  const [leaving, setLeaving] = useState(false)
  const form = useForm<z.infer<typeof passwordSchema>>({ resolver: zodResolver(passwordSchema) })
  return <main className="auth-screen"><section className="card auth-card">
    <p className="eyebrow">{t('brand')}</p>
    <h1>{t('firstUse')}</h1><p>{t('firstUseHelp')}</p>
    {failure && <p className="callout error" data-tone="danger" role="alert">{t(failure.key, failure)}</p>}
    <form noValidate onSubmit={form.handleSubmit(async ({ currentPassword, newPassword }) => {
      setFailure(undefined)
      try { await browserSession.replacePassword({ currentPassword, newPassword }); onChanged() }
      catch (error) { setFailure(describeError(error, 'password')) }
    })}>
      {([
        ['currentPassword', 'current-password'], ['newPassword', 'new-password'], ['confirmPassword', 'new-password'],
      ] as const).map(([name, autocomplete]) => {
        const error = form.formState.errors[name]?.message
        return <div key={name}><label>{t(name)}
          <input type="password" autoComplete={autocomplete} aria-invalid={!!error}
            aria-describedby={error ? name + '-error' : undefined} {...form.register(name)} />
        </label>{error && <span className="error" id={name + '-error'}>{t(error)}</span>}</div>
      })}
      <button disabled={form.formState.isSubmitting || leaving}>{t(form.formState.isSubmitting ? 'saving' : 'replacePassword')}</button>
      <button className="secondary" type="button" disabled={form.formState.isSubmitting || leaving} onClick={async () => {
        setLeaving(true)
        try { await browserSession.signOut() } catch {
          onLogoutUnconfirmed()
        }
      }}>{t('logout')}</button>
    </form>
  </section></main>
}
