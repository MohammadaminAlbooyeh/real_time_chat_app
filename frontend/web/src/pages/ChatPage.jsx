import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useSelector, useDispatch } from 'react-redux'
import { clearSearch } from '../store/chatSlice'
import RoomList from '../components/RoomList'
import ChatWindow from '../components/ChatWindow'

export default function ChatPage() {
  const { roomId } = useParams()
  const dispatch = useDispatch()
  const searchResults = useSelector(state => state.chat.searchResults)
  const searchLoading = useSelector(state => state.chat.searchLoading)

  return (
    <div className="chat-layout">
      <RoomList />
      <main className="chat-content">
        {searchResults.length > 0 && (
          <div className="search-results-overlay" onClick={() => dispatch(clearSearch())}>
            <div className="search-results-panel" onClick={e => e.stopPropagation()}>
              <div className="search-results-header">
                <h3>Search Results</h3>
                <button className="btn-icon" onClick={() => dispatch(clearSearch())}>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              </div>
              <div className="search-results-list">
                {searchResults.map(msg => (
                  <div key={msg.id} className="search-result-item">
                    <div className="search-result-sender">{msg.sender_username}</div>
                    <div className="search-result-content">{msg.content}</div>
                    <div className="search-result-time">{new Date(msg.created_at).toLocaleString()}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
        {searchLoading && (
          <div className="no-room-selected">
            <div className="loading-spinner" />
            <p>Searching...</p>
          </div>
        )}
        {roomId ? (
          <ChatWindow roomId={roomId} />
        ) : (
          <div className="no-room-selected">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#ccc" strokeWidth="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
            <h2>Select a room to start chatting</h2>
            <p>Choose a room from the sidebar or create a new one</p>
          </div>
        )}
      </main>
    </div>
  )
}
