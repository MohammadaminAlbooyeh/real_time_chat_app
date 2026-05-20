import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { api } from '../services/api'

export const fetchMessages = createAsyncThunk('chat/fetchMessages', async ({ roomId, limit, before }, { rejectWithValue }) => {
  try {
    const params = { limit: limit || 50 }
    if (before) params.before = before
    const result = await api.get(`/rooms/${roomId}/messages`, params)
    return { roomId, ...result }
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const sendMessage = createAsyncThunk('chat/sendMessage', async ({ roomId, content, replyToId }, { rejectWithValue }) => {
  try {
    return await api.post(`/rooms/${roomId}/messages`, { room_id: roomId, content, reply_to_id: replyToId || null })
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const editMessage = createAsyncThunk('chat/editMessage', async ({ roomId, messageId, content }, { rejectWithValue }) => {
  try {
    return await api.put(`/rooms/${roomId}/messages/${messageId}`, { content })
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const deleteMessage = createAsyncThunk('chat/deleteMessage', async ({ roomId, messageId }, { rejectWithValue }) => {
  try {
    await api.delete(`/rooms/${roomId}/messages/${messageId}`)
    return { messageId }
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const addReaction = createAsyncThunk('chat/addReaction', async ({ roomId, messageId, emoji }, { rejectWithValue }) => {
  try {
    return await api.post(`/rooms/${roomId}/messages/${messageId}/reactions`, { emoji })
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const removeReaction = createAsyncThunk('chat/removeReaction', async ({ roomId, messageId, reactionId }, { rejectWithValue }) => {
  try {
    await api.delete(`/rooms/${roomId}/messages/${messageId}/reactions/${reactionId}`)
    return { messageId, reactionId }
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const searchMessages = createAsyncThunk('chat/searchMessages', async ({ query, roomId }, { rejectWithValue }) => {
  try {
    const params = { q: query }
    if (roomId) params.room_id = roomId
    return await api.get('/search/messages', params)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

const chatSlice = createSlice({
  name: 'chat',
  initialState: {
    messagesByRoom: {},
    loading: false,
    error: null,
    hasMore: {},
    searchResults: [],
    searchLoading: false,
  },
  reducers: {
    addMessage(state, action) {
      const msg = action.payload
      if (!state.messagesByRoom[msg.room_id]) {
        state.messagesByRoom[msg.room_id] = []
      }
      const existing = state.messagesByRoom[msg.room_id].find(m => m.id === msg.id)
      if (!existing) {
        state.messagesByRoom[msg.room_id].unshift(msg)
      }
    },
    updateMessage(state, action) {
      const msg = action.payload
      const room = state.messagesByRoom[msg.room_id]
      if (!room) return
      const idx = room.findIndex(m => m.id === msg.id)
      if (idx !== -1) room[idx] = { ...room[idx], ...msg }
    },
    removeMessage(state, action) {
      const { room_id, id } = action.payload
      const room = state.messagesByRoom[room_id]
      if (!room) return
      state.messagesByRoom[room_id] = room.filter(m => m.id !== id)
    },
    addReactionToMessage(state, action) {
      const reaction = action.payload
      const room = state.messagesByRoom[reaction.room_id]
      if (!room) return
      const msg = room.find(m => m.id === reaction.message_id)
      if (msg) {
        const existing = msg.reactions.find(r => r.id === reaction.id)
        if (!existing) msg.reactions.push(reaction)
      }
    },
    removeReactionFromMessage(state, action) {
      const { message_id, reaction_id } = action.payload
      for (const room of Object.values(state.messagesByRoom)) {
        const msg = room.find(m => m.id === message_id)
        if (msg) {
          msg.reactions = msg.reactions.filter(r => r.id !== reaction_id)
          break
        }
      }
    },
    clearMessages(state, action) {
      const roomId = action.payload
      state.messagesByRoom[roomId] = []
    },
    clearSearch(state) {
      state.searchResults = []
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchMessages.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(fetchMessages.fulfilled, (state, action) => {
        state.loading = false
        const { roomId, items, has_more } = action.payload
        state.messagesByRoom[roomId] = [...(state.messagesByRoom[roomId] || []), ...items]
        state.hasMore[roomId] = has_more
      })
      .addCase(fetchMessages.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload
      })
      .addCase(searchMessages.pending, (state) => {
        state.searchLoading = true
      })
      .addCase(searchMessages.fulfilled, (state, action) => {
        state.searchLoading = false
        state.searchResults = action.payload
      })
      .addCase(searchMessages.rejected, (state) => {
        state.searchLoading = false
      })
  },
})

export const {
  addMessage, updateMessage, removeMessage,
  addReactionToMessage, removeReactionFromMessage,
  clearMessages, clearSearch,
} = chatSlice.actions
export default chatSlice.reducer
