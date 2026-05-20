import { useEffect, useRef, useCallback } from 'react'
import { useDispatch } from 'react-redux'
import wsService from '../services/websocket'
import { addMessage, updateMessage, removeMessage, addReactionToMessage, removeReactionFromMessage } from '../store/chatSlice'
import { updateLastMessage } from '../store/roomSlice'
import { setUserOnline, setUserOffline } from '../store/authSlice'
import { setUserOnline as setFriendOnline, setUserOffline as setFriendOffline } from '../store/userSlice'
import { WSEvent } from '../utils/constants'

export function useWebSocket() {
  const dispatch = useDispatch()
  const cleanupRef = useRef(null)

  useEffect(() => {
    const unsub1 = wsService.on(WSEvent.MESSAGE_NEW, (payload) => {
      dispatch(addMessage(payload))
      dispatch(updateLastMessage({
        room_id: payload.room_id,
        content: payload.content,
        created_at: payload.created_at,
      }))
    })

    const unsub2 = wsService.on(WSEvent.MESSAGE_UPDATED, (payload) => {
      dispatch(updateMessage(payload))
    })

    const unsub3 = wsService.on(WSEvent.MESSAGE_DELETED, (payload) => {
      dispatch(removeMessage(payload))
    })

    const unsub4 = wsService.on(WSEvent.MESSAGE_REACTION, (payload) => {
      dispatch(addReactionToMessage(payload))
    })

    const unsub5 = wsService.on(WSEvent.PRESENCE_ONLINE, (payload) => {
      dispatch(setUserOnline(payload.user_id))
      dispatch(setFriendOnline(payload.user_id))
    })

    const unsub6 = wsService.on(WSEvent.PRESENCE_OFFLINE, (payload) => {
      dispatch(setUserOffline(payload.user_id))
      dispatch(setFriendOffline(payload.user_id))
    })

    const unsub7 = wsService.on(WSEvent.TYPING_INDICATOR, () => {})

    cleanupRef.current = () => {
      unsub1(); unsub2(); unsub3(); unsub4()
      unsub5(); unsub6(); unsub7()
    }

    return () => cleanupRef.current?.()
  }, [dispatch])

  const sendMessage = useCallback((roomId, content, messageType = 'text', replyToId = null) => {
    wsService.sendMessage(roomId, content, messageType, replyToId)
  }, [])

  const startTyping = useCallback((roomId) => {
    wsService.startTyping(roomId)
  }, [])

  const stopTyping = useCallback((roomId) => {
    wsService.stopTyping(roomId)
  }, [])

  const joinRoom = useCallback((roomId) => {
    wsService.joinRoom(roomId)
  }, [])

  const leaveRoom = useCallback((roomId) => {
    wsService.leaveRoom(roomId)
  }, [])

  return { sendMessage, startTyping, stopTyping, joinRoom, leaveRoom }
}
