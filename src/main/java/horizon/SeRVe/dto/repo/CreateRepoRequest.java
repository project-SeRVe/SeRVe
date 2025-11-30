package horizon.SeRVe.dto.repo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRepoRequest {

    // 저장소 이름 (예: "Smart Factory Alpha")
    private String name;

    // 저장소 설명
    private String description;

    /**
     * [보안 핵심] 암호화된 팀 키 (Encrypted Team Key)
     * - 클라이언트가 생성한 Random AES 키를, 생성자(Owner) 자신의 공개키로 암호화한 값입니다.
     * - 서버는 이 키를 복호화할 수 없으며(Blind), DB에 그대로 저장합니다.
     * - 나중에 Owner가 로그인하면 자신의 개인키로 이 값을 풀어 실제 팀 키를 획득합니다.
     */
    private String encryptedTeamKey;
}