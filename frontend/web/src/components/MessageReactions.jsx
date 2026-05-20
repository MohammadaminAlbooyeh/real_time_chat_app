import { useState } from 'react'

const EMOJIS = ['👍', '❤️', '😂', '😮', '😢', '🙏']

export default function MessageReactions({ messageId, reactions, onReact, onUnreact, currentUserId }) {
  const [showPicker, setShowPicker] = useState(false)

  const userReacted = (emoji) => reactions?.some(r => r.user_id === currentUserId && r.emoji === emoji)
  const userReaction = (emoji) => reactions?.find(r => r.user_id === currentUserId && r.emoji === emoji)

  const handleClick = (emoji) => {
    if (userReacted(emoji)) {
      onUnreact(userReaction(emoji).id)
    } else {
      onReact(emoji)
    }
    setShowPicker(false)
  }

  return (
    <div className="message-reactions">
      {reactions && reactions.length > 0 && (
        <div className="reaction-list">
          {Object.entries(
            reactions.reduce((acc, r) => {
              acc[r.emoji] = (acc[r.emoji] || 0) + 1
              return acc
            }, {})
          ).map(([emoji, count]) => (
            <span
              key={emoji}
              className={`reaction-badge ${userReacted(emoji) ? 'reacted' : ''}`}
              onClick={() => handleClick(emoji)}
            >
              {emoji} {count}
            </span>
          ))}
        </div>
      )}
      <button className="btn-icon btn-icon-sm" onClick={() => setShowPicker(!showPicker)} title="Add reaction">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" /><path d="M8 14s1.5 2 4 2 4-2 4-2" /><line x1="9" y1="9" x2="9.01" y2="9" /><line x1="15" y1="9" x2="15.01" y2="9" />
        </svg>
      </button>
      {showPicker && (
        <div className="emoji-picker">
          {EMOJIS.map(emoji => (
            <button key={emoji} className="emoji-option" onClick={() => handleClick(emoji)}>
              {emoji}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
