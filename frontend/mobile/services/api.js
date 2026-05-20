import * as SecureStore from 'expo-secure-store'

const API_URL = 'http://localhost:8000/api/v1'

let accessToken = null
let refreshToken = null

export async function setTokens(access, refresh) {
  accessToken = access
  refreshToken = refresh
  if (access) {
    await SecureStore.setItemAsync('access_token', access)
    await SecureStore.setItemAsync('refresh_token', refresh || '')
  } else {
    await SecureStore.deleteItemAsync('access_token')
    await SecureStore.deleteItemAsync('refresh_token')
  }
}

export async function loadTokens() {
  accessToken = await SecureStore.getItemAsync('access_token')
  refreshToken = await SecureStore.getItemAsync('refresh_token')
  return { accessToken, refreshToken }
}

export function getAccessToken() {
  return accessToken
}

async function request(endpoint, options = {}) {
  const url = `${API_URL}${endpoint}`
  const headers = { ...options.headers }
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  const response = await fetch(url, {
    ...options,
    headers,
    body: options.body instanceof FormData ? options.body : options.body ? JSON.stringify(options.body) : undefined,
  })

  if (response.status === 401 && refreshToken) {
    const refreshResp = await fetch(`${API_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
    })
    if (refreshResp.ok) {
      const data = await refreshResp.json()
      await setTokens(data.access_token, data.refresh_token)
      headers['Authorization'] = `Bearer ${accessToken}`
      const retry = await fetch(url, {
        ...options,
        headers,
        body: options.body instanceof FormData ? options.body : options.body ? JSON.stringify(options.body) : undefined,
      })
      return handleResponse(retry)
    }
    await setTokens(null, null)
    throw new Error('Session expired')
  }

  return handleResponse(response)
}

async function handleResponse(response) {
  if (response.status === 204) return null
  const data = await response.json()
  if (!response.ok) {
    throw new Error(data.detail || 'Request failed')
  }
  return data
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
  upload: (endpoint, formData) => request(endpoint, { method: 'POST', body: formData }),
}
