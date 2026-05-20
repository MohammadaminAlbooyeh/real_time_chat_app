import config from '../config'

let accessToken = null
let refreshToken = null

export function setTokens(access, refresh) {
  accessToken = access
  refreshToken = refresh
  if (access) {
    localStorage.setItem('access_token', access)
    localStorage.setItem('refresh_token', refresh || '')
  } else {
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
  }
}

export function loadTokens() {
  accessToken = localStorage.getItem('access_token')
  refreshToken = localStorage.getItem('refresh_token')
  return { accessToken, refreshToken }
}

export function getAccessToken() {
  if (!accessToken) loadTokens()
  return accessToken
}

async function request(endpoint, options = {}) {
  const url = `${config.apiUrl}${endpoint}`
  const headers = { ...options.headers }

  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  const token = getAccessToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(url, {
    ...options,
    headers,
    body: options.body instanceof FormData ? options.body : options.body ? JSON.stringify(options.body) : undefined,
  })

  if (response.status === 401 && refreshToken) {
    const refreshed = await tryRefresh()
    if (refreshed) {
      headers['Authorization'] = `Bearer ${accessToken}`
      const retryResponse = await fetch(url, {
        ...options,
        headers,
        body: options.body instanceof FormData ? options.body : options.body ? JSON.stringify(options.body) : undefined,
      })
      return handleResponse(retryResponse)
    }
    setTokens(null, null)
    window.location.href = '/login'
    throw new Error('Session expired')
  }

  return handleResponse(response)
}

async function handleResponse(response) {
  if (response.status === 204) return null
  const data = await response.json()
  if (!response.ok) {
    const error = new Error(data.detail || 'Request failed')
    error.response = { data, status: response.status }
    throw error
  }
  return data
}

async function tryRefresh() {
  try {
    const response = await fetch(`${config.apiUrl}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
    })
    if (!response.ok) return false
    const data = await response.json()
    setTokens(data.access_token, data.refresh_token)
    return true
  } catch {
    return false
  }
}

export const api = {
  get: (endpoint, params) => {
    const query = params ? '?' + new URLSearchParams(params).toString() : ''
    return request(`${endpoint}${query}`)
  },
  post: (endpoint, body) => request(endpoint, { method: 'POST', body }),
  put: (endpoint, body) => request(endpoint, { method: 'PUT', body }),
  patch: (endpoint, body) => request(endpoint, { method: 'PATCH', body }),
  delete: (endpoint) => request(endpoint, { method: 'DELETE' }),
  upload: (endpoint, formData) =>
    request(endpoint, { method: 'POST', body: formData }),
}
