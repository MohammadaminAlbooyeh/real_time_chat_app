import React, { useState } from 'react'
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, ScrollView } from 'react-native'
import { api, setTokens } from '../services/api'

export default function ProfileScreen({ user, onLogout }) {
  const [displayName, setDisplayName] = useState(user?.display_name || '')
  const [bio, setBio] = useState(user?.bio || '')
  const [saving, setSaving] = useState(false)

  const handleSave = async () => {
    setSaving(true)
    try {
      await api.patch('/users/me', { display_name: displayName || undefined, bio: bio || undefined })
      Alert.alert('Success', 'Profile updated')
    } catch (err) {
      Alert.alert('Error', err.message)
    } finally {
      setSaving(false)
    }
  }

  const handleLogout = async () => {
    await setTokens(null, null)
    onLogout()
  }

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.card}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>
            {(user?.display_name || user?.username || '?').charAt(0).toUpperCase()}
          </Text>
        </View>
        <Text style={styles.username}>@{user?.username}</Text>

        <Text style={styles.label}>Display Name</Text>
        <TextInput style={styles.input} value={displayName} onChangeText={setDisplayName} maxLength={100} />

        <Text style={styles.label}>Bio</Text>
        <TextInput style={[styles.input, styles.textArea]} value={bio} onChangeText={setBio} maxLength={500} multiline numberOfLines={3} />

        <Text style={styles.label}>Email</Text>
        <TextInput style={[styles.input, styles.disabled]} value={user?.email} editable={false} />

        <TouchableOpacity style={styles.button} onPress={handleSave} disabled={saving}>
          <Text style={styles.buttonText}>{saving ? 'Saving...' : 'Save Profile'}</Text>
        </TouchableOpacity>

        <TouchableOpacity style={[styles.button, styles.logoutButton]} onPress={handleLogout}>
          <Text style={[styles.buttonText, { color: '#ef4444' }]}>Sign Out</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8fafc' },
  content: { padding: 20 },
  card: { backgroundColor: '#fff', borderRadius: 12, padding: 24 },
  avatar: { width: 72, height: 72, borderRadius: 36, backgroundColor: '#eef2ff', alignItems: 'center', justifyContent: 'center', alignSelf: 'center', marginBottom: 12 },
  avatarText: { fontSize: 28, fontWeight: '600', color: '#4f46e5' },
  username: { fontSize: 14, color: '#64748b', textAlign: 'center', marginBottom: 20 },
  label: { fontSize: 13, fontWeight: '500', color: '#0f172a', marginBottom: 4, marginTop: 12 },
  input: { borderWidth: 1, borderColor: '#e2e8f0', borderRadius: 8, padding: 12, fontSize: 16, backgroundColor: '#f8fafc' },
  textArea: { minHeight: 64, textAlignVertical: 'top' },
  disabled: { backgroundColor: '#f1f5f9', color: '#94a3b8' },
  button: { backgroundColor: '#4f46e5', borderRadius: 8, padding: 14, alignItems: 'center', marginTop: 20 },
  logoutButton: { backgroundColor: '#fef2f2', marginTop: 8 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
})
