import React, { useState, useEffect } from 'react'
import { StatusBar } from 'expo-status-bar'
import RootNavigator from './navigation/RootNavigator'
import { loadTokens, getAccessToken } from './services/api'
import wsService from './services/websocket'

export default function App() {
  const [user, setUser] = useState(null)

  useEffect(() => {
    loadTokens().then(() => {
      const token = getAccessToken()
      if (token) {
        wsService.connect(token)
        import('./services/api').then(({ api }) => {
          api.get('/users/me').then(setUser).catch(() => setUser(null))
        })
      }
    })
  }, [])

  const handleLogin = (userData) => {
    setUser(userData)
    const token = getAccessToken()
    if (token) wsService.connect(token)
  }

  const handleLogout = () => {
    wsService.disconnect()
    setUser(null)
  }

  return (
    <>
      <StatusBar style="dark" />
      <RootNavigator user={user} onLogin={handleLogin} onLogout={handleLogout} />
    </>
  )
}
