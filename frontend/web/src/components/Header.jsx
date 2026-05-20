import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useAuth } from '../hooks/useAuth'
import { getInitials } from '../utils/helpers'

export default function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [showMenu, setShowMenu] = useState(false)

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
