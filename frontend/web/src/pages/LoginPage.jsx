import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function LoginPage() {
  const { login, register, loading, error } = useAuth()
  const navigate = useNavigate()
  const [isRegister, setIsRegister] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [username, setUsername] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [formError, setFormError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setFormError(null)

    try {
      if (isRegister) {
        await register(username, email, password, displayName)
      } else {
        await login(email, password)
      }
      navigate('/')
    } catch (err) {
      setFormError(err.message || 'Authentication failed')
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <svg width="48" height="48" viewBox="0 0 100 100" fill="none">
            <rect width="100" height="100" rx="20" fill="#4f46e5" />
            <text x="50" y="65" fontSize="50" textAnchor="middle" fill="white" fontFamily="Arial" fontWeight="bold">C</text>
          </svg>
          <h1>{isRegister ? 'Create Account' : 'Welcome Back'}</h1>
          <p>{isRegister ? 'Sign up to start chatting' : 'Sign in to your account'}</p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          {(error || formError) && (
            <div className="alert alert-error">{formError || error}</div>
          )}

          {isRegister && (
            <>
              <div className="form-group">
                <label>Username</label>
                <input
                  type="text" value={username} onChange={e => setUsername(e.target.value)}
                  required minLength={3} maxLength={50} placeholder="Choose a username"
                />
              </div>
              <div className="form-group">
                <label>Display Name (optional)</label>
                <input
                  type="text" value={displayName} onChange={e => setDisplayName(e.target.value)}
                  maxLength={100} placeholder="Your display name"
                />
              </div>
            </>
          )}

          <div className="form-group">
            <label>Email</label>
            <input
              type="email" value={email} onChange={e => setEmail(e.target.value)}
              required placeholder="you@example.com"
            />
          </div>

          <div className="form-group">
            <label>Password</label>
            <input
              type="password" value={password} onChange={e => setPassword(e.target.value)}
              required minLength={8} placeholder={isRegister ? 'At least 8 characters' : 'Your password'}
            />
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={loading}>
            {loading ? 'Please wait...' : isRegister ? 'Create Account' : 'Sign In'}
          </button>
        </form>

        <div className="auth-footer">
          {isRegister ? (
            <p>Already have an account? <Link to="#" onClick={() => { setIsRegister(false); setFormError(null) }}>Sign In</Link></p>
          ) : (
            <p>Don't have an account? <Link to="#" onClick={() => { setIsRegister(true); setFormError(null) }}>Create One</Link></p>
          )}
        </div>
      </div>
    </div>
  )
}
