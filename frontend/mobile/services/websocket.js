import { config } from '../config'

const WS_URL = config.wsUrl

class WebSocketService {
  constructor() {
    this.ws = null
    this.listeners = {}
    this.connected = false
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 10
    this.shouldReconnect = true
  }

  connect(token) {
    if (!token) return
    this.shouldReconnect = true
    this.ws = new WebSocket(`${WS_URL}?token=${token}`)

    this.ws.onopen = () => {
      this.connected = true
      this.reconnectAttempts = 0
      this.emit('connect')
    }

    this.ws.onclose = () => {
      this.connected = false
      this.emit('disconnect')
      if (this.shouldReconnect) this.scheduleReconnect()
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        this.emit(data.type, data.payload)
      } catch {}
    }
  }

  disconnect() {
    this.shouldReconnect = false
    if (this.ws) { this.ws.close(); this.ws = null }
    this.connected = false
  }

  send(type, payload = {}) {
    if (this.ws && this.connected) {
      this.ws.send(JSON.stringify({ type, payload }))
    }
  }

  on(event, callback) {
    if (!this.listeners[event]) this.listeners[event] = new Set()
    this.listeners[event].add(callback)
    return () => this.listeners[event]?.delete(callback)
  }

  emit(event, data) {
    const callbacks = this.listeners[event]
    if (callbacks) callbacks.forEach(cb => cb(data))
  }

  scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) return
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000)
    this.reconnectAttempts++
    setTimeout(() => { if (this.shouldReconnect) this.connect() }, delay)
  }

  joinRoom(roomId) { this.send('room:join', { room_id: roomId }) }
  leaveRoom(roomId) { this.send('room:leave', { room_id: roomId }) }
  sendMessage(roomId, content, messageType = 'text', replyToId = null) {
    this.send('message:send', { room_id: roomId, content, message_type: messageType, reply_to_id: replyToId })
  }
  startTyping(roomId) { this.send('typing:start', { room_id: roomId }) }
  stopTyping(roomId) { this.send('typing:stop', { room_id: roomId }) }
}

export default new WebSocketService()
