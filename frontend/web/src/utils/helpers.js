export function classNames(...classes) {
  return classes.filter(Boolean).join(' ')
}

export function truncate(text, maxLength = 100) {
  if (text.length <= maxLength) return text
  return text.slice(0, maxLength - 3) + '...'
}

export function getInitials(name) {
  if (!name) return '?'
  return name
    .split(' ')
    .map(part => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

export function groupMessagesByDate(messages) {
  const groups = {}
  for (const msg of messages) {
    const date = new Date(msg.created_at).toLocaleDateString()
    if (!groups[date]) groups[date] = []
    groups[date].push(msg)
  }
  return groups
}

export function isImageFile(fileType) {
  return ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(fileType)
}

export function debounce(func, wait) {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}
