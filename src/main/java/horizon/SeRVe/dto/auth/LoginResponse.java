package horizon.SeRVe.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String userId;
    private String email;

    /**
     * [Key Recovery]
     * 로그인 성공 시, 서버에 보관해뒀던 '암호화된 개인키'를 반환합니다.
     * 클라이언트는 이걸 받아서 로컬에서 비밀번호로 복호화(Unwrap) 후,
     * Phase 1(AES 키 수신)을 진행할 준비를 마칩니다.
     */
    private String encryptedPrivateKey;

    // 클라이언트 검증용 공개키 (선택 사항)
    private String publicKey;
}