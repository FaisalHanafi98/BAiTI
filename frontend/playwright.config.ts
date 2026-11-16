import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests/browser',
  workers: 1,
  timeout: 90_000,
  use: { baseURL: 'http://localhost:5173', channel: 'chrome', actionTimeout: 10_000 },
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: false,
    env: { BAITI_DEV_API: process.env.BAITI_DEV_API || 'http://127.0.0.1:18080' },
  },
})
