import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://api-service:8000',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://websocket-service:8001',
        ws: true,
      },
      '/uploads': {
        target: 'http://api-service:8000',
        changeOrigin: true,
      },
    },
  },
})
