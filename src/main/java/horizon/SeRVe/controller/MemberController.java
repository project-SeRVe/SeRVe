package horizon.SeRVe.controller;

import horizon.SeRVe.dto.member.InviteMemberRequest;
import horizon.SeRVe.dto.member.MemberResponse;
import horizon.SeRVe.dto.member.UpdateRoleRequest;
import horizon.SeRVe.dto.member.UpdateTeamKeysRequest;
import horizon.SeRVe.entity.User;
import horizon.SeRVe.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams/{teamId}/members") // 기존: /repositories/{repoId}/members
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 1. 멤버 초대 (ADMIN 권한 필요)
    @PostMapping
    public ResponseEntity<Void> inviteMember(
            @PathVariable String teamId, // 기존: repoId
            @AuthenticationPrincipal User user,
            @RequestBody InviteMemberRequest request) {

        memberService.inviteMember(teamId, user.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    // 2. 멤버 목록 조회 (해당 팀의 멤버만 조회 가능)
    @GetMapping
    public ResponseEntity<List<MemberResponse>> getMembers(
            @PathVariable String teamId, // 기존: repoId
            @AuthenticationPrincipal User user) {

        List<MemberResponse> members = memberService.getMembers(teamId, user.getUserId());
        return ResponseEntity.ok(members);
    }

    // 3. 멤버 강퇴 (ADMIN 권한 필요)
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable String teamId, // 기존: repoId
            @PathVariable String targetUserId,
            @AuthenticationPrincipal User admin) {

        memberService.kickMember(teamId, targetUserId, admin.getUserId());
        return ResponseEntity.ok().build();
    }

    // 4. 권한 변경 (ADMIN 권한 필요)
    @PutMapping("/{targetUserId}")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable String teamId, // 기존: repoId
            @PathVariable String targetUserId,
            @AuthenticationPrincipal User admin,
            @RequestBody UpdateRoleRequest request) {

        memberService.updateMemberRole(teamId, targetUserId, admin.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * 5. 키 로테이션 (팀 키 일괄 업데이트)
     *
     * POST /api/teams/{teamId}/members/rotate-keys
     *
     * Request Body:
     * {
     *   "memberKeys": [
     *     {"userId": "user1", "encryptedTeamKey": "..."},
     *     {"userId": "user2", "encryptedTeamKey": "..."}
     *   ]
     * }
     *
     * 멤버 강퇴 후 보안을 위해 팀 키를 로테이션할 때 사용합니다.
     * 클라이언트에서 새 팀 키를 생성하고, 각 멤버의 공개키로 래핑한 후 이 API를 호출합니다.
     * ADMIN 권한이 필요합니다.
     */
    @PostMapping("/rotate-keys")
    public ResponseEntity<Void> rotateTeamKeys(
            @PathVariable String teamId,
            @AuthenticationPrincipal User admin,
            @RequestBody UpdateTeamKeysRequest request) {

        memberService.rotateTeamKeys(teamId, admin.getUserId(), request);
        return ResponseEntity.ok().build();
    }
}
