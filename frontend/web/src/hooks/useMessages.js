import { useCallback, useEffect, useRef } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { fetchMessages, sendMessage as sendMessageApi, editMessage, deleteMessage, addReaction, removeReaction, searchMessages, clearSearch } from '../store/chatSlice'

export function useMessages(roomId) {
  const dispatch = useDispatch()
  const messagesByRoom = useSelector(state => state.chat.messagesByRoom)
  const loading = useSelector(state => state.chat.loading)
  const hasMore = useSelector(state => state.chat.hasMore)
  const searchResults = useSelector(state => state.chat.searchResults)
  const searchLoading = useSelector(state => state.chat.searchLoading)
  const messages = messagesByRoom[roomId] || []

  const loadMessages = useCallback(({ limit, before } = {}) => {
    if (roomId) dispatch(fetchMessages({ roomId, limit, before }))
  }, [dispatch, roomId])

  const loadMore = useCallback(() => {
    if (hasMore[roomId] && messages.length > 0) {
      const oldest = messages[messages.length - 1]
      dispatch(fetchMessages({ roomId, before: oldest.created_at }))
    }
  }, [dispatch, roomId, hasMore, messages])

  const send = useCallback((content, replyToId) => {
    if (roomId) dispatch(sendMessageApi({ roomId, content, replyToId }))
  }, [dispatch, roomId])

  const edit = useCallback((messageId, content) => {
    if (roomId) dispatch(editMessage({ roomId, messageId, content }))
  }, [dispatch, roomId])

  const deleteMsg = useCallback((messageId) => {
    if (roomId) dispatch(deleteMessage({ roomId, messageId }))
  }, [dispatch, roomId])

  const react = useCallback((messageId, emoji) => {
    if (roomId) dispatch(addReaction({ roomId, messageId, emoji }))
  }, [dispatch, roomId])

  const unreact = useCallback((messageId, reactionId) => {
    if (roomId) dispatch(removeReaction({ roomId, messageId, reactionId }))
  }, [dispatch, roomId])

  const search = useCallback((query) => {
    dispatch(searchMessages({ query, roomId }))
  }, [dispatch, roomId])

  const clearSearchResults = useCallback(() => {
    dispatch(clearSearch())
  }, [dispatch])

  return {
    messages,
    loading,
    hasMore: hasMore[roomId],
    searchResults,
    searchLoading,
    loadMessages,
    loadMore,
    send,
    edit,
    delete: deleteMsg,
    react,
    unreact,
    search,
    clearSearchResults,
  }
}
