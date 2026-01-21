import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import {webfontDownload} from "vite-plugin-webfont-dl";

// https://vite.dev/config/
export default defineConfig({
  plugins: [
      react(),
      webfontDownload(),
  ],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // Points to your Spring Boot app
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
