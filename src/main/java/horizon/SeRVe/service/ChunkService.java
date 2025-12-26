package horizon.SeRVe.service;

import horizon.SeRVe.dto.chunk.*;
import horizon.SeRVe.entity.*;
import horizon.SeRVe.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChunkService {

    private final VectorChunkRepository vectorChunkRepository;
    private final DocumentRepository documentRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final RateLimitService rateLimitService;

    /**
     * A. 청크 업로드 (배치)
     * - ADMIN 권한 필요
     * - fileName으로 Document 찾거나 생성
     * - 기존 chunk_index 존재 시 UPDATE, 없으면 INSERT
     */
    @Transactional
    public void uploadChunks(String teamId, String fileName, String userId, ChunkUploadRequest request) {
        // 1. Team 조회
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1-1. Rate Limit 체크 (악의적인 대량 업로드 방지)
        rateLimitService.checkAndRecordUpload(userId);

        // 2. 멤버십 및 권한 체크 (Federated Model: MEMBER 전용, ADMIN은 Key Master 역할만)
        RepositoryMember member = memberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        // ADMIN은 업로드 금지 (Key Master 역할만 수행)
        if (member.getRole() == Role.ADMIN) {
            throw new SecurityException("ADMIN은 데이터 업로드가 불가능합니다. MEMBER만 업로드할 수 있습니다.");
        }

        // 3. Document 찾거나 생성
        Optional<Document> existingDoc = documentRepository.findByTeamAndOriginalFileName(team, fileName);
        Document document;

        if (existingDoc.isPresent()) {
            document = existingDoc.get();
            // 3-1. 기존 문서가 있으면 uploader 검증 (타인의 문서 수정 방지)
            if (!document.getUploader().getUserId().equals(user.getUserId())) {
                throw new SecurityException(
                    String.format("타인의 문서를 수정할 수 없습니다. (원본 업로더: %s, 현재 사용자: %s)",
                            document.getUploader().getEmail(), user.getEmail())
                );
            }
        } else {
            // 3-2. 새 문서 생성
            document = Document.builder()
                    .documentId(UUID.randomUUID().toString())
                    .team(team)
                    .uploader(user)
                    .originalFileName(fileName)
                    .fileType("application/octet-stream") // 기본값
                    .build();
            document = documentRepository.save(document);
        }

        // 4. 각 청크 처리 (UPDATE or INSERT)
        for (ChunkUploadItem item : request.getChunks()) {
            byte[] blobData = Base64.getDecoder().decode(item.getEncryptedBlob());

            Optional<VectorChunk> existingChunk = vectorChunkRepository
                    .findByDocumentIdAndChunkIndex(document.getDocumentId(), item.getChunkIndex());

            if (existingChunk.isPresent()) {
                // UPDATE: 기존 청크 내용 갱신 (version 자동 증가)
                VectorChunk chunk = existingChunk.get();
                chunk.updateContent(blobData);
                chunk.setDeleted(false); // 재업로드 시 삭제 플래그 해제
            } else {
                // INSERT: 새 청크 생성 (version = 0)
                VectorChunk newChunk = VectorChunk.builder()
                        .chunkId(UUID.randomUUID().toString())
                        .documentId(document.getDocumentId())
                        .teamId(team.getTeamId())
                        .chunkIndex(item.getChunkIndex())
                        .encryptedBlob(blobData)
                        .isDeleted(false)
                        .build();
                vectorChunkRepository.save(newChunk);
            }
        }
    }

    /**
     * C. 청크 삭제 (논리적 삭제)
     * - ADMIN 권한 필요
     * - fileName으로 Document 조회
     */
    @Transactional
    public void deleteChunk(String teamId, String fileName, int chunkIndex, String userId) {
        // 1. Team 조회
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. ADMIN 권한 체크
        RepositoryMember member = memberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        if (member.getRole() != Role.ADMIN) {
            throw new SecurityException("청크 삭제는 ADMIN 권한이 필요합니다.");
        }

        // 3. Document 조회
        Document document = documentRepository.findByTeamAndOriginalFileName(team, fileName)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        // 4. 청크 논리적 삭제 (version 자동 증가)
        VectorChunk chunk = vectorChunkRepository
                .findByDocumentIdAndChunkIndex(document.getDocumentId(), chunkIndex)
                .orElseThrow(() -> new IllegalArgumentException("청크를 찾을 수 없습니다."));

        chunk.markAsDeleted();
    }

    /**
     * E. 팀별 증분 동기화
     * - ADMIN 또는 MEMBER 권한 허용
     * - 해당 팀의 모든 문서에서 version > lastVersion인 청크 조회
     */
    @Transactional(readOnly = true)
    public List<ChunkSyncResponse> syncTeamChunks(String teamId, int lastVersion, String userId) {
        // 1. Team 조회
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 팀 멤버십 체크
        if (!memberRepository.existsByTeamAndUser(team, user)) {
            throw new SecurityException("팀 멤버가 아닙니다.");
        }

        // 3. 팀의 모든 문서에서 변경된 청크 조회
        List<VectorChunk> chunks = vectorChunkRepository
                .findByTeamIdAndVersionGreaterThanOrderByVersionAsc(teamId, lastVersion);

        // 4. Document 정보 조회 (N+1 방지: IN 쿼리 사용)
        List<String> documentIds = chunks.stream()
                .map(VectorChunk::getDocumentId)
                .distinct()
                .collect(Collectors.toList());

        // Document ID → Uploader Email 매핑 생성
        java.util.Map<String, String> documentUploaderMap = documentRepository
                .findAllByDocumentIdIn(documentIds)
                .stream()
                .collect(Collectors.toMap(
                        Document::getDocumentId,
                        doc -> doc.getUploader().getEmail()
                ));

        // 5. ChunkSyncResponse 생성 (createdBy 포함)
        return chunks.stream()
                .map(chunk -> {
                    String createdBy = documentUploaderMap.getOrDefault(chunk.getDocumentId(), "unknown");
                    return ChunkSyncResponse.from(chunk, createdBy);
                })
                .collect(Collectors.toList());
    }
}
