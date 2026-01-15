import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 로컬 환경변수 GOOGLE_MAPS_API_KEY를 Vite 환경변수로 주입
  // .env 파일의 VITE_GOOGLE_MAPS_API_KEY 또는 시스템 환경변수 GOOGLE_MAPS_API_KEY 사용
  const googleMapsApiKey = process.env.GOOGLE_MAPS_API_KEY || process.env.VITE_GOOGLE_MAPS_API_KEY || ''

  return {
    plugins: [
      vue(),
      vueDevTools(),
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    define: {
      'import.meta.env.VITE_GOOGLE_MAPS_API_KEY': JSON.stringify(googleMapsApiKey)
    }
  }
})
