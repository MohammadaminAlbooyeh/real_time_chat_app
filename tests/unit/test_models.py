import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../../services/api-service"))

from models.user import User
from models.room import Room, RoomMember, MemberRole
from models.message import Message, MessageReaction, MessageAttachment, MessageType
from models.relationship import Relationship, RelationshipStatus


def test_user_model():
    user = User(
        username="testuser",
        email="test@example.com",
        hashed_password="hashed_pw",
        display_name="Test User",
    )
    assert user.username == "testuser"
    assert user.email == "test@example.com"
    assert user.is_online is False
    assert user.is_active is True


def test_room_model():
    room = Room(name="Test Room", description="A test room", is_private=False)
    assert room.name == "Test Room"
    assert room.is_private is False


def test_room_member():
    member = RoomMember(role=MemberRole.ADMIN)
    assert member.role == MemberRole.ADMIN


def test_message_model():
    msg = Message(content="Hello!", message_type=MessageType.TEXT)
    assert msg.content == "Hello!"
    assert msg.message_type == MessageType.TEXT
    assert msg.is_deleted is False


def test_relationship_model():
    rel = Relationship(status=RelationshipStatus.PENDING)
    assert rel.status == RelationshipStatus.PENDING
