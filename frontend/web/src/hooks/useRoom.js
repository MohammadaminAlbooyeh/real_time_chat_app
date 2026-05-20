import { useCallback, useEffect } from 'react'
import { useSelector, useDispatch } from 'react-redux'
import { fetchRooms, fetchRoom, createRoom, updateRoom, deleteRoom, addMember, removeMember, leaveRoomAction, setCurrentRoom } from '../store/roomSlice'

export function useRoom(roomId) {
  const dispatch = useDispatch()
  const { rooms, currentRoom, loading } = useSelector(state => state.rooms)

  const loadRooms = useCallback(() => dispatch(fetchRooms()), [dispatch])

  const loadRoom = useCallback((id) => dispatch(fetchRoom(id)), [dispatch])

  const create = useCallback((data) => dispatch(createRoom(data)), [dispatch])

  const update = useCallback((id, data) => dispatch(updateRoom({ roomId: id, data })), [dispatch])

  const remove = useCallback((id) => dispatch(deleteRoom(id)), [dispatch])

  const leave = useCallback((id) => dispatch(leaveRoomAction(id)), [dispatch])

  const addUser = useCallback((id, userId) => dispatch(addMember({ roomId: id, userId })), [dispatch])

  const removeUser = useCallback((id, userId) => dispatch(removeMember({ roomId: id, userId })), [dispatch])

  useEffect(() => {
    if (roomId) {
      dispatch(setCurrentRoom(null))
      dispatch(fetchRoom(roomId))
    }
  }, [dispatch, roomId])

  return {
    rooms,
    currentRoom,
    loading,
    loadRooms,
    loadRoom,
    create,
    update,
    remove,
    leave,
    addUser,
    removeUser,
    selectRoom: (id) => dispatch(setCurrentRoom(id)),
  }
}
