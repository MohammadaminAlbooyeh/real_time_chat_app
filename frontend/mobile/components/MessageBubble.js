import React from 'react'
import { View, Text, StyleSheet } from 'react-native'

export default function MessageBubble({ message, isOwn }) {
  return (
    <View style={[styles.container, isOwn && styles.own]}>
      {!isOwn && <Text style={styles.sender}>{message.sender_username}</Text>}
      <Text style={styles.content}>{message.is_deleted ? 'This message was deleted' : message.content}</Text>
      <Text style={styles.time}>
        {new Date(message.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
      </Text>
    </View>
  )
}

const styles = StyleSheet.create({
  container: { backgroundColor: '#fff', borderRadius: 10, padding: 10, marginBottom: 6, maxWidth: '80%', alignSelf: 'flex-start' },
  own: { alignSelf: 'flex-end', backgroundColor: '#eef2ff' },
  sender: { fontSize: 11, fontWeight: '600', color: '#4f46e5', marginBottom: 2 },
  content: { fontSize: 15, color: '#0f172a' },
  time: { fontSize: 10, color: '#94a3b8', marginTop: 4, textAlign: 'right' },
})
