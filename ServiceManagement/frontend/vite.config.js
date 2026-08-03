import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: process.env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
                changeOrigin: true
            },
            '/ws': {
                target: process.env.VITE_API_PROXY_TARGET || 'http://localhost:8080',
                ws: true,
                changeOrigin: true
            }
        }
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks: {
                    charts: ['recharts'],
                    realtime: ['@stomp/stompjs', 'sockjs-client'],
                    vendor: ['react', 'react-dom', 'react-router-dom', 'axios']
                }
            }
        }
    }
});
