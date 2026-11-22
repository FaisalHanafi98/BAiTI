import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, expect, it } from 'vitest'
import { App } from '../src/App'
import { browserSession } from '../src/auth/browser'
import '../src/i18n'
import { server } from './server'

afterEach(() => browserSession.store.setState({ session: { kind: 'signed-out' } }))

function view(permissions = ['USER_MANAGE']) {
  browserSession.store.setState({ session: {
    kind: 'signed-in', slug: 'baiti-demo', accessToken: 'synthetic',
    user: { name: 'Synthetic Admin', userId: 'admin', tenantId: 'tenant', role: 'Admin', permissions },
  } })
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { gcTime: 0 } } })
  render(<QueryClientProvider client={queries}>
    <MemoryRouter initialEntries={['/people/users']} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}><App /></MemoryRouter>
  </QueryClientProvider>)
  return queries
}

it('creates an account and shows the temporary password without caching it', async () => {
  const origin = window.location.origin
  const account = { id: 'user-a', name: 'Synthetic Resident', email: 'resident@example.invalid', roleId: 'role-a', roleName: 'Resident', status: 'ACTIVE', mustChangePassword: true }
  let created = false
  server.use(
    http.get(origin + '/api/users', () => HttpResponse.json({ content: created ? [account] : [], totalPages: created ? 1 : 0 })),
    http.get(origin + '/api/roles', () => HttpResponse.json({ content: [{ id: 'role-a', name: 'Resident' }], totalPages: 1 })),
    http.post(origin + '/api/users', async ({ request }) => {
      expect(await request.json()).toEqual({ name: account.name, email: account.email, roleId: 'role-a' })
      created = true
      return HttpResponse.json({ user: account, temporaryPassword: 'Synthetic-only-credential-928' }, { status: 201 })
    }),
  )
  const queries = view()
  const user = userEvent.setup()
  await screen.findByText('No user accounts yet.')
  await user.click(screen.getByRole('button', { name: 'Add user' }))
  await user.click(screen.getByRole('button', { name: 'Create account' }))
  expect(screen.getByRole('textbox', { name: 'Name' })).toHaveAccessibleDescription('Enter a value.')
  await user.type(screen.getByRole('textbox', { name: 'Name' }), account.name)
  await user.type(screen.getByRole('textbox', { name: 'Email address' }), account.email)
  await user.selectOptions(screen.getByRole('combobox', { name: 'Role' }), 'role-a')
  await user.click(screen.getByRole('button', { name: 'Create account' }))
  expect(await screen.findByLabelText('Temporary password')).toHaveValue('Synthetic-only-credential-928')
  expect(JSON.stringify(queries.getQueryCache().getAll().map((query) => query.state.data))).not.toContain('Synthetic-only-credential-928')
  expect(JSON.stringify(queries.getMutationCache().getAll().map((mutation) => mutation.state.data))).not.toContain('Synthetic-only-credential-928')
  await user.click(screen.getByRole('button', { name: 'Dismiss password' }))
  expect(screen.queryByLabelText('Temporary password')).toBeNull()
  expect(await screen.findByRole('cell', { name: account.email })).toBeVisible()
})

it('hides user administration from a user without USER_MANAGE', () => {
  view(['UNIT_READ_ALL'])
  expect(screen.queryByRole('link', { name: 'Users' })).toBeNull()
  expect(screen.getByRole('heading', { name: 'Welcome, Synthetic Admin' })).toBeVisible()
})

it('preserves the draft and blocks resubmission when account creation is unconfirmed', async () => {
  const origin = window.location.origin
  let attempts = 0
  let created = false
  server.use(
    http.get(origin + '/api/users', () => HttpResponse.json({ content: created ? [{ id: 'user-a', name: 'Synthetic', email: 'draft@example.invalid', status: 'ACTIVE' }] : [], totalPages: created ? 1 : 0 })),
    http.get(origin + '/api/roles', () => HttpResponse.json({ content: [{ id: 'role-a', name: 'Resident' }], totalPages: 1 })),
    http.post(origin + '/api/users', () => { attempts++; created = true; return HttpResponse.error() }),
  )
  view()
  const user = userEvent.setup()
  await screen.findByText('No user accounts yet.')
  await user.click(screen.getByRole('button', { name: 'Add user' }))
  await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Synthetic')
  await user.type(screen.getByRole('textbox', { name: 'Email address' }), 'draft@example.invalid')
  await user.selectOptions(screen.getByRole('combobox', { name: 'Role' }), 'role-a')
  await user.click(screen.getByRole('button', { name: 'Create account' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('A temporary password from a lost response cannot be retrieved.')
  expect(screen.getByRole('textbox', { name: 'Email address' })).toHaveValue('draft@example.invalid')
  expect(screen.getByRole('button', { name: 'Create account' })).toBeDisabled()
  await user.click(screen.getByRole('button', { name: 'Reload list' }))
  expect(await screen.findByRole('cell', { name: 'draft@example.invalid' })).toBeVisible()
  expect(screen.getByRole('button', { name: 'Create account' })).toBeDisabled()
  expect(attempts).toBe(1)
})

it('confirms role changes and deactivation and restores focus when an editor is dismissed', async () => {
  const origin = window.location.origin
  let roleId = 'role-a'
  let status = 'ACTIVE'
  server.use(
    http.get(origin + '/api/users', () => HttpResponse.json({ content: [{ id: 'user-a', name: 'Synthetic Resident', email: 'resident@example.invalid', roleId, roleName: roleId === 'role-a' ? 'Resident' : 'Manager', status }], totalPages: 1 })),
    http.get(origin + '/api/roles', () => HttpResponse.json({ content: [{ id: 'role-a', name: 'Resident' }, { id: 'role-b', name: 'Manager' }], totalPages: 1 })),
    http.put(origin + '/api/users/user-a/role', async ({ request }) => {
      expect(await request.json()).toEqual({ roleId: 'role-b' }); roleId = 'role-b'
      return HttpResponse.json({ id: 'user-a', roleId })
    }),
    http.post(origin + '/api/users/user-a/deactivate', () => { status = 'INACTIVE'; return new HttpResponse(null, { status: 204 }) }),
  )
  view()
  const user = userEvent.setup()
  await screen.findByRole('cell', { name: 'resident@example.invalid' })
  const change = screen.getByRole('button', { name: 'Change role for Synthetic Resident' })
  await user.click(change)
  await user.keyboard('{Escape}')
  expect(change).toHaveFocus()
  expect(screen.queryByRole('combobox', { name: 'Role' })).toBeNull()
  await user.click(change)
  await user.selectOptions(screen.getByRole('combobox', { name: 'Role' }), 'role-b')
  await user.click(screen.getByRole('button', { name: 'Save' }))
  expect(await screen.findByRole('cell', { name: 'Manager' })).toBeVisible()
  await user.click(screen.getByRole('button', { name: 'Deactivate Synthetic Resident' }))
  expect(screen.getByText(/An existing access token can remain valid/)).toBeVisible()
  await user.click(screen.getByRole('button', { name: 'Deactivate' }))
  expect(await screen.findByRole('cell', { name: 'Inactive' })).toBeVisible()
  expect(screen.queryByRole('button', { name: 'Deactivate Synthetic Resident' })).toBeNull()
})

it('shows a duplicate email error while keeping the account draft', async () => {
  const origin = window.location.origin
  server.use(
    http.get(origin + '/api/users', () => HttpResponse.json({ content: [], totalPages: 0 })),
    http.get(origin + '/api/roles', () => HttpResponse.json({ content: [{ id: 'role-a', name: 'Resident' }], totalPages: 1 })),
    http.post(origin + '/api/users', () => HttpResponse.json({ errors: [{ field: 'email' }] }, { status: 409 })),
  )
  view()
  const user = userEvent.setup()
  await screen.findByText('No user accounts yet.')
  await user.click(screen.getByRole('button', { name: 'Add user' }))
  await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Synthetic')
  await user.type(screen.getByRole('textbox', { name: 'Email address' }), 'existing@example.invalid')
  await user.selectOptions(screen.getByRole('combobox', { name: 'Role' }), 'role-a')
  await user.click(screen.getByRole('button', { name: 'Create account' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('An account with this email address already exists in this community.')
  expect(screen.getByRole('textbox', { name: 'Email address' })).toHaveValue('existing@example.invalid')
})
