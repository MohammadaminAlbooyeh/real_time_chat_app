import { HashRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import Header from '../../web/src/components/Header'
import LoginPage from '../../web/src/pages/LoginPage'
import ChatPage from '../../web/src/pages/ChatPage'
import ProfilePage from '../../web/src/pages/ProfilePage'

function ProtectedRoute({ children }) {
  const { user, initialized } = useSelector(state => state.auth)
  if (!initialized) return <div className="loading-page"><div className="loading-spinner" /></div>
  if (!user) return <Navigate to="/login" replace />
  return <div className="app-layout">{children}</div>
}

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<ProtectedRoute><Header /><ChatPage /></ProtectedRoute>} />
        <Route path="/rooms/:roomId" element={<ProtectedRoute><Header /><ChatPage /></ProtectedRoute>} />
        <Route path="/profile" element={<ProtectedRoute><Header /><ProfilePage /></ProtectedRoute>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  )
}
