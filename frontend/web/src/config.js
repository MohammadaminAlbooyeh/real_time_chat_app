const config = {
  apiUrl: import.meta.env.VITE_API_URL || '/api/v1',
  wsUrl: import.meta.env.VITE_WS_URL || `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`,
  uploadUrl: import.meta.env.VITE_UPLOAD_URL || '/api/v1/upload',
}

export default config
