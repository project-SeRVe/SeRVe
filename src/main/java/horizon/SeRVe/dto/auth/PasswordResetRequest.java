package horizon.SeRVe.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    // 기존 비밀번호 (본인 인증 및 클라이언트 측에서 구형 개인키 복호화용)
    @NotBlank(message = "기존 비밀번호를 입력해주세요.")
    private String oldPassword;

    // 새 비밀번호 (서버 DB 해시 업데이트용)
    @NotBlank(message = "새로운 비밀번호를 입력해주세요.")
    private String newPassword;

    /**
     * [Key Update]
     * 비밀번호가 바뀌면 키를 잠그는 자물쇠도 바뀌어야 합니다.
     * 클라이언트가 '새 비밀번호'로 다시 암호화한 개인키를 보냅니다.
     */
    @NotBlank(message = "재암호화된 개인키가 필요합니다.")
    private String newEncryptedPrivateKey;
}