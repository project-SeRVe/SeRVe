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

    /**
     * A. 청크 업로드 (배치)
     * - ADMIN 권한 필요
     * - 기존 chunk_index 존재 시 UPDATE, 없으면 INSERT
     */
    @Transactional
    public void uploadChunks(String documentId, String userId, ChunkUploadRequest request) {
        // 1. Document 조회
        Document document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        Team team = document.getTeam();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. ADMIN 권한 체크
        RepositoryMember member = memberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        if (member.getRole() != Role.ADMIN) {
            throw new SecurityException("청크 업로드는 ADMIN 권한이 필요합니다.");
        }

        // 3. 각 청크 처리 (UPDATE or INSERT)
        for (ChunkUploadItem item : request.getChunks()) {
            byte[] blobData = Base64.getDecoder().decode(item.getEncryptedBlob());

            Optional<VectorChunk> existingChunk = vectorChunkRepository
                    .findByDocumentIdAndChunkIndex(documentId, item.getChunkIndex());

            if (existingChunk.isPresent()) {
                // UPDATE: 기존 청크 내용 갱신 (version 자동 증가)
                VectorChunk chunk = existingChunk.get();
                chunk.updateContent(blobData);
                chunk.setDeleted(false); // 재업로드 시 삭제 플래그 해제
            } else {
                // INSERT: 새 청크 생성 (version = 1)
                VectorChunk newChunk = VectorChunk.builder()
                        .chunkId(UUID.randomUUID().toString())
                        .documentId(documentId)
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
     * B. 청크 다운로드
     * - ADMIN 또는 MEMBER 권한 허용
     * - is_deleted = false인 청크만 반환
     */
    @Transactional(readOnly = true)
    public List<ChunkResponse> getChunks(String documentId, String userId) {
        // 1. Document 조회
        Document document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 멤버십 체크 (ADMIN 또는 MEMBER)
        if (!memberRepository.existsByTeamAndUser(document.getTeam(), user)) {
            throw new SecurityException("저장소 멤버가 아닙니다.");
        }

        // 3. 삭제되지 않은 청크만 조회 (chunk_index 순 정렬)
        List<VectorChunk> chunks = vectorChunkRepository
                .findByDocumentIdAndIsDeletedOrderByChunkIndexAsc(documentId, false);

        return chunks.stream()
                .map(ChunkResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * C. 청크 삭제 (논리적 삭제)
     * - ADMIN 권한 필요
     */
    @Transactional
    public void deleteChunk(String documentId, int chunkIndex, String userId) {
        // 1. Document 조회
        Document document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. ADMIN 권한 체크
        RepositoryMember member = memberRepository.findByTeamAndUser(document.getTeam(), user)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        if (member.getRole() != Role.ADMIN) {
            throw new SecurityException("청크 삭제는 ADMIN 권한이 필요합니다.");
        }

        // 3. 청크 논리적 삭제 (version 자동 증가)
        VectorChunk chunk = vectorChunkRepository
                .findByDocumentIdAndChunkIndex(documentId, chunkIndex)
                .orElseThrow(() -> new IllegalArgumentException("청크를 찾을 수 없습니다."));

        chunk.markAsDeleted();
    }

    /**
     * D. 문서별 증분 동기화
     * - ADMIN 또는 MEMBER 권한 허용
     * - version > lastVersion인 청크 조회 (삭제된 것 포함)
     */
    @Transactional(readOnly = true)
    public List<ChunkSyncResponse> syncDocumentChunks(String documentId, int lastVersion, String userId) {
        // 1. Document 조회
        Document document = documentRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 멤버십 체크
        if (!memberRepository.existsByTeamAndUser(document.getTeam(), user)) {
            throw new SecurityException("저장소 멤버가 아닙니다.");
        }

        // 3. 변경된 청크 조회 (삭제된 것 포함)
        List<VectorChunk> chunks = vectorChunkRepository
                .findByDocumentIdAndVersionGreaterThanOrderByChunkIndexAsc(documentId, lastVersion);

        return chunks.stream()
                .map(ChunkSyncResponse::from)
                .collect(Collectors.toList());
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

        return chunks.stream()
                .map(ChunkSyncResponse::from)
                .collect(Collectors.toList());
    }
}
