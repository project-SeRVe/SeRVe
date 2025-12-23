package horizon.SeRVe.dto.member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTeamKeysRequest {

    /**
     * 멤버별 새로운 암호화된 팀 키 목록
     */
    private List<MemberKey> memberKeys;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberKey {
        private String userId;
        private String encryptedTeamKey;
    }
}
