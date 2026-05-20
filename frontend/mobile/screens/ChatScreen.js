import React, { useEffect, useState, useRef } from 'react'
import { View, Text, FlatList, TextInput, TouchableOpacity, StyleSheet, KeyboardAvoidingView, Platform, Alert } from 'react-native'
import { api } from '../services/api'
import wsService from '../services/websocket'

export default function ChatScreen({ route, navigation }) {
  const { roomId, roomName } = route.params
  const [messages, setMessages] = useState([])
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(true)
  const [typingUsers, setTypingUsers] = useState({})
  const flatRef = useRef(null)

  useEffect(() => {
    navigation.setOptions({ title: roomName })
    loadMessages()
    wsService.joinRoom(roomId)

    const unsub1 = wsService.on('message:new', (payload) => {
      if (payload.room_id === roomId) {
        setMessages(prev => [payload, ...prev])
      }
    })
    const unsub2 = wsService.on('typing:indicator', (payload) => {
      if (payload.room_id !== roomId) return
      setTypingUsers(prev => {
        const next = { ...prev }
        if (payload.is_typing) next[payload.user_id] = { username: payload.username, time: Date.now() }
        else delete next[payload.user_id]
        return next
      })
    })

    const interval = setInterval(() => {
      setTypingUsers(prev => {
        const now = Date.now()
        const next = {}
        for (const [id, data] of Object.entries(prev)) {
          if (now - data.time < 3000) next[id] = data
        }
        return next
      })
    }, 1000)

    return () => {
      wsService.leaveRoom(roomId)
      unsub1()
      unsub2()
      clearInterval(interval)
    }
  }, [roomId])

  const loadMessages = async () => {
    try {
      const data = await api.get(`/rooms/${roomId}/messages`)
      setMessages(data.items || [])
    } catch (err) {
      Alert.alert('Error', err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleSend = () => {
    if (!text.trim()) return
    wsService.sendMessage(roomId, text.trim())
    wsService.stopTyping(roomId)
    setText('')
  }

  const typingNames = Object.values(typingUsers).map(u => u.username)
  const typingText = typingNames.length === 1 ? `${typingNames[0]} is typing`
    : typingNames.length === 2 ? `${typingNames[0]} and ${typingNames[1]} are typing`
    : typingNames.length > 2 ? `${typingNames[0]} and ${typingNames.length - 1} others are typing`
    : ''

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}>
      {loading ? (
        <View style={styles.loading} />
      ) : (
        <FlatList
          ref={flatRef}
          data={messages}
          keyExtractor={item => item.id}
          inverted
          renderItem={({ item }) => (
            <View style={styles.message}>
              <Text style={styles.sender}>{item.sender_username || 'System'}</Text>
              <Text style={styles.content}>{item.is_deleted ? 'This message was deleted' : item.content}</Text>
              <Text style={styles.time}>{new Date(item.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</Text>
            </View>
          )}
          contentContainerStyle={styles.listContent}
        />
      )}

      {typingText ? <Text style={styles.typing}>{typingText}</Text> : null}

      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          value={text}
          onChangeText={v => { setText(v); wsService.startTyping(roomId) }}
          placeholder="Type a message..."
          multiline
        />
        <TouchableOpacity style={styles.sendButton} onPress={handleSend} disabled={!text.trim()}>
          <Text style={styles.sendText}>Send</Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8fafc' },
  loading: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  listContent: { padding: 12 },
  message: { backgroundColor: '#fff', borderRadius: 10, padding: 12, marginBottom: 8 },
  sender: { fontSize: 12, fontWeight: '600', color: '#4f46e5', marginBottom: 2 },
  content: { fontSize: 15, color: '#0f172a' },
  time: { fontSize: 11, color: '#94a3b8', marginTop: 4, textAlign: 'right' },
  typing: { paddingHorizontal: 16, paddingVertical: 4, fontSize: 12, color: '#64748b', fontStyle: 'italic' },
  inputContainer: { flexDirection: 'row', padding: 12, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: '#e2e8f0', alignItems: 'flex-end' },
  input: { flex: 1, borderWidth: 1, borderColor: '#e2e8f0', borderRadius: 20, paddingHorizontal: 16, paddingVertical: 8, fontSize: 15, maxHeight: 100, backgroundColor: '#f8fafc' },
  sendButton: { backgroundColor: '#4f46e5', borderRadius: 20, paddingHorizontal: 16, paddingVertical: 10, marginLeft: 8 },
  sendText: { color: '#fff', fontWeight: '600' },
})
