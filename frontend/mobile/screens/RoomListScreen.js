import React, { useEffect, useState } from 'react'
import { View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator, Alert, TextInput, Modal } from 'react-native'
import { api } from '../services/api'

export default function RoomListScreen({ navigation, onLogout }) {
  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const loadRooms = async () => {
    try {
      const data = await api.get('/rooms')
      setRooms(data)
    } catch (err) {
      Alert.alert('Error', err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadRooms() }, [])

  const handleCreate = async () => {
    if (!name.trim()) return
    try {
      const room = await api.post('/rooms', { name: name.trim(), description: description.trim() || undefined })
      setRooms(prev => [room, ...prev])
      setShowCreate(false)
      setName('')
      setDescription('')
      navigation.navigate('Chat', { roomId: room.id, roomName: room.name })
    } catch (err) {
      Alert.alert('Error', err.message)
    }
  }

  const renderRoom = ({ item }) => (
    <TouchableOpacity style={styles.roomItem} onPress={() => navigation.navigate('Chat', { roomId: item.id, roomName: item.name })}>
      <View style={styles.roomAvatar}>
        <Text style={styles.roomAvatarText}>{item.name.charAt(0).toUpperCase()}</Text>
      </View>
      <View style={styles.roomInfo}>
        <Text style={styles.roomName}>{item.name}</Text>
        <Text style={styles.roomPreview} numberOfLines={1}>{item.last_message || 'No messages yet'}</Text>
      </View>
      <Text style={styles.roomCount}>{item.member_count}</Text>
    </TouchableOpacity>
  )

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Rooms</Text>
        <TouchableOpacity onPress={() => setShowCreate(true)} style={styles.headerButton}>
          <Text style={styles.headerButtonText}>+ New</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <ActivityIndicator size="large" color="#4f46e5" style={{ marginTop: 40 }} />
      ) : (
        <FlatList data={rooms} renderItem={renderRoom} keyExtractor={item => item.id} contentContainerStyle={styles.list} />
      )}

      <Modal visible={showCreate} transparent animationType="slide">
        <View style={styles.modalOverlay}>
          <View style={styles.modal}>
            <Text style={styles.modalTitle}>Create Room</Text>
            <TextInput style={styles.input} placeholder="Room name" value={name} onChangeText={setName} maxLength={100} />
            <TextInput style={styles.input} placeholder="Description (optional)" value={description} onChangeText={setDescription} maxLength={500} />
            <View style={styles.modalActions}>
              <TouchableOpacity style={styles.button} onPress={handleCreate}><Text style={styles.buttonText}>Create</Text></TouchableOpacity>
              <TouchableOpacity style={[styles.button, styles.buttonSecondary]} onPress={() => setShowCreate(false)}><Text style={[styles.buttonText, { color: '#0f172a' }]}>Cancel</Text></TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </View>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f8fafc' },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, backgroundColor: '#fff', borderBottomWidth: 1, borderBottomColor: '#e2e8f0' },
  headerTitle: { fontSize: 20, fontWeight: '700' },
  headerButton: { backgroundColor: '#4f46e5', paddingHorizontal: 14, paddingVertical: 8, borderRadius: 8 },
  headerButtonText: { color: '#fff', fontWeight: '600' },
  list: { padding: 12 },
  roomItem: { flexDirection: 'row', alignItems: 'center', padding: 12, backgroundColor: '#fff', borderRadius: 8, marginBottom: 8, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  roomAvatar: { width: 44, height: 44, borderRadius: 22, backgroundColor: '#4f46e5', alignItems: 'center', justifyContent: 'center', marginRight: 12 },
  roomAvatarText: { color: '#fff', fontSize: 18, fontWeight: '600' },
  roomInfo: { flex: 1 },
  roomName: { fontSize: 15, fontWeight: '600' },
  roomPreview: { fontSize: 13, color: '#64748b', marginTop: 2 },
  roomCount: { fontSize: 12, color: '#94a3b8', marginLeft: 8 },
  modalOverlay: { flex: 1, justifyContent: 'center', backgroundColor: 'rgba(0,0,0,0.4)', padding: 20 },
  modal: { backgroundColor: '#fff', borderRadius: 12, padding: 24 },
  modalTitle: { fontSize: 18, fontWeight: '700', marginBottom: 16 },
  input: { borderWidth: 1, borderColor: '#e2e8f0', borderRadius: 8, padding: 12, marginBottom: 12, fontSize: 16 },
  modalActions: { flexDirection: 'row', gap: 12, marginTop: 8 },
  button: { flex: 1, backgroundColor: '#4f46e5', borderRadius: 8, padding: 12, alignItems: 'center' },
  buttonSecondary: { backgroundColor: '#e2e8f0' },
  buttonText: { color: '#fff', fontSize: 15, fontWeight: '600' },
})
