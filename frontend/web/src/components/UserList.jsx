import { useEffect } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { fetchRoom } from '../store/roomSlice'
import { getInitials } from '../utils/helpers'

export default function UserList({ roomId, onClose }) {
  const dispatch = useDispatch()
  const currentRoom = useSelector(state => state.rooms.currentRoom)

  useEffect(() => {
    if (roomId) dispatch(fetchRoom(roomId))
  }, [dispatch, roomId])

  const members = currentRoom?.members || []

  return (
    <aside className="user-list-panel">
      <div className="user-list-header">
        <h3>Members ({members.length})</h3>
        <button className="btn-icon" onClick={onClose}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
      <div className="user-list-items">
        {members.map(m => (
          <div key={m.id} className="user-list-item">
            <div className="avatar avatar-sm">
              {m.avatar_url ? (
                <img src={m.avatar_url} alt={m.display_name || m.username} />
              ) : (
                <span>{getInitials(m.display_name || m.username)}</span>
              )}
            </div>
            <div className="user-list-info">
              <span className="user-list-name">{m.display_name || m.username}</span>
              <span className="user-list-role">{m.role}</span>
            </div>
          </div>
        ))}
      </div>
    </aside>
  )
}
