import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "../../shared"))

from utils.validators import is_valid_username, is_valid_email
from utils.helpers import slugify, truncate


def test_valid_username():
    assert is_valid_username("alice")
    assert is_valid_username("alice_123")
    assert is_valid_username("a" * 50)
    assert not is_valid_username("al ice")
    assert not is_valid_username("al@ice")
    assert not is_valid_username("ab")


def test_valid_email():
    assert is_valid_email("alice@example.com")
    assert is_valid_email("alice.smith@example.co.uk")
    assert not is_valid_email("not-an-email")
    assert not is_valid_email("@example.com")


def test_slugify():
    assert slugify("Hello World") == "hello-world"
    assert slugify("  Hello   World  ") == "hello-world"
    assert slugify("Hello! World?") == "hello-world"


def test_truncate():
    assert truncate("Hello World") == "Hello World"
    assert len(truncate("Hello World", 5)) == 5
    assert truncate("Hello World", 8).endswith("...")
