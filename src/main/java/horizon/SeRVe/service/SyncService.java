package horizon.SeRVe.service;

import horizon.SeRVe.dto.sync.ChangedDocumentResponse;
import horizon.SeRVe.entity.Document;
import horizon.SeRVe.entity.Team;
import horizon.SeRVe.entity.User;
import horizon.SeRVe.repository.DocumentRepository;
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
public class SyncService {

    private final DocumentRepository documentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    /**
     * 변경된 문서 목록 조회 (증분 동기화)
     *
     * @param teamId 팀 ID
     * @param lastSyncVersion 마지막 동기화 버전 (0이면 전체 조회)
     * @param userId 요청자 ID (멤버십 검증용)
     * @return 변경된 문서 목록
     */
    @Transactional(readOnly = true)
    public List<ChangedDocumentResponse> getChangedDocuments(String teamId, int lastSyncVersion, String userId) {
        // 1. Team 조회
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀입니다."));

        // 2. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 3. 멤버십 검증 (ADMIN 또는 MEMBER만 동기화 가능)
        if (!memberRepository.existsByTeamAndUser(team, user)) {
            throw new SecurityException("팀 멤버가 아닙니다. 동기화 권한이 없습니다.");
        }

        // 4. 팀의 모든 문서 조회
        List<Document> allDocuments = documentRepository.findAllByTeam(team);

        // 5. 버전 필터링 (version > lastSyncVersion)
        return allDocuments.stream()
                .filter(doc -> doc.getEncryptedData() != null &&
                        doc.getEncryptedData().getVersion() > lastSyncVersion)
                .map(ChangedDocumentResponse::from)
                .collect(Collectors.toList());
    }
}
