import { useCallback, useEffect } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { loginUser, registerUser, clearAuth, setUser, updateProfile, changePassword } from '../store/authSlice'
import { refreshSession } from '../services/auth'
import { loadTokens } from '../services/api'
import wsService from '../services/websocket'

export function useAuth() {
  const dispatch = useDispatch()
  const { user, loading, error, initialized } = useSelector(state => state.auth)

  const login = useCallback((email, password) => dispatch(loginUser({ email, password })), [dispatch])
  const register = useCallback((username, email, password, displayName) =>
    dispatch(registerUser({ username, email, password, displayName })), [dispatch])

  const logout = useCallback(async () => {
    wsService.disconnect()
    dispatch(clearAuth())
  }, [dispatch])

  useEffect(() => {
    const tokens = loadTokens()
    if (tokens.accessToken) {
      refreshSession().then(user => {
        if (user) {
          dispatch(setUser(user))
          wsService.connect()
        }
      })
    } else {
      dispatch(clearAuth())
    }
  }, [dispatch])

  return {
    user,
    loading,
    error,
    initialized,
    isAuthenticated: !!user,
    login,
    register,
    logout,
    updateProfile: (data) => dispatch(updateProfile(data)),
    changePassword: (data) => dispatch(changePassword(data)),
  }
}
