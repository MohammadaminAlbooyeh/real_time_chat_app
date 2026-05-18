import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../../services/api-service"))

import pytest
from auth import hash_password, verify_password, create_access_token, create_refresh_token, decode_token


def test_password_hashing():
    password = "secure-password-123"
    hashed = hash_password(password)
    assert hashed != password
    assert verify_password(password, hashed) is True
    assert verify_password("wrong-password", hashed) is False


def test_jwt_tokens():
    user_id = "00000000-0000-0000-0000-000000000001"

    access_token = create_access_token(user_id)
    assert access_token is not None

    decoded = decode_token(access_token)
    assert decoded is not None
    assert decoded["sub"] == user_id
    assert decoded["type"] == "access"

    refresh_token = create_refresh_token(user_id)
    decoded_refresh = decode_token(refresh_token)
    assert decoded_refresh["type"] == "refresh"


def test_expired_token():
    from datetime import datetime, timedelta, timezone
    from jose import jwt
    from config import settings

    expired = jwt.encode(
        {
            "sub": "test-user",
            "exp": datetime.now(timezone.utc) - timedelta(hours=1),
            "type": "access",
        },
        settings.jwt_secret_key,
        algorithm=settings.jwt_algorithm,
    )
    assert decode_token(expired) is None
