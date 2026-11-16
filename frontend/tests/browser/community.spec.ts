import { randomUUID } from 'node:crypto'
import { expect, test, type Page } from '@playwright/test'

const firstSlug = 'baiti-dev-rumbia'
const otherSlug = 'baiti-dev-kenari'
const firstEmail = firstSlug + '@example.invalid'
const otherEmail = otherSlug + '@example.invalid'

async function signIn(page: Page, slug: string, email: string, password: string) {
  await page.getByLabel('Community code').fill(slug)
  await page.getByLabel('Email address').fill(email)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
}

test('first-use sign-in, property, isolation, refresh, and logout', async ({ page, browser }) => {
  const initial = process.env.BAITI_BROWSER_INITIAL_PASSWORD
  const replacement = process.env.BAITI_BROWSER_REPLACEMENT
  const otherPassword = process.env.BAITI_BROWSER_OTHER_PASSWORD
  if (!initial || !replacement || !otherPassword) throw new Error('Use a dedicated seeded browser test database and private credentials.')

  await page.goto('/dashboard')
  await expect(page).toHaveURL(/\/login$/)
  await signIn(page, firstSlug, firstEmail, 'incorrect-synthetic-password')
  await expect(page.getByRole('alert')).toHaveText('Check your community code, email and password.')
  await signIn(page, otherSlug, firstEmail, initial)
  await expect(page.getByRole('alert')).toHaveText('Check your community code, email and password.')

  await signIn(page, firstSlug, firstEmail, initial)
  await expect(page).toHaveURL(/\/password$/)
  await page.getByLabel('Current password', { exact: true }).fill(initial)
  await page.getByLabel('New password', { exact: true }).fill(replacement)
  await page.getByLabel('Confirm new password', { exact: true }).fill(replacement)
  await page.getByRole('button', { name: 'Change password' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('status')).toHaveText('Password changed. Sign in again with your new password.')
  const normalLogin = page.waitForResponse((response) => response.url().endsWith('/api/auth/login') && response.ok())
  await signIn(page, firstSlug, firstEmail, replacement)
  const auth = await (await normalLogin).json() as { accessToken: string; expiresIn: number }
  await expect(page.getByRole('heading', { name: 'Welcome, Synthetic Admin' })).toBeVisible()

  await page.getByRole('link', { name: 'Property' }).click()
  const blockName = 'J1 ' + randomUUID()
  await page.getByRole('button', { name: 'Add block' }).click()
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByText('Enter a value.')).toBeVisible()
  await page.getByLabel('Name', { exact: true }).fill(blockName)
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByRole('cell', { name: blockName, exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Add unit' })).toBeEnabled()
  await page.getByRole('button', { name: 'Add unit' }).click()
  const editor = page.locator('form')
  await editor.getByRole('combobox', { name: 'Block', exact: true }).selectOption({ label: blockName })
  await editor.getByRole('textbox', { name: 'Unit number' }).fill('10A')
  await editor.getByRole('textbox', { name: 'Floor' }).fill('G')
  const unitCreated = page.waitForResponse((response) => response.url().endsWith('/api/units') && response.status() === 201)
  await page.getByRole('button', { name: 'Save' }).click()
  const { id } = await (await unitCreated).json() as { id: string }
  await expect(page.getByRole('cell', { name: '10A', exact: true })).toBeVisible()

  await page.getByRole('button', { name: 'Edit 10A', exact: true }).click()
  await page.locator('form').getByRole('combobox', { name: 'Occupancy', exact: true }).selectOption('OCCUPIED')
  await page.getByRole('button', { name: 'Save' }).click()
  const unitRow = page.getByRole('row').filter({ has: page.getByRole('cell', { name: '10A', exact: true }) })
  await expect(unitRow.getByRole('cell', { name: 'Occupied' })).toBeVisible()

  const other = await browser.newContext()
  try {
    const otherPage = await other.newPage()
    await otherPage.goto('http://localhost:5173/login')
    const otherLogin = otherPage.waitForResponse((response) => response.url().endsWith('/api/auth/login') && response.ok())
    await signIn(otherPage, otherSlug, otherEmail, otherPassword)
    const otherAuth = await (await otherLogin).json() as { accessToken: string }
    const denied = await otherPage.request.get('http://localhost:5173/api/units/' + id, {
      headers: { Authorization: 'Bearer ' + otherAuth.accessToken, 'X-Tenant-Slug': otherSlug },
    })
    expect(denied.status()).toBe(404)
    expect((await denied.json()).detail).toBe('Resource not found')
  } finally {
    await other.close()
  }

  const expiry = await page.request.get('/api/auth/me', {
    headers: { Authorization: 'Bearer ' + auth.accessToken, 'X-Tenant-Slug': firstSlug },
  })
  if (expiry.status() === 200) await new Promise((resolve) => setTimeout(resolve, (auth.expiresIn + 1) * 1000))
  await page.getByRole('link', { name: 'Overview' }).click()
  const refresh = page.waitForResponse((response) => response.url().endsWith('/api/auth/refresh'))
  await page.getByRole('button', { name: 'Check session' }).click()
  expect((await refresh).status()).toBe(200)
  await expect(page.getByRole('status')).toHaveText('Session active.')

  const restored = page.waitForResponse((response) => response.url().endsWith('/api/auth/refresh'))
  await page.reload()
  expect((await restored).status()).toBe(200)
  await expect(page.getByRole('heading', { name: 'Welcome, Synthetic Admin' })).toBeVisible()

  await page.getByRole('link', { name: 'Property' }).click()
  await page.getByRole('button', { name: 'Delete ' + blockName }).click()
  await page.getByRole('button', { name: 'Delete', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('could not be changed')
  await page.getByRole('button', { name: 'Cancel' }).click()
  await page.getByRole('button', { name: 'Delete 10A' }).click()
  await page.getByRole('button', { name: 'Delete', exact: true }).click()
  await expect(page.getByRole('cell', { name: '10A', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: 'Delete ' + blockName }).click()
  await page.getByRole('button', { name: 'Delete', exact: true }).click()
  await expect(page.getByRole('cell', { name: blockName, exact: true })).toHaveCount(0)

  const logout = page.waitForResponse((response) => response.url().endsWith('/api/auth/logout'))
  await page.getByRole('button', { name: 'Sign out' }).click()
  expect((await logout).status()).toBe(204)
  expect((await page.context().cookies()).some(({ name }) => name === 'baiti_refresh')).toBe(false)
  await page.goto('/dashboard')
  await expect(page).toHaveURL(/\/login$/)
})
