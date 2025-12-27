package horizon.SeRVe.controller;

import horizon.SeRVe.dto.document.DocumentResponse;
import horizon.SeRVe.dto.document.EncryptedDataResponse;
import horizon.SeRVe.dto.document.ReencryptKeysRequest;
import horizon.SeRVe.dto.document.UploadDocumentRequest;
import horizon.SeRVe.entity.User;
import horizon.SeRVe.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * [Modified] 문서 업로드
     * - URL: /api/teams/{teamId}/documents (기존: /api/repositories/{repoId}/documents)
     * - Body: UploadDocumentRequest (DTO 변경)
     */
    @PostMapping("/api/teams/{teamId}/documents") // 기존: /api/repositories/{repoId}/documents
    public ResponseEntity<Void> uploadDocument(
            @PathVariable String teamId, // 기존: repoId
            @AuthenticationPrincipal User user,
            @RequestBody UploadDocumentRequest request) {

        // 수정된 Service 메서드 호출
        documentService.uploadDocument(teamId, user.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    // 문서 목록 조회
    @GetMapping("/api/teams/{teamId}/documents") // 기존: /api/repositories/{repoId}/documents
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @PathVariable String teamId, // 기존: repoId
            @AuthenticationPrincipal User user) {

        List<DocumentResponse> response = documentService.getDocuments(teamId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    // [REMOVED] 다운로드 기능 제거 - Federated Model에서는 동기화만 사용
    // 기존 GET /api/documents/{docId}/data 엔드포인트 삭제됨
    // 대신 GET /api/sync/chunks 사용

    // 문서 삭제
    @DeleteMapping("/api/teams/{teamId}/documents/{docId}") // 기존: /api/repositories/{repoId}/documents/{docId}
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String teamId, // 기존: repoId
            @PathVariable String docId,
            @AuthenticationPrincipal User user) {

        documentService.deleteDocument(docId, user.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * DEK 재암호화 (키 로테이션 시 사용)
     * - Envelope Encryption: 청크 데이터는 변경하지 않고 DEK만 새 팀 키로 재암호화
     * - ADMIN 권한 필요
     */
    @PostMapping("/api/teams/{teamId}/documents/reencrypt-keys")
    public ResponseEntity<Void> reencryptDocumentKeys(
            @PathVariable String teamId,
            @AuthenticationPrincipal User user,
            @RequestBody ReencryptKeysRequest request) {

        documentService.reencryptDocumentKeys(teamId, user.getUserId(), request);
        return ResponseEntity.ok().build();
    }
}
