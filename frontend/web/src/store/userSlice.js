import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { api } from '../services/api'

export const searchUsers = createAsyncThunk('users/search', async (query, { rejectWithValue }) => {
  try {
    return await api.get('/users/search', { q: query })
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const fetchFriends = createAsyncThunk('users/fetchFriends', async (_, { rejectWithValue }) => {
  try {
    return await api.get('/relationships/friends')
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const fetchPendingRequests = createAsyncThunk('users/fetchPendingRequests', async (_, { rejectWithValue }) => {
  try {
    return await api.get('/relationships/pending')
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const sendFriendRequest = createAsyncThunk('users/sendFriendRequest', async (userId, { rejectWithValue }) => {
  try {
    return await api.post(`/relationships/request/${userId}`)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const respondToRequest = createAsyncThunk('users/respondToRequest', async ({ relationshipId, action }, { rejectWithValue }) => {
  try {
    return await api.post(`/relationships/respond/${relationshipId}?action=${action}`)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const removeFriend = createAsyncThunk('users/removeFriend', async (relationshipId, { rejectWithValue }) => {
  try {
    await api.delete(`/relationships/${relationshipId}`)
    return relationshipId
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

const userSlice = createSlice({
  name: 'users',
  initialState: {
    searchResults: [],
    friends: [],
    pendingRequests: [],
    loading: false,
    error: null,
  },
  reducers: {
    clearSearchResults(state) {
      state.searchResults = []
    },
    setUserOnline(state, action) {
      const userId = action.payload
      state.friends = state.friends.map(f =>
        f.id === userId ? { ...f, is_online: true } : f
      )
    },
    setUserOffline(state, action) {
      const userId = action.payload
      state.friends = state.friends.map(f =>
        f.id === userId ? { ...f, is_online: false } : f
      )
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(searchUsers.fulfilled, (state, action) => {
        state.searchResults = action.payload
      })
      .addCase(fetchFriends.fulfilled, (state, action) => {
        state.friends = action.payload
      })
      .addCase(fetchPendingRequests.fulfilled, (state, action) => {
        state.pendingRequests = action.payload
      })
      .addCase(respondToRequest.fulfilled, (state, action) => {
        state.pendingRequests = state.pendingRequests.filter(r => r.id !== action.meta.arg.relationshipId)
      })
      .addCase(removeFriend.fulfilled, (state, action) => {
        state.friends = state.friends.filter(f => f.id !== action.payload)
      })
      .addCase(sendFriendRequest.fulfilled, (state) => {
        state.error = null
      })
  },
})

export const { clearSearchResults, setUserOnline, setUserOffline } = userSlice.actions
export default userSlice.reducer
