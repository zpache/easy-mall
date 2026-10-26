import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'

// vite 以 ESM 方式加载配置时，CJS 插件需要取一次 default
const uni = ((uniModule as any).default ?? uniModule) as typeof uniModule

export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // H5 调试时走代理，避免跨域；小程序端直接请求接口地址
      '/api': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true
      }
    }
  }
})
