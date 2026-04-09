let clearSessionHandler = null

export function registerClearSessionHandler(handler) {
  clearSessionHandler = handler
}

export function clearAuthSession() {
  if (typeof clearSessionHandler === 'function') {
    clearSessionHandler()
    return
  }

  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

export function readStoredToken() {
  return localStorage.getItem('token') || ''
}

export function readStoredUserInfo() {
  return JSON.parse(localStorage.getItem('userInfo') || 'null')
}

export function persistAuthSession(token, userInfo) {
  localStorage.setItem('token', token)
  localStorage.setItem('userInfo', JSON.stringify(userInfo))
}
