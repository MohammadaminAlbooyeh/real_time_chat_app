import { useState } from 'react'
import MessageList from './MessageList'
import MessageInput from './MessageInput'
import TypingIndicator from './TypingIndicator'
import UserList from './UserList'

export default function ChatWindow({ roomId }) {
  const [replyTo, setReplyTo] = useState(null)
  const [editingMessage, setEditingMessage] = useState(null)
  const [showUsers, setShowUsers] = useState(false)

  return (
    <div className="chat-window">
      <div className="chat-main">
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
