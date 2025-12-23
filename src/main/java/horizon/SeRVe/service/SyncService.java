package horizon.SeRVe.service;

import horizon.SeRVe.dto.sync.ChangedDocumentResponse;
import horizon.SeRVe.entity.Document;
import horizon.SeRVe.entity.Team;
import horizon.SeRVe.repository.DocumentRepository;
import horizon.SeRVe.repository.TeamRepository;
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

    /**
     * 변경된 문서 목록 조회 (증분 동기화)
     *
     * @param teamId 팀 ID
     * @param lastSyncVersion 마지막 동기화 버전 (0이면 전체 조회)
     * @return 변경된 문서 목록
     */
    @Transactional(readOnly = true)
    public List<ChangedDocumentResponse> getChangedDocuments(String teamId, int lastSyncVersion) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀입니다."));

        // 팀의 모든 문서 조회
        List<Document> allDocuments = documentRepository.findAllByTeam(team);

        // 버전 필터링 (version > lastSyncVersion)
        return allDocuments.stream()
                .filter(doc -> doc.getEncryptedData() != null &&
                        doc.getEncryptedData().getVersion() > lastSyncVersion)
                .map(ChangedDocumentResponse::from)
                .collect(Collectors.toList());
    }
}
