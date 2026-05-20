import React from 'react'
import { View, TextInput, TouchableOpacity, Text, StyleSheet } from 'react-native'

export default function InputBar({ value, onChangeText, onSend, placeholder }) {
  return (
    <View style={styles.container}>
      <TextInput
        style={styles.input}
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder || 'Type a message...'}
        multiline
      />
      <TouchableOpacity style={styles.sendButton} onPress={onSend} disabled={!value?.trim()}>
        <Text style={[styles.sendText, !value?.trim() && styles.disabled]}>Send</Text>
      </TouchableOpacity>
    </View>
  )
}

const styles = StyleSheet.create({
  container: { flexDirection: 'row', padding: 8, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: '#e2e8f0', alignItems: 'flex-end' },
  input: { flex: 1, borderWidth: 1, borderColor: '#e2e8f0', borderRadius: 20, paddingHorizontal: 16, paddingVertical: 8, fontSize: 15, maxHeight: 100, backgroundColor: '#f8fafc' },
  sendButton: { backgroundColor: '#4f46e5', borderRadius: 20, paddingHorizontal: 16, paddingVertical: 10, marginLeft: 8 },
  sendText: { color: '#fff', fontWeight: '600' },
  disabled: { opacity: 0.4 },
})
