from locust import HttpUser, task, between
import json


class ChatUser(HttpUser):
    wait_time = between(1, 3)

    def on_start(self):
        response = self.client.post(
            "/api/v1/auth/login",
            json={"email": "alice@example.com", "password": "password123"},
        )
        if response.status_code == 200:
            self.token = response.json()["access_token"]
            self.headers = {"Authorization": f"Bearer {self.token}"}
        else:
            self.token = None

    @task(3)
    def list_rooms(self):
        if self.token:
            self.client.get("/api/v1/rooms", headers=self.headers)

    @task(2)
    def get_profile(self):
        if self.token:
            self.client.get("/api/v1/users/me", headers=self.headers)

    @task(1)
    def search_users(self):
        if self.token:
            self.client.get("/api/v1/users/search?q=test", headers=self.headers)
