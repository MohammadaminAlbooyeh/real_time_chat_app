import { useState, useCallback } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useSelector, useDispatch } from 'react-redux'
import { useAuth } from '../hooks/useAuth'
import { getInitials, debounce } from '../utils/helpers'
import { searchMessages, clearSearch } from '../store/chatSlice'
import { useParams } from 'react-router-dom'

export default function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const { roomId } = useParams()
  const [showMenu, setShowMenu] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')

  const debouncedSearch = useCallback(
    debounce((query) => {
      if (query.trim()) {
        dispatch(searchMessages({ query, roomId }))
      } else {
        dispatch(clearSearch())
      }
    }, 300),
    [dispatch, roomId]
  )

  const handleSearchChange = (e) => {
    const value = e.target.value
    setSearchQuery(value)
    debouncedSearch(value)
  }

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <header className="header">
      <div className="header-left">
        <Link to="/" className="header-logo">
          <svg width="28" height="28" viewBox="0 0 100 100" fill="none">
            <rect width="100" height="100" rx="20" fill="#4f46e5" />
            <text x="50" y="65" fontSize="50" textAnchor="middle" fill="white" fontFamily="Arial" fontWeight="bold">C</text>
          </svg>
          <span className="header-title">ChatApp</span>
        </Link>
        <div className="header-search">
          <svg className="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            className="search-input"
            placeholder="Search messages..."
            value={searchQuery}
            onChange={handleSearchChange}
          />
          {searchQuery && (
            <button className="search-clear" onClick={() => { setSearchQuery(''); dispatch(clearSearch()) }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          )}
        </div>
      </div>
      <div className="header-right">
        <div className="header-user" onClick={() => setShowMenu(!showMenu)}>
          <div className="avatar avatar-sm">
            {user?.avatar_url ? (
              <img src={user.avatar_url} alt={user.display_name || user.username} />
            ) : (
              <span>{getInitials(user?.display_name || user?.username)}</span>
            )}
          </div>
          <span className="header-username">{user?.display_name || user?.username}</span>
        </div>
        {showMenu && (
          <>
            <div className="dropdown-backdrop" onClick={() => setShowMenu(false)} />
            <div className="dropdown dropdown-right">
              <Link to="/profile" className="dropdown-item" onClick={() => setShowMenu(false)}>
                Profile Settings
              </Link>
              <hr className="dropdown-divider" />
              <button className="dropdown-item dropdown-item-danger" onClick={handleLogout}>
                Sign Out
              </button>
            </div>
          </>
        )}
      </div>
    </header>
  )
}
