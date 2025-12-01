package horizon.SeRVe.service;

import horizon.SeRVe.dto.auth.LoginRequest;
import horizon.SeRVe.dto.auth.LoginResponse;
import horizon.SeRVe.dto.auth.SignupRequest;
import horizon.SeRVe.entity.User;
import horizon.SeRVe.repository.UserRepository;
import horizon.SeRVe.config.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signup_Success() {
        // given
        SignupRequest request = new SignupRequest(
                "test@example.com", "password123", "pubKey", "encPrivKey"
        );
        given(userRepository.existsByEmail(request.getEmail())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");

        // when
        authService.signup(request);

        // then
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() {
        // given
        String email = "test@example.com";
        String password = "password123";
        User mockUser = User.builder()
                .userId("uuid-1234")
                .email(email)
                .hashedPassword("encodedPassword")
                .encryptedPrivateKey("encPrivKey")
                .build();

        LoginRequest request = new LoginRequest(email, password);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches(password, mockUser.getHashedPassword())).willReturn(true);
        given(jwtTokenProvider.createToken(mockUser.getUserId(), email)).willReturn("accessToken_example");

        // when
        LoginResponse response = authService.login(request);

        // then
        assertNotNull(response.getAccessToken());
        assertEquals(email, response.getEmail());
        assertEquals("encPrivKey", response.getEncryptedPrivateKey());
    }
}