package horizon.SeRVe.controller;

import horizon.SeRVe.dto.document.DocumentResponse;
import horizon.SeRVe.dto.document.EncryptedDataResponse;
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

    // 데이터 다운로드
    @GetMapping("/api/documents/{docId}/data")
    public ResponseEntity<EncryptedDataResponse> downloadData(
            @PathVariable String docId,
            @AuthenticationPrincipal User user) {

        EncryptedDataResponse response = documentService.getData(docId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    // 문서 삭제
    @DeleteMapping("/api/teams/{teamId}/documents/{docId}") // 기존: /api/repositories/{repoId}/documents/{docId}
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String teamId, // 기존: repoId
            @PathVariable String docId,
            @AuthenticationPrincipal User user) {

        documentService.deleteDocument(docId, user.getUserId());
        return ResponseEntity.ok().build();
    }
}
