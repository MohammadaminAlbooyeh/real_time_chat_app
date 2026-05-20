export const API_VERSION = 'v1'
export const PAGINATION_DEFAULT_LIMIT = 50
export const PAGINATION_MAX_LIMIT = 200
export const MAX_MESSAGE_LENGTH = 5000
export const MAX_ROOM_NAME_LENGTH = 100
export const MAX_USERNAME_LENGTH = 50

export const WSEvent = {
  MESSAGE_SEND: 'message:send',
  MESSAGE_NEW: 'message:new',
  MESSAGE_EDIT: 'message:edit',
  MESSAGE_UPDATED: 'message:updated',
  MESSAGE_DELETE: 'message:delete',
  MESSAGE_DELETED: 'message:deleted',
  MESSAGE_REACT: 'message:react',
  MESSAGE_REACTION: 'message:reaction',
  TYPING_START: 'typing:start',
  TYPING_STOP: 'typing:stop',
  TYPING_INDICATOR: 'typing:indicator',
  PRESENCE_ONLINE: 'presence:online',
  PRESENCE_OFFLINE: 'presence:offline',
  ROOM_JOIN: 'room:join',
  ROOM_LEAVE: 'room:leave',
  ROOM_UPDATED: 'room:updated',
  ROOM_JOINED: 'room:joined',
  ROOM_LEFT: 'room:left',
  ERROR: 'error',
}

export const MESSAGE_TYPE = {
  TEXT: 'text',
  IMAGE: 'image',
  FILE: 'file',
  SYSTEM: 'system',
}
