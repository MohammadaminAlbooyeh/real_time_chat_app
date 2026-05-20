import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useRoom } from '../hooks/useRoom'
import { formatDate, truncate } from '../utils/helpers'

export default function RoomList() {
  const { roomId } = useParams()
  const navigate = useNavigate()
  const { rooms, loading, loadRooms, create } = useRoom()
  const [showCreate, setShowCreate] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [isPrivate, setIsPrivate] = useState(false)

  useEffect(() => {
    loadRooms()
  }, [loadRooms])

  const handleCreate = async (e) => {
    e.preventDefault()
    if (!name.trim()) return
    try {
      const room = await create({ name: name.trim(), description: description.trim() || undefined, is_private: isPrivate })
      if (room?.id) navigate(`/rooms/${room.id}`)
      setShowCreate(false)
      setName('')
      setDescription('')
      setIsPrivate(false)
    } catch { }
  }

  return (
    <aside className="room-list">
      <div className="room-list-header">
        <h2>Rooms</h2>
        <button className="btn-icon" onClick={() => setShowCreate(true)} title="Create Room">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="10" y1="4" x2="10" y2="16" /><line x1="4" y1="10" x2="16" y2="10" />
          </svg>
        </button>
      </div>

      {showCreate && (
        <form className="create-room-form" onSubmit={handleCreate}>
          <input
            type="text" placeholder="Room name" value={name}
            onChange={e => setName(e.target.value)} maxLength={100} autoFocus
          />
          <input
            type="text" placeholder="Description (optional)" value={description}
            onChange={e => setDescription(e.target.value)} maxLength={500}
          />
          <label className="checkbox-label">
            <input type="checkbox" checked={isPrivate} onChange={e => setIsPrivate(e.target.checked)} />
            Private room
          </label>
          <div className="form-actions">
            <button type="submit" className="btn btn-primary btn-sm">Create</button>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowCreate(false)}>Cancel</button>
          </div>
        </form>
      )}

      <div className="room-list-items">
        {loading && rooms.length === 0 ? (
          <div className="loading-spinner" />
        ) : rooms.length === 0 ? (
          <p className="empty-state">No rooms yet. Create one!</p>
        ) : (
          rooms.map(room => (
            <div
              key={room.id}
              className={`room-item ${room.id === roomId ? 'active' : ''}`}
              onClick={() => navigate(`/rooms/${room.id}`)}
            >
              <div className="room-item-avatar">
                {room.name.charAt(0).toUpperCase()}
              </div>
              <div className="room-item-info">
                <div className="room-item-name">{room.name}</div>
                <div className="room-item-preview">
                  {room.last_message ? truncate(room.last_message, 50) : 'No messages yet'}
                </div>
              </div>
              <div className="room-item-meta">
                <span className="room-item-time">
                  {room.last_message_at ? formatDate(room.last_message_at) : ''}
                </span>
                <span className="room-item-count">{room.member_count}</span>
              </div>
            </div>
          ))
        )}
      </div>
    </aside>
  )
}
