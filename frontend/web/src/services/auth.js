import { api, setTokens, loadTokens } from './api'

export async function register(username, email, password, displayName) {
  const data = await api.post('/auth/register', {
    username,
    email,
    password,
    display_name: displayName || undefined,
  })
  setTokens(data.access_token, data.refresh_token)
  return data.user
}

export async function login(email, password) {
  const data = await api.post('/auth/login', { email, password })
  setTokens(data.access_token, data.refresh_token)
  return data.user
}

export async function logout() {
  try {
    await api.post('/auth/logout')
  } catch {
    // ignore
  }
  setTokens(null, null)
}

export async function refreshSession() {
  const tokens = loadTokens()
  if (!tokens.refreshToken) return null
  try {
    const data = await api.post('/auth/refresh', {
      refresh_token: tokens.refreshToken,
    })
    setTokens(data.access_token, data.refresh_token)
    return data.user
  } catch {
    setTokens(null, null)
    return null
  }
}
