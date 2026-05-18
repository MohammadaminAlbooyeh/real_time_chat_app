import json
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("worker")


def setup_logging():
    return logger
