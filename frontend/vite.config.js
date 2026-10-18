import { defineConfig } from 'vite'

export default defineConfig(() => {

  return {
    publicDir: './public',
    plugins: [],
    server: {
      port: 3000,
      proxy: {
        "/api": {
          target: "http://127.0.0.1:9000"
        },
        "/ws": {
          target: "http://127.0.0.1:9000",
          ws: true
        }
      }
    },
    base: "/static",
    build: {
      chunkSizeWarningLimit: 2000,
      rolldownOptions: {
        output: {
          codeSplitting: {
            groups: [
              {
                name: 'ui5',
                test: /node_modules[\\/]@ui5/
              }
            ]
          }
        }
      }
    }
  }
})