package horizon.SeRVe.controller;

import horizon.SeRVe.dto.chunk.ChunkResponse;
import horizon.SeRVe.dto.chunk.ChunkSyncResponse;
import horizon.SeRVe.dto.chunk.ChunkUploadRequest;
import horizon.SeRVe.entity.User;
import horizon.SeRVe.service.ChunkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChunkController {

    private final ChunkService chunkService;

    /**
     * A. 청크 업로드 (배치)
     * POST /api/documents/{documentId}/chunks
     */
    @PostMapping("/api/documents/{documentId}/chunks")
    public ResponseEntity<Void> uploadChunks(
            @PathVariable String documentId,
            @AuthenticationPrincipal User user,
            @RequestBody ChunkUploadRequest request) {

        chunkService.uploadChunks(documentId, user.getUserId(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * B. 청크 다운로드
     * GET /api/documents/{documentId}/chunks
     */
    @GetMapping("/api/documents/{documentId}/chunks")
    public ResponseEntity<List<ChunkResponse>> getChunks(
            @PathVariable String documentId,
            @AuthenticationPrincipal User user) {

        List<ChunkResponse> response = chunkService.getChunks(documentId, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * C. 청크 삭제 (논리적 삭제)
     * DELETE /api/documents/{documentId}/chunks/{chunkIndex}
     */
    @DeleteMapping("/api/documents/{documentId}/chunks/{chunkIndex}")
    public ResponseEntity<Void> deleteChunk(
            @PathVariable String documentId,
            @PathVariable int chunkIndex,
            @AuthenticationPrincipal User user) {

        chunkService.deleteChunk(documentId, chunkIndex, user.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * D. 문서별 증분 동기화
     * GET /api/documents/{documentId}/chunks/sync?lastVersion={n}
     */
    @GetMapping("/api/documents/{documentId}/chunks/sync")
    public ResponseEntity<List<ChunkSyncResponse>> syncDocumentChunks(
            @PathVariable String documentId,
            @RequestParam(defaultValue = "0") int lastVersion,
            @AuthenticationPrincipal User user) {

        List<ChunkSyncResponse> response = chunkService.syncDocumentChunks(
                documentId, lastVersion, user.getUserId());
        return ResponseEntity.ok(response);
    }

    /**
     * E. 팀별 증분 동기화
     * GET /api/sync/chunks?teamId={id}&lastVersion={n}
     */
    @GetMapping("/api/sync/chunks")
    public ResponseEntity<List<ChunkSyncResponse>> syncTeamChunks(
            @RequestParam String teamId,
            @RequestParam(defaultValue = "0") int lastVersion,
            @AuthenticationPrincipal User user) {

        List<ChunkSyncResponse> response = chunkService.syncTeamChunks(
                teamId, lastVersion, user.getUserId());
        return ResponseEntity.ok(response);
    }
}
