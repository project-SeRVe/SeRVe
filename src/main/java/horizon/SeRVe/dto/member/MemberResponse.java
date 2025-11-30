package horizon.SeRVe.dto.member;

import horizon.SeRVe.entity.RepositoryMember;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponse {

    private String userId;  // 멤버의 유저 ID
    private String email;   // 멤버 이메일
    private String role;    // 멤버의 역할 (ADMIN, MEMBER)

    // Entity -> DTO 변환 팩토리 메서드
    public static MemberResponse from(RepositoryMember member) {
        return MemberResponse.builder()
                .userId(member.getUser().getUserId()) // User 엔티티에서 ID 추출
                .email(member.getUser().getEmail())   // User 엔티티에서 이메일 추출
                .role(member.getRole().name())        // Enum -> String 변환
                .build();
    }
}