import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { api } from '../services/api'

export const fetchRooms = createAsyncThunk('rooms/fetchRooms', async (_, { rejectWithValue }) => {
  try {
    return await api.get('/rooms')
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const fetchRoom = createAsyncThunk('rooms/fetchRoom', async (roomId, { rejectWithValue }) => {
  try {
    return await api.get(`/rooms/${roomId}`)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const createRoom = createAsyncThunk('rooms/createRoom', async (data, { rejectWithValue }) => {
  try {
    return await api.post('/rooms', data)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const updateRoom = createAsyncThunk('rooms/updateRoom', async ({ roomId, data }, { rejectWithValue }) => {
  try {
    return await api.put(`/rooms/${roomId}`, data)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const deleteRoom = createAsyncThunk('rooms/deleteRoom', async (roomId, { rejectWithValue }) => {
  try {
    await api.delete(`/rooms/${roomId}`)
    return roomId
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const addMember = createAsyncThunk('rooms/addMember', async ({ roomId, userId }, { rejectWithValue }) => {
  try {
    await api.post(`/rooms/${roomId}/members`, { user_id: userId })
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const removeMember = createAsyncThunk('rooms/removeMember', async ({ roomId, userId }, { rejectWithValue }) => {
  try {
    await api.delete(`/rooms/${roomId}/members/${userId}`)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const leaveRoomAction = createAsyncThunk('rooms/leaveRoom', async (roomId, { rejectWithValue }) => {
  try {
    await api.post(`/rooms/${roomId}/leave`)
    return roomId
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

const roomSlice = createSlice({
  name: 'rooms',
  initialState: {
    rooms: [],
    currentRoom: null,
    loading: false,
    error: null,
  },
  reducers: {
    setCurrentRoom(state, action) {
      state.currentRoom = action.payload
    },
    addRoomToList(state, action) {
      const exists = state.rooms.find(r => r.id === action.payload.id)
      if (!exists) state.rooms.unshift(action.payload)
    },
    updateRoomInList(state, action) {
      const idx = state.rooms.findIndex(r => r.id === action.payload.id)
      if (idx !== -1) state.rooms[idx] = { ...state.rooms[idx], ...action.payload }
    },
    removeRoomFromList(state, action) {
      state.rooms = state.rooms.filter(r => r.id !== action.payload)
      if (state.currentRoom?.id === action.payload) state.currentRoom = null
    },
    updateLastMessage(state, action) {
      const { room_id, content, created_at } = action.payload
      const room = state.rooms.find(r => r.id === room_id)
      if (room) {
        room.last_message = content
        room.last_message_at = created_at
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchRooms.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchRooms.fulfilled, (state, action) => {
        state.loading = false
        state.rooms = action.payload
      })
      .addCase(fetchRooms.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload
      })
      .addCase(fetchRoom.fulfilled, (state, action) => {
        state.currentRoom = action.payload
      })
      .addCase(createRoom.fulfilled, (state, action) => {
        state.rooms.unshift(action.payload)
      })
      .addCase(updateRoom.fulfilled, (state, action) => {
        const idx = state.rooms.findIndex(r => r.id === action.payload.id)
        if (idx !== -1) state.rooms[idx] = action.payload
        if (state.currentRoom?.id === action.payload.id) state.currentRoom = action.payload
      })
      .addCase(deleteRoom.fulfilled, (state, action) => {
        state.rooms = state.rooms.filter(r => r.id !== action.payload)
        if (state.currentRoom?.id === action.payload) state.currentRoom = null
      })
      .addCase(leaveRoomAction.fulfilled, (state, action) => {
        state.rooms = state.rooms.filter(r => r.id !== action.payload)
        if (state.currentRoom?.id === action.payload) state.currentRoom = null
      })
  },
})

export const { setCurrentRoom, addRoomToList, updateRoomInList, removeRoomFromList, updateLastMessage } = roomSlice.actions
export default roomSlice.reducer
