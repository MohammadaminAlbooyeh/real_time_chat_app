import React, { useEffect, useState, useRef, useCallback } from 'react'
import { View, Text, FlatList, StyleSheet, KeyboardAvoidingView, Platform, Alert } from 'react-native'
import { api } from '../services/api'
import wsService from '../services/websocket'
import ChatHeader from '../components/ChatHeader'
import InputBar from '../components/InputBar'
import MessageBubble from '../components/MessageBubble'
import { config } from '../config'

export default function ChatScreen({ route, navigation }) {
  const { roomId, roomName } = route.params
  const [messages, setMessages] = useState([])
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(true)
  const [typingUsers, setTypingUsers] = useState({})
  const [currentUserId, setCurrentUserId] = useState(null)
  const flatRef = useRef(null)
  const typingTimerRef = useRef(null)

  useEffect(() => {
    navigation.setOptions({ title: roomName })
    loadMessages()
    wsService.joinRoom(roomId)

    api.get('/users/me').then(user => setCurrentUserId(user.id)).catch(() => {})

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
      if (typingTimerRef.current) clearTimeout(typingTimerRef.current)
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

  const handleSend = useCallback(() => {
    if (!text.trim()) return
    wsService.sendMessage(roomId, text.trim())
    wsService.stopTyping(roomId)
    setText('')
    if (typingTimerRef.current) clearTimeout(typingTimerRef.current)
  }, [text, roomId])

  const handleChangeText = useCallback((value) => {
    setText(value)
    wsService.startTyping(roomId)
    if (typingTimerRef.current) clearTimeout(typingTimerRef.current)
    typingTimerRef.current = setTimeout(() => {
      wsService.stopTyping(roomId)
    }, config.typingDebounceMs)
  }, [roomId])

  const typingNames = Object.values(typingUsers).map(u => u.username)
  const typingText = typingNames.length === 1 ? `${typingNames[0]} is typing`
    : typingNames.length === 2 ? `${typingNames[0]} and ${typingNames[1]} are typing`
    : typingNames.length > 2 ? `${typingNames[0]} and ${typingNames.length - 1} others are typing`
    : ''

  return (
    <KeyboardAvoidingView style={styles.container} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}>
      <ChatHeader
        title={roomName}
        onBack={() => navigation.goBack()}
        onProfile={() => navigation.navigate('Profile')}
      />
      {loading ? (
        <View style={styles.loading} />
      ) : (
        <FlatList
          ref={flatRef}
          data={messages}
          keyExtractor={item => item.id}
          inverted
          renderItem={({ item }) => (
            <MessageBubble message={item} isOwn={item.sender_id === currentUserId} />
          )}
          contentContainerStyle={styles.listContent}
        />
      )}

      {typingText ? <Text style={styles.typing}>{typingText}</Text> : null}

      <InputBar value={text} onChangeText={handleChangeText} onSend={handleSend} />
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8fafc' },
  loading: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  listContent: { padding: 12 },
  typing: { paddingHorizontal: 16, paddingVertical: 4, fontSize: 12, color: '#64748b', fontStyle: 'italic' },
})
