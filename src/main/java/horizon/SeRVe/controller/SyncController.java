package horizon.SeRVe.controller;

import horizon.SeRVe.dto.sync.ChangedDocumentResponse;
import horizon.SeRVe.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class
    SyncController {

    private final SyncService syncService;

    /**
     * 변경된 문서 목록 조회 (증분 동기화)
     *
     * GET /api/sync/documents?teamId={teamId}&lastSyncVersion={version}
     *
     * @param teamId 팀 ID
     * @param lastSyncVersion 마지막 동기화 버전 (기본값: 0 = 전체 조회)
     * @return 변경된 문서 목록
     *
     * 사용 예시:
     * - 첫 동기화: GET /api/sync/documents?teamId=xxx&lastSyncVersion=0
     * - 증분 동기화: GET /api/sync/documents?teamId=xxx&lastSyncVersion=5
     *
     * Response:
     * [
     *   {
     *     "documentId": "doc-uuid",
     *     "fileName": "file.txt",
     *     "fileType": "text/plain",
     *     "version": 6,
     *     "uploaderId": "user-uuid"
     *   },
     *   ...
     * ]
     */
    @GetMapping("/documents")
    public ResponseEntity<List<ChangedDocumentResponse>> getChangedDocuments(
            @RequestParam String teamId,
            @RequestParam(defaultValue = "0") int lastSyncVersion) {

        List<ChangedDocumentResponse> changedDocuments =
                syncService.getChangedDocuments(teamId, lastSyncVersion);

        return ResponseEntity.ok(changedDocuments);
    }
}
