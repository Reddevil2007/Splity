import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // Local development: forward API calls to Spring Boot so the browser sees one
    // origin and no CORS (Cross-Origin Resource Sharing) setup is needed.
    // In Azure the frontend calls App Service directly (see src/api.js).
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
  },
})
