package horizon.SeRVe.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    /**
     * [Phase 1: Key Exchange 준비]
     * 클라이언트가 생성한 ECIES 공개키 (JSON String)
     * - 용도: 서버가 AES 키를 암호화(Wrap)해서 줄 때 사용
     * - 저장: User.publicKey
     */
    @NotBlank(message = "공개키는 필수입니다.")
    private String publicKey;

    /**
     * [Key Persistence]
     * 클라이언트가 생성하고, 본인의 비밀번호로 암호화한 개인키
     * - 용도: 나중에 로그인했을 때 다시 복구하기 위함 (서버는 내용 모름)
     * - 저장: User.encryptedPrivateKey
     */
    @NotBlank(message = "암호화된 개인키는 필수입니다.")
    private String encryptedPrivateKey;
}