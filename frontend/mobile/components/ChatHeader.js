import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native'

export default function ChatHeader({ title, onBack, onProfile }) {
  return (
    <View style={styles.header}>
      <TouchableOpacity onPress={onBack} style={styles.backButton}>
        <Text style={styles.backText}>Back</Text>
      </TouchableOpacity>
      <Text style={styles.title} numberOfLines={1}>{title}</Text>
      <TouchableOpacity onPress={onProfile}>
        <Text style={styles.profileText}>Profile</Text>
      </TouchableOpacity>
    </View>
  )
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 12, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#e2e8f0' },
  backButton: { padding: 4 },
  backText: { color: '#4f46e5', fontSize: 16 },
  title: { fontSize: 17, fontWeight: '600', flex: 1, textAlign: 'center' },
  profileText: { color: '#4f46e5', fontSize: 14 },
})
