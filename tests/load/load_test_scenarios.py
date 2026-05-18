import json


SCENARIOS = {
    "light": {"users": 10, "spawn_rate": 2, "run_time": "1m"},
    "medium": {"users": 50, "spawn_rate": 5, "run_time": "5m"},
    "heavy": {"users": 200, "spawn_rate": 10, "run_time": "10m"},
    "stress": {"users": 500, "spawn_rate": 25, "run_time": "15m"},
}


def get_scenario(name: str = "medium") -> dict:
    return SCENARIOS.get(name, SCENARIOS["medium"])
