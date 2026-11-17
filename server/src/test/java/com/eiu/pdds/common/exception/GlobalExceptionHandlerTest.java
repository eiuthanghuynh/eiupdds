package com.eiu.pdds.common.exception;

import com.eiu.pdds.auth.security.JwtTokenProvider;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Malformed JSON Body - Should return HTTP 400 Bad Request with REQ_001 ErrorCode")
    void shouldReturn400OnMalformedJson() throws Exception {
        String malformedJson = "{ \"username\": \"admin\", ";

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REQ_001"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Invalid Sort Property - Should return HTTP 400 Bad Request with REQ_001 ErrorCode")
    void shouldReturn400OnInvalidSortProperty() throws Exception {
        User user = userRepository.findAll().stream().findFirst().orElseGet(() ->
                userRepository.save(User.builder()
                        .username("test_handler_user")
                        .email("handler_user@eiu.edu.vn")
                        .passwordHash("hash")
                        .role(Role.ROLE_USER)
                        .isActive(true)
                        .build()));
        String token = jwtTokenProvider.generateAccessToken(user);

        mockMvc.perform(get("/api/v1/diagnoses")
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "nonExistentField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REQ_001"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort property: nonExistentField")));
    }

    @Test
    @DisplayName("Invalid Path Parameter Format - Should return HTTP 400 Bad Request with REQ_001 ErrorCode")
    void shouldReturn400OnInvalidUuidPathParameter() throws Exception {
        User user = userRepository.findAll().stream().findFirst().orElseGet(() ->
                userRepository.save(User.builder()
                        .username("test_handler_user")
                        .email("handler_user@eiu.edu.vn")
                        .passwordHash("hash")
                        .role(Role.ROLE_USER)
                        .isActive(true)
                        .build()));
        String token = jwtTokenProvider.generateAccessToken(user);

        mockMvc.perform(get("/api/v1/diagnoses/invalid-uuid-format")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REQ_001"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid parameter format for: id")));
    }
}
