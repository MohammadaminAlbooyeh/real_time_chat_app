import { useParams } from 'react-router-dom'
import RoomList from '../components/RoomList'
import ChatWindow from '../components/ChatWindow'

export default function ChatPage() {
  const { roomId } = useParams()

  return (
    <div className="chat-layout">
      <RoomList />
      <main className="chat-content">
        {roomId ? (
          <ChatWindow roomId={roomId} />
        ) : (
          <div className="no-room-selected">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#ccc" strokeWidth="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
            </svg>
            <h2>Select a room to start chatting</h2>
            <p>Choose a room from the sidebar or create a new one</p>
          </div>
        )}
      </main>
    </div>
  )
}
