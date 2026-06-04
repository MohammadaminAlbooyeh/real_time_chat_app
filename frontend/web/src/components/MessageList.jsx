import { useEffect, useRef, useCallback } from 'react'
import { useSelector } from 'react-redux'
import { useMessages } from '../hooks/useMessages'
import { formatMessageTime } from '../utils/formatters'
import { groupMessagesByDate } from '../utils/helpers'
import MessageReactions from './MessageReactions'

export default function MessageList({ roomId, onReply, editingMessage, setEditingMessage }) {
  const { messages, loading, loadMessages, loadMore, hasMore, delete: deleteMsg, react, unreact } = useMessages(roomId)
  const currentUserId = useSelector(state => state.auth.user?.id)
  const listRef = useRef(null)
  const bottomRef = useRef(null)
  const prevCountRef = useRef(0)
  const isLoadingMore = useRef(false)

  useEffect(() => {
    if (roomId) {
      loadMessages()
      isLoadingMore.current = false
    }
  }, [roomId, loadMessages])

  useEffect(() => {
    if (messages.length > prevCountRef.current) {
      if (!isLoadingMore.current) {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
      } else {
        isLoadingMore.current = false
      }
    }
    prevCountRef.current = messages.length
  }, [messages.length])

  const handleScroll = useCallback(() => {
    const el = listRef.current
    if (!el || !hasMore || loading) return
    if (el.scrollTop < 100) {
      isLoadingMore.current = true
      loadMore()
    }
  }, [hasMore, loading, loadMore])

  const grouped = groupMessagesByDate(messages)

  const handleEditClick = (msg) => {
    setEditingMessage(msg)
    onReply(null)
  }

  return (
    <div className="message-list" ref={listRef} onScroll={handleScroll}>
      {loading && messages.length === 0 && (
        <div className="loading-spinner" />
      )}
      {!loading && messages.length === 0 && (
        <div className="empty-state">No messages yet. Start the conversation!</div>
      )}
      {Object.entries(grouped).reverse().map(([date, msgs]) => (
        <div key={date}>
          <div className="date-divider">
            <span>{date === new Date().toLocaleDateString() ? 'Today' : date}</span>
          </div>
          {msgs.map(msg => (
            <div key={msg.id} className={`message ${msg.sender_id === 'system' ? 'message-system' : ''}`}>
              <div className="message-avatar">
                {msg.sender_avatar ? (
                  <img src={msg.sender_avatar} alt="" />
                ) : (
                  <span>{(msg.sender_username || '?')[0].toUpperCase()}</span>
                )}
              </div>
              <div className="message-body">
                <div className="message-header">
                  <span className="message-sender">{msg.sender_username || 'System'}</span>
                  <span className="message-time">{formatMessageTime(msg.created_at)}</span>
                </div>
                {msg.reply_preview && (
                  <div className="message-reply-preview">
                    <span>@{msg.reply_preview}</span>
                  </div>
                )}
                <div className="message-content">
                  {msg.is_deleted ? (
                    <em className="deleted">This message was deleted</em>
                  ) : (
                    msg.content
                  )}
                </div>
                {msg.attachments?.length > 0 && (
                  <div className="message-attachments">
                    {msg.attachments.map(att => (
                      <div key={att.id} className="attachment-item">
                        <a href={att.file_url} target="_blank" rel="noopener noreferrer">
                          {att.file_type === 'image' ? (
                            <img src={att.file_url} alt={att.file_name} className="attachment-image" />
                          ) : (
                            <span>{att.file_name}</span>
                          )}
                        </a>
                      </div>
                    ))}
                  </div>
                )}
                <MessageReactions
                  messageId={msg.id}
                  reactions={msg.reactions}
                  onReact={(emoji) => react(msg.id, emoji)}
                  onUnreact={(reactionId) => unreact(msg.id, reactionId)}
                  currentUserId={currentUserId}
                />
                {msg.edited_at && <span className="edited-indicator">(edited)</span>}
                {!msg.is_deleted && (
                  <div className="message-actions">
                    <button className="btn-icon btn-icon-sm" onClick={() => onReply(msg)} title="Reply">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                      </svg>
                    </button>
                    <button className="btn-icon btn-icon-sm" onClick={() => handleEditClick(msg)} title="Edit">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                      </svg>
                    </button>
                    <button className="btn-icon btn-icon-sm btn-icon-danger" onClick={() => {
                      if (window.confirm('Delete this message?')) deleteMsg(msg.id)
                    }} title="Delete">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="3 6 5 6 21 6" /><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                      </svg>
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      ))}
      <div ref={bottomRef} />
    </div>
  )
}
