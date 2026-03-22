import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  base: '/admin/',  // Spring Boot에서 /admin/으로 서빙
  build: {
    outDir: '../app/src/main/resources/static/admin',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080'  // 개발 시 API 프록시
    }
  }
})
