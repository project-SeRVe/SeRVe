package horizon.SeRVe;

import com.google.crypto.tink.KeysetHandle;
import horizon.SeRVe.dto.auth.LoginResponse;
import horizon.SeRVe.dto.auth.SignupRequest;
import horizon.SeRVe.security.crypto.KeyExchangeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthDtoTest {

    @Test
    @DisplayName("보안 모듈의 키 데이터가 인증 DTO에 문제없이 호환되는지 검증")
    void testDtoCompatibilityWithSecurityModule() throws Exception {
        // 1. [준비] 기존 보안 모듈 로직을 가져옴
        KeyExchangeService keyService = new KeyExchangeService();

        // 2. [실행] 실제 Tink 키 쌍 생성 (클라이언트 역할 시뮬레이션)
        KeysetHandle clientKeyPair = keyService.generateClientKeyPair();
        String realPublicKey = keyService.getPublicKeyJson(clientKeyPair);

        // 가정: 클라이언트가 개인키를 비밀번호로 암호화해서 문자열로 만들었다고 침
        String fakeEncryptedPrivateKey = "encrypted-private-key-string-example";

        // 3. [검증 1] SignupRequest DTO에 잘 담기는가?
        SignupRequest signupReq = SignupRequest.builder()
                .email("test@example.com")
                .password("1234")
                .publicKey(realPublicKey) // 실제 Tink 키 문자열 주입
                .encryptedPrivateKey(fakeEncryptedPrivateKey)
                .build();

        System.out.println("DTO에 담긴 공개키: " + signupReq.getPublicKey());
        assertNotNull(signupReq.getPublicKey());
        assertEquals(realPublicKey, signupReq.getPublicKey());

        // 4. [검증 2] LoginResponse DTO로 잘 옮겨지는가? (DB에서 꺼냈다고 가정)
        LoginResponse loginRes = LoginResponse.builder()
                .userId("user-uuid-1234")
                .email(signupReq.getEmail())
                .publicKey(signupReq.getPublicKey())
                .encryptedPrivateKey(signupReq.getEncryptedPrivateKey())
                .build();

        // 결과 확인
        assertEquals(fakeEncryptedPrivateKey, loginRes.getEncryptedPrivateKey());
        assertEquals(realPublicKey, loginRes.getPublicKey());

        System.out.println(">> 검증 성공: 기존 보안 모듈의 키 데이터가 DTO 규격과 완벽하게 호환됩니다.");
    }
}