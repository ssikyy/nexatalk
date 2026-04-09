import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('@tiptap')) return 'editor-tiptap'
          if (id.includes('md-editor-v3')) return 'editor-markdown'
          if (id.includes('emoji-picker-react')) return 'emoji-picker'
          if (id.includes('element-plus') || id.includes('@element-plus')) return 'element-plus'
          if (id.includes('@stomp/stompjs') || id.includes('sockjs-client')) return 'realtime'
        }
      }
    }
  },
  resolve: {
    alias: {
      // @ 指向 src 目录，方便模块引用
      '@': resolve(__dirname, 'src')
    }
  },
  define: {
    // 解决 sockjs-client 的 global 兼容性问题
    global: 'window'
  },
  server: {
    port: 3000,
    // 开发时代理 API 请求到后端，解决跨域
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // 静态上传文件（头像、背景图等）也由后端 Spring Boot 提供
      // 开发环境下需要一起代理，否则会从 3000 端口请求，导致图片始终加载失败
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // WebSocket 代理
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
