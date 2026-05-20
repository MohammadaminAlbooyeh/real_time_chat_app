import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import * as authService from '../services/auth'
import { api } from '../services/api'

export const loginUser = createAsyncThunk('auth/login', async ({ email, password }, { rejectWithValue }) => {
  try {
    return await authService.login(email, password)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const registerUser = createAsyncThunk('auth/register', async ({ username, email, password, displayName }, { rejectWithValue }) => {
  try {
    return await authService.register(username, email, password, displayName)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const updateProfile = createAsyncThunk('auth/updateProfile', async (data, { rejectWithValue }) => {
  try {
    return await api.patch('/users/me', data)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

export const changePassword = createAsyncThunk('auth/changePassword', async (data, { rejectWithValue }) => {
  try {
    await api.post('/users/me/change-password', data)
  } catch (error) {
    return rejectWithValue(error.message)
  }
})

const authSlice = createSlice({
  name: 'auth',
  initialState: {
    user: null,
    loading: false,
    error: null,
    initialized: false,
  },
  reducers: {
    setUser(state, action) {
      state.user = action.payload
      state.initialized = true
    },
    clearAuth(state) {
      state.user = null
      state.error = null
      state.initialized = true
    },
    setUserOnline(state, action) {
      if (state.user && state.user.id === action.payload) {
        state.user.is_online = true
      }
    },
    setUserOffline(state, action) {
      if (state.user && state.user.id === action.payload) {
        state.user.is_online = false
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginUser.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.loading = false
        state.user = action.payload
        state.initialized = true
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload
      })
      .addCase(registerUser.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(registerUser.fulfilled, (state, action) => {
        state.loading = false
        state.user = action.payload
        state.initialized = true
      })
      .addCase(registerUser.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload
      })
      .addCase(updateProfile.fulfilled, (state, action) => {
        state.user = action.payload
      })
  },
})

export const { setUser, clearAuth, setUserOnline, setUserOffline } = authSlice.actions
export default authSlice.reducer
