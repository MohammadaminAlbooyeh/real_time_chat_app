import config from '../config'
import { getAccessToken } from './api'

class WebSocketService {
  constructor() {
    this.ws = null
    this.listeners = new Map()
    this.connected = false
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 10
    this.reconnectDelay = 1000
    this.shouldReconnect = true
  }

  connect() {
    const token = getAccessToken()
    if (!token) return

    this.shouldReconnect = true
    const url = `${config.wsUrl}?token=${token}`
    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      this.connected = true
      this.reconnectAttempts = 0
      this.emit('connect')
    }

    this.ws.onclose = () => {
      this.connected = false
      this.emit('disconnect')
      if (this.shouldReconnect) {
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = () => {
      this.connected = false
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        this.emit(data.type, data.payload)
      } catch {
        // ignore malformed messages
      }
    }
  }

  disconnect() {
    this.shouldReconnect = false
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.connected = false
  }

  send(type, payload = {}) {
    if (this.ws && this.connected) {
      this.ws.send(JSON.stringify({ type, payload }))
    }
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())
    }
    this.listeners.get(event).add(callback)
    return () => this.listeners.get(event)?.delete(callback)
  }

  emit(event, data) {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      callbacks.forEach(cb => cb(data))
    }
    const wildcardCallbacks = this.listeners.get('*')
    if (wildcardCallbacks) {
      wildcardCallbacks.forEach(cb => cb(event, data))
    }
  }

  scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) return
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts)
    this.reconnectAttempts++
    setTimeout(() => {
      if (this.shouldReconnect) {
        this.connect()
      }
    }, Math.min(delay, 30000))
  }

  joinRoom(roomId) {
    this.send('room:join', { room_id: roomId })
  }

  leaveRoom(roomId) {
    this.send('room:leave', { room_id: roomId })
  }

  sendMessage(roomId, content, messageType = 'text', replyToId = null) {
    this.send('message:send', {
      room_id: roomId,
      content,
      message_type: messageType,
      reply_to_id: replyToId,
    })
  }

  startTyping(roomId) {
    this.send('typing:start', { room_id: roomId })
  }

  stopTyping(roomId) {
    this.send('typing:stop', { room_id: roomId })
  }
}

const wsService = new WebSocketService()
export default wsService
