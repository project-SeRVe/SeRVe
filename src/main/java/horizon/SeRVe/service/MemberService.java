package horizon.SeRVe.service;

import horizon.SeRVe.dto.member.InviteMemberRequest;
import horizon.SeRVe.dto.member.MemberResponse;
import horizon.SeRVe.dto.member.UpdateRoleRequest;
import horizon.SeRVe.dto.member.UpdateTeamKeysRequest;
import horizon.SeRVe.entity.*;
import horizon.SeRVe.repository.MemberRepository;
import horizon.SeRVe.repository.TeamRepository;
import horizon.SeRVe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository; // 기존: TeamRepoRepository
    private final UserRepository userRepository;

    // 1. 멤버 초대
    @Transactional
    public void inviteMember(String teamId, String inviterUserId, InviteMemberRequest req) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));

        User inviter = userRepository.findById(inviterUserId)
                .orElseThrow(() -> new IllegalArgumentException("초대자를 찾을 수 없습니다."));

        // 초대자의 멤버십 및 권한 검증
        RepositoryMember inviterMember = memberRepository.findByTeamAndUser(team, inviter)
                .orElseThrow(() -> new SecurityException("초대자가 저장소 멤버가 아닙니다."));

        if (inviterMember.getRole() != Role.ADMIN) {
            throw new SecurityException("멤버 초대는 ADMIN 권한이 필요합니다.");
        }

        // 이메일로 유저 찾기
        User invitee = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 중복 체크
        // 기존: existsByTeamRepositoryAndUser → existsByTeamAndUser
        if (memberRepository.existsByTeamAndUser(team, invitee)) {
            throw new IllegalArgumentException("이미 해당 저장소의 멤버입니다.");
        }

        // 멤버 추가
        RepositoryMemberId memberId = new RepositoryMemberId(team.getTeamId(), invitee.getUserId());
        RepositoryMember newMember = RepositoryMember.builder()
                .id(memberId)
                .team(team) // 기존: teamRepository
                .user(invitee)
                .role(Role.MEMBER)
                .encryptedTeamKey(req.getEncryptedTeamKey()) // DTO에서 받은 키 저장
                .build();

        memberRepository.save(newMember);
    }

    // 2. 멤버 목록 조회
    public List<MemberResponse> getMembers(String teamId, String requesterId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 멤버십 검증 (해당 팀의 멤버만 멤버 목록 조회 가능)
        if (!memberRepository.existsByTeamAndUser(team, requester)) {
            throw new SecurityException("저장소 멤버가 아닙니다.");
        }

        // 기존: findAllByTeamRepository → findAllByTeam
        return memberRepository.findAllByTeam(team).stream()
                .map(MemberResponse::from)
                .collect(Collectors.toList());
    }

    // 3. 멤버 강퇴
    @Transactional
    public void kickMember(String teamId, String targetUserId, String adminUserId) {
        RepositoryMember targetMember = validateAdminAndGetTarget(teamId, targetUserId, adminUserId);
        memberRepository.delete(targetMember);

        // TODO: 키 로테이션 권장
        // 강퇴된 멤버는 이미 팀 키를 알고 있으므로, 보안을 위해 팀 키를 로테이션해야 합니다.
        // 클라이언트에서 다음 단계를 수행해야 합니다:
        // 1. 새로운 팀 키 생성
        // 2. 남은 멤버들의 공개키로 새 팀 키 래핑
        // 3. POST /teams/{teamId}/members/rotate-keys API 호출하여 키 업데이트
        // 4. (선택적) 기존 문서를 새 팀 키로 재암호화
    }

    // 4. 권한 변경
    @Transactional
    public void updateMemberRole(String teamId, String targetUserId, String adminUserId, UpdateRoleRequest req) {
        RepositoryMember targetMember = validateAdminAndGetTarget(teamId, targetUserId, adminUserId);
        try {
            Role newRole = Role.valueOf(req.getRole().toUpperCase());
            targetMember.setRole(newRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 Role입니다.");
        }
    }

    // 5. 키 로테이션 (팀 키 일괄 업데이트)
    @Transactional
    public void rotateTeamKeys(String teamId, String adminUserId, UpdateTeamKeysRequest req) {
        // 1. 관리자 권한 확인
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보 오류"));

        RepositoryMember adminMember = memberRepository.findByTeamAndUser(team, admin)
                .orElseThrow(() -> new SecurityException("관리자가 멤버가 아닙니다."));

        if (adminMember.getRole() != Role.ADMIN) {
            throw new SecurityException("관리자 권한이 필요합니다.");
        }

        // 2. 각 멤버의 암호화된 팀 키 업데이트
        for (UpdateTeamKeysRequest.MemberKey memberKey : req.getMemberKeys()) {
            User user = userRepository.findById(memberKey.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + memberKey.getUserId()));

            RepositoryMember member = memberRepository.findByTeamAndUser(team, user)
                    .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다: " + memberKey.getUserId()));

            member.setEncryptedTeamKey(memberKey.getEncryptedTeamKey());
        }
    }

    // [Helper] 권한 검증 및 타겟 조회
    private RepositoryMember validateAdminAndGetTarget(String teamId, String targetUserId, String adminUserId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보 오류"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자 정보 오류"));

        // 기존: findByTeamRepositoryAndUser → findByTeamAndUser
        RepositoryMember adminMember = memberRepository.findByTeamAndUser(team, admin)
                .orElseThrow(() -> new SecurityException("관리자가 멤버가 아닙니다."));

        if (adminMember.getRole() != Role.ADMIN) {
            throw new SecurityException("관리자 권한이 필요합니다.");
        }

        return memberRepository.findByTeamAndUser(team, target)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자가 멤버가 아닙니다."));
    }
}
