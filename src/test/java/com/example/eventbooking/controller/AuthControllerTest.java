package com.example.eventbooking.controller;

import com.example.eventbooking.config.SecurityConfig;
import com.example.eventbooking.dto.LoginRequest;
import com.example.eventbooking.dto.RegisterRequest;
import com.example.eventbooking.service.AuthService;
import com.example.eventbooking.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.example.eventbooking.security.JwtBlacklist jwtBlacklist;

    @Autowired
    private ObjectMapper objectMapper;

    // Тест 1: Rate limiting — после 5 запросов возвращает 429
    @Test
    void login_RateLimitExceeded_Returns429() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("user@test.com", "password");
        String body = objectMapper.writeValueAsString(request);

        // When: делаем 5 запросов (лимит)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
        }

        // Then: 6-й запрос получает 429
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void register_RateLimitExceeded_Returns429() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest("Test", "test@test.com", "password123");
        String body = objectMapper.writeValueAsString(request);

        // When: делаем 5 запросов (лимит)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
        }

        // Then: 6-й запрос получает 429
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());
    }

    // Тест 2: @Size валидация на title
    @Test
    void register_TitleTooLong_Returns400() throws Exception {
        // Given: name длиннее 100 символов
        RegisterRequest request = new RegisterRequest("A".repeat(101), "test@test.com", "password123");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
