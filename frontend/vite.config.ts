import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, loadEnv } from 'vite'

export default defineConfig(({ mode }) => ({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': { target: loadEnv(mode, '.', 'BAITI_DEV_API').BAITI_DEV_API || 'http://localhost:8080' },
    },
  },
}))
