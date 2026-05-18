import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../../services/api-service"))

import pytest
from models.base import Base
from models.user import User
from models.room import Room, RoomMember, MemberRole
from models.message import Message, MessageType
from models.relationship import Relationship, RelationshipStatus


def test_create_user():
    user = User(
        username="testuser",
        email="test@example.com",
        hashed_password="hashed_pw",
    )
    assert user.id is not None
    assert str(user.id)


def test_create_room_with_members():
    user = User(username="admin", email="admin@test.com", hashed_password="pw")
    room = Room(name="Test Room", created_by_id=user.id)
    member = RoomMember(room_id=room.id, user_id=user.id, role=MemberRole.ADMIN)

    assert room.name == "Test Room"
    assert member.role == MemberRole.ADMIN
    assert room.created_by_id == user.id


def test_message_relationships():
    room = Room(name="Chat Room")
    user = User(username="sender", email="sender@test.com", hashed_password="pw")
    msg = Message(
        room_id=room.id,
        sender_id=user.id,
        content="Hello!",
        message_type=MessageType.TEXT,
    )

    assert msg.content == "Hello!"
    assert msg.message_type == MessageType.TEXT
    assert msg.is_deleted is False


def test_relationship_status():
    rel = Relationship(status=RelationshipStatus.PENDING)
    assert rel.status == RelationshipStatus.PENDING

    rel.status = RelationshipStatus.ACCEPTED
    assert rel.status == RelationshipStatus.ACCEPTED
