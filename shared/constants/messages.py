class WSEvent:
    MESSAGE_SEND = "message:send"
    MESSAGE_NEW = "message:new"
    MESSAGE_EDIT = "message:edit"
    MESSAGE_UPDATED = "message:updated"
    MESSAGE_DELETE = "message:delete"
    MESSAGE_DELETED = "message:deleted"
    MESSAGE_REACT = "message:react"
    MESSAGE_REACTION = "message:reaction"

    TYPING_START = "typing:start"
    TYPING_STOP = "typing:stop"
    TYPING_INDICATOR = "typing:indicator"

    PRESENCE_ONLINE = "presence:online"
    PRESENCE_OFFLINE = "presence:offline"

    ROOM_JOIN = "room:join"
    ROOM_LEAVE = "room:leave"
    ROOM_UPDATED = "room:updated"

    ERROR = "error"
