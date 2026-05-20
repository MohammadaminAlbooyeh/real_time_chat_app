import { configureStore } from '@reduxjs/toolkit'
import authReducer from './authSlice'
import chatReducer from './chatSlice'
import roomReducer from './roomSlice'
import userReducer from './userSlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    chat: chatReducer,
    rooms: roomReducer,
    users: userReducer,
  },
})
