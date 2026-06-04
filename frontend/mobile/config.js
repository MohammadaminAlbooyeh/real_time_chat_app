const ENV = {
  development: {
    API_URL: 'http://localhost:8000/api/v1',
    WS_URL: 'ws://localhost:8001/ws',
  },
  staging: {
    API_URL: 'https://staging-api.chatapp.com/api/v1',
    WS_URL: 'wss://staging-ws.chatapp.com/ws',
  },
  production: {
    API_URL: 'https://api.chatapp.com/api/v1',
    WS_URL: 'wss://ws.chatapp.com/ws',
  },
}

const currentEnv = __DEV__ ? 'development' : 'production'

export const config = {
  apiUrl: ENV[currentEnv].API_URL,
  wsUrl: ENV[currentEnv].WS_URL,
  maxReconnectAttempts: 10,
  reconnectBaseDelay: 1000,
  typingDebounceMs: 2000,
}
