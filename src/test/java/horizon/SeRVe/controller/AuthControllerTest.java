package horizon.SeRVe.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import horizon.SeRVe.dto.auth.LoginRequest;
import horizon.SeRVe.dto.auth.LoginResponse;
import horizon.SeRVe.dto.auth.SignupRequest;
import horizon.SeRVe.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("API: 회원가입 요청 성공 (201 Created)")
    void signup_Api_Success() throws Exception {
        // given
        SignupRequest request = new SignupRequest("new@test.com", "1234", "pubKey", "privKey");

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("API: 로그인 요청 성공 (200 OK)")
    void login_Api_Success() throws Exception {
        // given
        LoginRequest request = new LoginRequest("user@test.com", "1234");
        LoginResponse response = LoginResponse.builder()
                .accessToken("dummy_token")
                .userId("user1")
                .email("user@test.com")
                .build();

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("dummy_token"))
                .andExpect(jsonPath("$.email").value("user@test.com"));
    }
}