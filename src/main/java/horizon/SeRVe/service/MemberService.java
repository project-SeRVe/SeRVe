package horizon.SeRVe.service;

import horizon.SeRVe.dto.member.InviteMemberRequest;
import horizon.SeRVe.dto.member.MemberKickResponse;
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
    public MemberKickResponse kickMember(String teamId, String targetUserId, String adminUserId) {
        // [New] 강퇴 대상이 저장소 소유자(Owner)인지 확인하는 로직 추가
        // 실수로 관리자가 소유자를 강퇴하여 저장소가 고아 상태가 되는 것을 방지
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));

        if (team.getOwnerId().equals(targetUserId)) {
            throw new SecurityException("저장소 소유자(Owner)는 강퇴할 수 없습니다.");
        }
        RepositoryMember targetMember = validateAdminAndGetTarget(teamId, targetUserId, adminUserId);
        memberRepository.delete(targetMember);

        // [자동 키 로테이션 지원] 남은 멤버 목록 조회 및 반환
        // 클라이언트가 즉시 키 로테이션을 수행할 수 있도록 필요한 정보 제공
        List<MemberKickResponse.RemainingMemberInfo> remainingMembers = memberRepository.findAllByTeam(team)
                .stream()
                .map(member -> MemberKickResponse.RemainingMemberInfo.builder()
                        .userId(member.getUser().getUserId())
                        .email(member.getUser().getEmail())
                        .publicKey(member.getUser().getPublicKey())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        // [보안] 멤버 퇴출 시 Key Rotation 필수 알림
        // 퇴출된 멤버는 여전히 팀 키를 보유하고 있으므로, 즉시 팀 키를 갱신해야 합니다.
        // 클라이언트(Admin)는 응답의 remainingMembers를 사용하여:
        // 1. 새로운 팀 키 생성
        // 2. 남은 멤버들의 공개키로 새 팀 키 래핑
        // 3. POST /teams/{teamId}/members/rotate-keys API 호출하여 키 업데이트
        // 4. (선택적) 기존 문서를 새 팀 키로 재암호화
        return MemberKickResponse.createSuccess(remainingMembers);
    }

    // 4. 권한 변경
    @Transactional
    public void updateMemberRole(String teamId, String targetUserId, String adminUserId, UpdateRoleRequest req) {
        RepositoryMember targetMember = validateAdminAndGetTarget(teamId, targetUserId, adminUserId);
        Role newRole;
        try {
            newRole = Role.valueOf(req.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 Role입니다.");
        }

        // [Modified] 보안 패치: 소유자(Owner)가 자신의 권한을 MEMBER로 내리는 것을 차단
        boolean isOwner = targetMember.getTeam().getOwnerId().equals(targetUserId);
        if (isOwner && newRole != Role.ADMIN) {
            throw new SecurityException("저장소 소유자(Owner)는 권한을 변경할 수 없습니다. (항상 ADMIN 유지)");
        }

        targetMember.setRole(newRole);
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
