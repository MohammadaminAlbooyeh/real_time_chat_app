import { useState, useRef, useEffect, useCallback } from 'react'
import { MAX_MESSAGE_LENGTH } from '../utils/constants'
import { useWebSocket } from '../hooks/useWebSocket'
import { useMessages } from '../hooks/useMessages'
import FileUpload from './FileUpload'

export default function MessageInput({ roomId, replyTo, onCancelReply, editingMessage, onCancelEdit }) {
  const { sendMessage: wsSend, startTyping, stopTyping } = useWebSocket()
  const { send: apiSend, edit: editMsg } = useMessages(roomId)
  const [text, setText] = useState('')
  const [showUpload, setShowUpload] = useState(false)
  const inputRef = useRef(null)
  const typingTimeout = useRef(null)

  useEffect(() => {
    if (editingMessage) {
      setText(editingMessage.content)
      inputRef.current?.focus()
    }
  }, [editingMessage])

  useEffect(() => {
    inputRef.current?.focus()
  }, [roomId, replyTo])

  const handleTyping = useCallback(() => {
    startTyping(roomId)
    if (typingTimeout.current) clearTimeout(typingTimeout.current)
    typingTimeout.current = setTimeout(() => stopTyping(roomId), 2000)
  }, [roomId, startTyping, stopTyping])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!text.trim()) return

    if (editingMessage) {
      await editMsg(editingMessage.id, text.trim())
      onCancelEdit()
    } else if (replyTo) {
      wsSend(roomId, text.trim(), 'text', replyTo.id)
    } else {
      wsSend(roomId, text.trim())
    }

    setText('')
    if (typingTimeout.current) {
      clearTimeout(typingTimeout.current)
      stopTyping(roomId)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit(e)
    }
    if (e.key === 'Escape') {
      if (editingMessage) onCancelEdit()
      else if (replyTo) onCancelReply()
    }
  }

  const handleFileAttached = (fileData) => {
    apiSend(roomId, `Sent a file: ${fileData.file_name}`)
  }

  return (
    <div className="message-input-container">
      {replyTo && (
        <div className="reply-preview">
          <span>Replying to <strong>{replyTo.sender_username}</strong>: {replyTo.content.slice(0, 80)}</span>
          <button className="btn-icon btn-icon-sm" onClick={onCancelReply}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
      )}
      {editingMessage && (
        <div className="reply-preview editing-preview">
          <span>Editing message</span>
          <button className="btn-icon btn-icon-sm" onClick={onCancelEdit}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
      )}
      <form className="message-input-form" onSubmit={handleSubmit}>
        <button type="button" className="btn-icon" onClick={() => setShowUpload(!showUpload)} title="Attach file">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
          </svg>
        </button>
        <input
          ref={inputRef}
          className="message-input"
          type="text"
          placeholder={replyTo ? 'Reply...' : editingMessage ? 'Edit message...' : 'Type a message...'}
          value={text}
          onChange={e => { setText(e.target.value); handleTyping() }}
          onKeyDown={handleKeyDown}
          maxLength={MAX_MESSAGE_LENGTH}
        />
        <button type="submit" className="btn-send" disabled={!text.trim()}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="22" y1="2" x2="11" y2="13" /><polygon points="22 2 15 22 11 13 2 9 22 2" />
          </svg>
        </button>
      </form>
      {showUpload && <FileUpload roomId={roomId} onFileAttached={handleFileAttached} onClose={() => setShowUpload(false)} />}
    </div>
  )
}
