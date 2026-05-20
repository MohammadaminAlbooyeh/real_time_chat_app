import { useState } from 'react'
import { useSelector } from 'react-redux'
import { useAuth } from '../hooks/useAuth'
import { getInitials, formatFullDate } from '../utils/helpers'

export default function ProfilePage() {
  const user = useSelector(state => state.auth.user)
  const { updateProfile, changePassword, logout } = useAuth()
  const [displayName, setDisplayName] = useState(user?.display_name || '')
  const [bio, setBio] = useState(user?.bio || '')
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState(null)

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [changingPassword, setChangingPassword] = useState(false)
  const [passwordMessage, setPasswordMessage] = useState(null)

  const handleProfileUpdate = async (e) => {
    e.preventDefault()
    setSaving(true)
    setMessage(null)
    try {
      await updateProfile({ display_name: displayName || undefined, bio: bio || undefined })
      setMessage({ type: 'success', text: 'Profile updated' })
    } catch (err) {
      setMessage({ type: 'error', text: err.message })
    } finally {
      setSaving(false)
    }
  }

  const handlePasswordChange = async (e) => {
    e.preventDefault()
    setChangingPassword(true)
    setPasswordMessage(null)
    try {
      await changePassword({ current_password: currentPassword, new_password: newPassword })
      setPasswordMessage({ type: 'success', text: 'Password changed' })
      setCurrentPassword('')
      setNewPassword('')
    } catch (err) {
      setPasswordMessage({ type: 'error', text: err.message })
    } finally {
      setChangingPassword(false)
    }
  }

  if (!user) return null

  return (
    <div className="profile-page">
      <div className="profile-card">
        <div className="profile-avatar-section">
          <div className="avatar avatar-lg">
            {user.avatar_url ? (
              <img src={user.avatar_url} alt={user.display_name || user.username} />
            ) : (
              <span>{getInitials(user.display_name || user.username)}</span>
            )}
          </div>
          <h2>{user.display_name || user.username}</h2>
          <p className="text-muted">@{user.username}</p>
          <p className="text-muted">Joined {formatFullDate(user.created_at)}</p>
        </div>

        <form onSubmit={handleProfileUpdate} className="profile-form">
          {message && (
            <div className={`alert alert-${message.type}`}>{message.text}</div>
          )}

          <div className="form-group">
            <label>Display Name</label>
            <input
              type="text" value={displayName} onChange={e => setDisplayName(e.target.value)}
              maxLength={100}
            />
          </div>

          <div className="form-group">
            <label>Bio</label>
            <textarea
              value={bio} onChange={e => setBio(e.target.value)}
              maxLength={500} rows={3}
            />
          </div>

          <div className="form-group">
            <label>Email</label>
            <input type="email" value={user.email} disabled />
          </div>

          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving...' : 'Save Profile'}
          </button>
        </form>

        <hr className="divider" />

        <form onSubmit={handlePasswordChange} className="profile-form">
          <h3>Change Password</h3>
          {passwordMessage && (
            <div className={`alert alert-${passwordMessage.type}`}>{passwordMessage.text}</div>
          )}

          <div className="form-group">
            <label>Current Password</label>
            <input
              type="password" value={currentPassword}
              onChange={e => setCurrentPassword(e.target.value)} required
            />
          </div>

          <div className="form-group">
            <label>New Password</label>
            <input
              type="password" value={newPassword}
              onChange={e => setNewPassword(e.target.value)}
              required minLength={8}
            />
          </div>

          <button type="submit" className="btn btn-primary" disabled={changingPassword}>
            {changingPassword ? 'Changing...' : 'Change Password'}
          </button>
        </form>
      </div>
    </div>
  )
}
