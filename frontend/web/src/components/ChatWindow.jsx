import { useState, useEffect } from 'react'
import { useWebSocket } from '../hooks/useWebSocket'
import MessageList from './MessageList'
import MessageInput from './MessageInput'
import TypingIndicator from './TypingIndicator'
import UserList from './UserList'

export default function ChatWindow({ roomId }) {
  const { joinRoom, leaveRoom } = useWebSocket()
  const [replyTo, setReplyTo] = useState(null)
  const [editingMessage, setEditingMessage] = useState(null)
  const [showUsers, setShowUsers] = useState(false)

  useEffect(() => {
    if (roomId) {
      joinRoom(roomId)
      return () => leaveRoom(roomId)
    }
  }, [roomId, joinRoom, leaveRoom])

  return (
    <div className="chat-window">
      <div className="chat-main">
        <div className="chat-room-header">
          <span className="chat-room-name">{roomId ? `Room` : ''}</span>
          <button className="btn-icon" onClick={() => setShowUsers(!showUsers)} title="Toggle members">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </svg>
          </button>
        </div>
        <MessageList
          roomId={roomId}
          onReply={setReplyTo}
          editingMessage={editingMessage}
          setEditingMessage={setEditingMessage}
        />
        <TypingIndicator roomId={roomId} />
        <MessageInput
          roomId={roomId}
          replyTo={replyTo}
          onCancelReply={() => setReplyTo(null)}
          editingMessage={editingMessage}
          onCancelEdit={() => setEditingMessage(null)}
        />
      </div>
      {showUsers && (
        <UserList roomId={roomId} onClose={() => setShowUsers(false)} />
      )}
    </div>
  )
}
