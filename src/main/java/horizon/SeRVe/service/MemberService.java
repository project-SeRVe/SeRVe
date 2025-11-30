package horizon.SeRVe.service;

import horizon.SeRVe.dto.member.InviteMemberRequest;
import horizon.SeRVe.dto.member.MemberResponse;
import horizon.SeRVe.dto.member.UpdateRoleRequest;
import horizon.SeRVe.entity.*;
import horizon.SeRVe.repository.MemberRepository;
import horizon.SeRVe.repository.TeamRepoRepository;
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
    private final TeamRepoRepository teamRepoRepository;
    private final UserRepository userRepository;

    // 1. 멤버 초대
    @Transactional
    public void inviteMember(Long repoId, InviteMemberRequest req) {
        TeamRepository repo = teamRepoRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));

        // 이메일로 유저 찾기
        User invitee = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 중복 체크
        if (memberRepository.existsByTeamRepositoryAndUser(repo, invitee)) {
            throw new IllegalArgumentException("이미 해당 저장소의 멤버입니다.");
        }

        // 멤버 추가
        RepositoryMemberId memberId = new RepositoryMemberId(repo.getId(), invitee.getUserId());
        RepositoryMember newMember = RepositoryMember.builder()
                .id(memberId)
                .teamRepository(repo)
                .user(invitee)
                .role(Role.MEMBER)
                .encryptedTeamKey(req.getEncryptedTeamKey()) // DTO에서 받은 키 저장
                .build();

        memberRepository.save(newMember);
    }

    // 2. 멤버 목록 조회
    public List<MemberResponse> getMembers(Long repoId) {
        TeamRepository repo = teamRepoRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));

        return memberRepository.findAllByTeamRepository(repo).stream()
                .map(MemberResponse::from)
                .collect(Collectors.toList());
    }

    // 3. 멤버 강퇴
    @Transactional
    public void kickMember(Long repoId, String targetUserId, String adminUserId) {
        RepositoryMember targetMember = validateAdminAndGetTarget(repoId, targetUserId, adminUserId);
        memberRepository.delete(targetMember);
    }

    // 4. 권한 변경
    @Transactional
    public void updateMemberRole(Long repoId, String targetUserId, String adminUserId, UpdateRoleRequest req) {
        RepositoryMember targetMember = validateAdminAndGetTarget(repoId, targetUserId, adminUserId);
        try {
            Role newRole = Role.valueOf(req.getRole().toUpperCase());
            targetMember.setRole(newRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 Role입니다.");
        }
    }

    // [Helper] 권한 검증 및 타겟 조회
    private RepositoryMember validateAdminAndGetTarget(Long repoId, String targetUserId, String adminUserId) {
        TeamRepository repo = teamRepoRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보 오류"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자 정보 오류"));

        RepositoryMember adminMember = memberRepository.findByTeamRepositoryAndUser(repo, admin)
                .orElseThrow(() -> new SecurityException("관리자가 멤버가 아닙니다."));

        if (adminMember.getRole() != Role.ADMIN) {
            throw new SecurityException("관리자 권한이 필요합니다.");
        }

        return memberRepository.findByTeamRepositoryAndUser(repo, target)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자가 멤버가 아닙니다."));
    }
}