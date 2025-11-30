package horizon.SeRVe.dto.member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InviteMemberRequest {

    // 초대할 사용자의 이메일 주소
    private String email;

    /**
     * [보안 핵심] 초대받는 사람용 암호화된 팀 키
     * - "초대하는 사람(Admin)"이 "초대받는 사람(New Member)"의 공개키를 조회하여,
     * 팀 AES 키를 암호화해서 보낸 값입니다.
     * - 서버는 이 값을 전달만 할 뿐, 내용을 알 수 없습니다.
     */
    private String encryptedTeamKey;
}