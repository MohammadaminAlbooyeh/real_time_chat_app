import { useState, useEffect } from 'react'
import wsService from '../services/websocket'

export default function TypingIndicator({ roomId }) {
  const [typingUsers, setTypingUsers] = useState({})

  useEffect(() => {
    const unsub = wsService.on('typing:indicator', (payload) => {
      if (payload.room_id !== roomId) return
      setTypingUsers(prev => {
        const next = { ...prev }
        if (payload.is_typing) {
          next[payload.user_id] = { username: payload.username, time: Date.now() }
        } else {
          delete next[payload.user_id]
        }
        return next
      })
    })

    const interval = setInterval(() => {
      setTypingUsers(prev => {
        const now = Date.now()
        const next = {}
        for (const [id, data] of Object.entries(prev)) {
          if (now - data.time < 3000) next[id] = data
        }
        return next
      })
    }, 1000)

    return () => { unsub(); clearInterval(interval) }
  }, [roomId])

  const names = Object.values(typingUsers).map(u => u.username)
  if (names.length === 0) return null

  let text = ''
  if (names.length === 1) text = `${names[0]} is typing`
  else if (names.length === 2) text = `${names[0]} and ${names[1]} are typing`
  else text = `${names[0]} and ${names.length - 1} others are typing`

  return (
    <div className="typing-indicator">
      <span className="typing-dots"><span>.</span><span>.</span><span>.</span></span>
      <span>{text}</span>
    </div>
  )
}
