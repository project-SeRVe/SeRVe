package horizon.SeRVe.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberKickResponse {

    private boolean success;

    /**
     * Key Rotation 필요 여부
     * 멤버 퇴출 시 보안을 위해 팀 키를 즉시 갱신해야 합니다.
     */
    private boolean keyRotationRequired;

    /**
     * 사용자에게 표시할 메시지
     */
    private String message;

    /**
     * Key Rotation을 수행해야 하는 이유
     */
    private String keyRotationReason;

    /**
     * 남은 멤버 목록 (Key Rotation에 사용)
     * 클라이언트가 이 정보로 새 팀 키를 각 멤버의 공개키로 래핑
     */
    private List<RemainingMemberInfo> remainingMembers;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemainingMemberInfo {
        private String userId;
        private String email;
        private String publicKey;
    }

    public static MemberKickResponse createSuccess(List<RemainingMemberInfo> remainingMembers) {
        return MemberKickResponse.builder()
                .success(true)
                .keyRotationRequired(true)
                .message("멤버가 성공적으로 퇴출되었습니다.")
                .keyRotationReason("퇴출된 멤버는 여전히 팀 키를 보유하고 있습니다. 즉시 팀 키를 갱신하여 데이터 보안을 유지하세요.")
                .remainingMembers(remainingMembers)
                .build();
    }
}
