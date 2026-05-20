export function isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

export function isValidUsername(username) {
  return /^[a-zA-Z0-9_]{3,50}$/.test(username)
}

export function isValidPassword(password) {
  return password.length >= 8
}

export function getErrorMessage(error) {
  if (typeof error === 'string') return error
  if (error?.response?.data?.detail) return error.response.data.detail
  if (error?.message) return error.message
  return 'An unexpected error occurred'
}
