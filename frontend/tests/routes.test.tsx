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

function view(path: string) {
  return render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <App />
    </MemoryRouter>
  </QueryClientProvider>)
}

it('redirects protected routes and shows validation beside the original field label', async () => {
  browserSession.store.setState({ session: { kind: 'signed-out' } })
  view('/dashboard')
  expect(screen.getByRole('heading', { name: 'Sign in' })).toBeVisible()
  await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
  expect(screen.getByRole('textbox', { name: 'Community code' })).toHaveAccessibleDescription(
    'Enter a community code such as taman-example.',
  )
})

it('renders only unit numbers to Guards and skips the block API', async () => {
  const origin = window.location.origin
  server.use(http.get(origin + '/api/units', ({ request }) => {
    expect(new URL(request.url).searchParams.has('blockId')).toBe(false)
    expect(new URL(request.url).searchParams.has('status')).toBe(false)
    return HttpResponse.json({ content: [{ unitNo: '10A' }], totalPages: 1 })
  }))
  browserSession.store.setState({ session: {
    kind: 'signed-in', slug: 'baiti-demo', accessToken: 'synthetic',
    user: { name: 'Synthetic Guard', userId: 'guard', tenantId: 'tenant', role: 'Guard', permissions: ['UNIT_READ_ALL'] },
  } })
  view('/property')
  expect(await screen.findByRole('cell', { name: '10A' })).toBeVisible()
  expect(screen.getAllByRole('columnheader').map((header) => header.textContent)).toEqual(['Unit number'])
  expect(screen.queryByRole('button', { name: 'Add unit' })).toBeNull()
})

it('preserves a block draft and requires a fresh read when a write response is lost', async () => {
  const origin = window.location.origin
  let written = false
  let attempts = 0
  server.use(
    http.get(origin + '/api/blocks', () => HttpResponse.json({
      content: written ? [{ id: 'block-a', name: 'Jalan 1' }] : [], totalPages: written ? 1 : 0,
    })),
    http.post(origin + '/api/blocks', () => {
      attempts++
      written = true
      return HttpResponse.error()
    }),
  )
  browserSession.store.setState({ session: {
    kind: 'signed-in', slug: 'baiti-demo', accessToken: 'synthetic',
    user: { name: 'Synthetic Admin', userId: 'admin', tenantId: 'tenant', role: 'Admin', permissions: ['BLOCK_MANAGE'] },
  } })
  const user = userEvent.setup()
  view('/property')
  await screen.findByText('No blocks yet.')
  await user.click(screen.getByRole('button', { name: 'Add block' }))
  await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Jalan 1')
  await user.click(screen.getByRole('button', { name: 'Save' }))
  const unconfirmedAlert = await screen.findByRole('alert')
  expect(unconfirmedAlert).toHaveTextContent('The server may have saved this change. Reload the list and check the result before submitting again.')
  expect(unconfirmedAlert.closest('.callout')).toHaveAttribute('data-tone', 'warning')
  expect(screen.getByRole('textbox', { name: 'Name' })).toHaveValue('Jalan 1')
  expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
  await user.click(screen.getByRole('button', { name: 'Reload list' }))
  expect(await screen.findByRole('cell', { name: 'Jalan 1' })).toBeVisible()
  expect(screen.getByRole('button', { name: 'Save' })).toBeEnabled()
  expect(attempts).toBe(1)
})

it('dismisses open inline editors and delete confirmations on Escape and marks occupancy and callout tones', async () => {
  const origin = window.location.origin
  server.use(
    http.get(origin + '/api/blocks', () => HttpResponse.json({
      content: [{ id: 'block-a', name: 'Persiaran Kenari Utama-Alpha', description: 'Phase 1' }], totalPages: 1,
    })),
    http.get(origin + '/api/units', () => HttpResponse.json({
      content: [
        { id: 'unit-1', unitNo: '10A', blockId: 'block-a', blockName: 'Persiaran Kenari Utama-Alpha', floorNo: 'G', type: 'Terrace', status: 'OCCUPIED' },
        { id: 'unit-2', unitNo: '10B', blockId: 'block-a', blockName: 'Persiaran Kenari Utama-Alpha', floorNo: 'G', type: 'Terrace', status: 'VACANT' },
      ],
      totalPages: 1,
    })),
  )
  browserSession.store.setState({ session: {
    kind: 'signed-in', slug: 'baiti-demo', accessToken: 'synthetic',
    user: { name: 'Synthetic Admin', userId: 'admin', tenantId: 'tenant', role: 'Admin', permissions: ['BLOCK_MANAGE', 'UNIT_READ_ALL', 'UNIT_MANAGE'] },
  } })
  const user = userEvent.setup()
  view('/property')
  expect(await screen.findByRole('cell', { name: '10A' })).toBeVisible()
  expect(screen.getByText('Occupied', { selector: '.status' })).toHaveAttribute('data-status', 'OCCUPIED')
  expect(screen.getByText('Vacant', { selector: '.status' })).toHaveAttribute('data-status', 'VACANT')

  const addBlockBtn = screen.getByRole('button', { name: 'Add block' })
  await user.click(addBlockBtn)
  const nameInput = screen.getByRole('textbox', { name: 'Name' })
  expect(nameInput).toBeVisible()
  await user.keyboard('{Escape}')
  expect(screen.queryByRole('textbox', { name: 'Name' })).toBeNull()
  expect(addBlockBtn).toHaveFocus()

  const deleteBlockBtn = screen.getByRole('button', { name: 'Delete Persiaran Kenari Utama-Alpha' })
  await user.click(deleteBlockBtn)
  expect(screen.getByRole('region', { name: 'Delete Persiaran Kenari Utama-Alpha?' })).toBeVisible()
  await user.keyboard('{Escape}')
  expect(screen.queryByRole('region', { name: 'Delete Persiaran Kenari Utama-Alpha?' })).toBeNull()
  expect(deleteBlockBtn).toHaveFocus()
})
