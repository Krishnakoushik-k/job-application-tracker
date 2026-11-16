import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the browser talks to the Vite server (5173) and "/api" is
// forwarded to Spring Boot on 8080, so no CORS configuration is needed locally.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
    css: false,
  },
});
