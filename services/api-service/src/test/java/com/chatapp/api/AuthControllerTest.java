package com.chatapp.api;

import com.chatapp.api.controller.AuthController;
import com.chatapp.api.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void registerShouldReturn201() throws Exception {
        String payload = """
            {"username":"testuser","email":"test@example.com","password":"password123","displayName":"Test"}
            """;
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(payload))
            .andExpect(status().isCreated());
    }

    @Test
    void loginShouldReturn200() throws Exception {
        String payload = """
            {"email":"test@example.com","password":"password123"}
            """;
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(payload))
            .andExpect(status().isOk());
    }

    @Test
    void registerWithInvalidDataShouldReturn400() throws Exception {
        String payload = """
            {"username":"ab","email":"invalid","password":"short","displayName":""}
            """;
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(payload))
            .andExpect(status().isBadRequest());
    }
}
