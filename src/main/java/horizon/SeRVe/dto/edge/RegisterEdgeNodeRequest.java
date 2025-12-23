package horizon.SeRVe.dto.edge;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterEdgeNodeRequest {
    @NotBlank(message = "시리얼 번호는 필수입니다.")
    private String serialNumber;

    @NotBlank(message = "API 토큰은 필수입니다.")
    private String apiToken;

    @NotBlank(message = "공개키는 필수입니다.")
    private String publicKey;

    @NotBlank(message = "팀 ID는 필수입니다.")
    private String teamId;

    // 암호화된 팀 키 (Optional - 나중에 설정 가능)
    private String encryptedTeamKey;
}
