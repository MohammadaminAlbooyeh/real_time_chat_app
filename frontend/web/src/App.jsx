import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { useWebSocket } from './hooks/useWebSocket'
import Header from './components/Header'
import LoginPage from './pages/LoginPage'
import ChatPage from './pages/ChatPage'
import ProfilePage from './pages/ProfilePage'

function ProtectedRoute({ children }) {
  const { user, initialized } = useSelector(state => state.auth)
  useWebSocket()
  if (!initialized) return <div className="loading-page"><div className="loading-spinner" /></div>
  if (!user) return <Navigate to="/login" replace />
  return children
}

function PublicRoute({ children }) {
  const { user, initialized } = useSelector(state => state.auth)
  if (!initialized) return <div className="loading-page"><div className="loading-spinner" /></div>
  if (user) return <Navigate to="/" replace />
  return children
}

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
        <Route element={
          <ProtectedRoute>
            <div className="app-layout">
              <Header />
              <div className="app-content">
                <Routes>
                  <Route path="/" element={<ChatPage />} />
                  <Route path="/rooms/:roomId" element={<ChatPage />} />
                  <Route path="/profile" element={<ProfilePage />} />
                </Routes>
              </div>
            </div>
          </ProtectedRoute>
        } />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  )
}
