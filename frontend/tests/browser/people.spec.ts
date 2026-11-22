import { readFileSync } from 'node:fs'
import { randomUUID } from 'node:crypto'
import { expect, test, type Page } from '@playwright/test'

type Fixture = {
  adminEmail: string; initialPassword: string; prefix: string; otherRoleName: string; roleName: string
}

async function signIn(page: Page, email: string, password: string) {
  await page.getByLabel('Community code').fill('baiti-foundation-a')
  await page.getByLabel('Email address').fill(email)
  await page.getByLabel('Password', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()
}

test('user administration creates first-use accounts, changes roles and revokes access', async ({ page, browser }) => {
  const path = process.env.BAITI_PEOPLE_BROWSER_FIXTURE
  if (!path) throw new Error('Provide an owned synthetic People browser fixture')
  const fixture = JSON.parse(readFileSync(path, 'utf8')) as Fixture
  const replacement = 'Synthetic-' + randomUUID()
  await page.goto('/login')
  await signIn(page, fixture.adminEmail, fixture.initialPassword)
  await expect(page).toHaveURL(/\/password$/)
  await page.getByLabel('Current password', { exact: true }).fill(fixture.initialPassword)
  await page.getByLabel('New password', { exact: true }).fill(replacement)
  await page.getByLabel('Confirm new password', { exact: true }).fill(replacement)
  await page.getByRole('button', { name: 'Change password' }).click()
  await expect(page).toHaveURL(/\/login$/)
  await signIn(page, fixture.adminEmail, replacement)
  await page.getByRole('link', { name: 'Users', exact: true }).click()
  const add = page.getByRole('button', { name: 'Add user', exact: true })
  await add.click()
  await page.keyboard.press('Escape')
  await expect(add).toBeFocused()
  await add.click()
  await page.getByRole('button', { name: 'Create account' }).click()
  await expect(page.getByRole('textbox', { name: 'Name', exact: true })).toHaveAccessibleDescription('Enter a value.')
  const email = 'p09-resident-' + randomUUID() + '@example.invalid'
  const name = fixture.prefix + ' Resident'
  await page.getByLabel('Name', { exact: true }).fill(name)
  await page.getByLabel('Email address', { exact: true }).fill(email)
  await page.getByRole('combobox', { name: 'Role', exact: true }).selectOption({ label: fixture.otherRoleName })
  await page.getByRole('button', { name: 'Create account' }).click()
  const password = await page.getByLabel('Temporary password').inputValue()
  await expect(page.getByRole('cell', { name: email, exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Dismiss password' }).click()
  await expect(page.getByLabel('Temporary password')).toHaveCount(0)

  const other = await browser.newContext()
  try {
    const resident = await other.newPage()
    await resident.goto('http://localhost:5173/login')
    await signIn(resident, email, password)
    await expect(resident).toHaveURL(/\/password$/)
    await resident.getByLabel('Current password', { exact: true }).fill(password)
    await resident.getByLabel('New password', { exact: true }).fill(replacement)
    await resident.getByLabel('Confirm new password', { exact: true }).fill(replacement)
    await resident.getByRole('button', { name: 'Change password' }).click()
    await expect(resident).toHaveURL(/\/login$/)
    const login = resident.waitForResponse((response) => response.url().endsWith('/api/auth/login') && response.ok())
    await signIn(resident, email, replacement)
    const auth = await (await login).json() as { accessToken: string }
    await expect(resident.getByRole('heading', { name: 'Welcome, ' + name })).toBeVisible()
    await expect(resident.getByRole('link', { name: 'Users', exact: true })).toHaveCount(0)
    const denied = await resident.request.get('http://localhost:5173/api/users', { headers: {
      Authorization: 'Bearer ' + auth.accessToken, 'X-Tenant-Slug': 'baiti-foundation-a',
    } })
    expect(denied.status()).toBe(403)

    await page.getByRole('button', { name: 'Change role for ' + name, exact: true }).click()
    await page.getByRole('combobox', { name: 'Role', exact: true }).selectOption({ label: fixture.roleName })
    await page.getByRole('button', { name: 'Save', exact: true }).click()
    const row = page.getByRole('row').filter({ has: page.getByRole('cell', { name: email, exact: true }) })
    await expect(row.getByRole('cell', { name: fixture.roleName, exact: true })).toBeVisible()
    await page.getByRole('button', { name: 'Deactivate ' + name, exact: true }).click()
    await page.getByRole('button', { name: 'Deactivate', exact: true }).click()
    await expect(row.getByRole('cell', { name: 'Inactive', exact: true })).toBeVisible()
    await resident.reload()
    await expect(resident).toHaveURL(/\/login$/)
    await signIn(resident, email, replacement)
    await expect(resident.getByRole('alert')).toHaveText('Check your community code, email and password.')
  } finally {
    await other.close()
  }
  await page.setViewportSize({ width: 375, height: 812 })
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await expect(page.getByRole('heading', { name: 'Users', exact: true })).toBeVisible()
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
})
