import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../../shared"))

from utils.validators import is_valid_username, is_valid_email
from schemas.common import ErrorResponse, PaginationRequest


def test_error_response():
    err = ErrorResponse(error="NOT_FOUND", message="User not found")
    assert err.error == "NOT_FOUND"
    assert err.message == "User not found"


def test_pagination_request():
    req = PaginationRequest(limit=20, offset=0)
    assert req.limit == 20
    assert req.offset == 0


def test_default_pagination():
    req = PaginationRequest()
    assert req.limit == 50
    assert req.offset == 0
