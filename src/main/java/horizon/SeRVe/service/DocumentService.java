package horizon.SeRVe.service;

import horizon.SeRVe.dto.document.*;
import horizon.SeRVe.entity.*;
import horizon.SeRVe.repository.*;
import lombok.RequiredArgsConstructor;
import horizon.SeRVe.entity.VectorChunk;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EncryptedDataRepository encryptedDataRepository;
    private final TeamRepository teamRepository; // 기존: TeamRepoRepository
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final EdgeNodeRepository edgeNodeRepository;
    private final VectorChunkRepository vectorChunkRepository;

    /**
     * [Modified] 기존 uploadDocument 메서드를 수정
     * - 파라미터 변경: 개별 인자 -> DTO (UploadDocumentRequest)
     * - 로직 변경: 단일 저장 -> Document(메타) + EncryptedData(바이너리) 분리 저장
     */
    @Transactional
    public void uploadDocument(String teamId, String userId, UploadDocumentRequest req) {
        // 1. 저장소 및 유저 조회
        // 기존: findByRepoId → findByTeamId
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소를 찾을 수 없습니다."));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 멤버십 및 권한 검증 (Federated Model: MEMBER만 업로드 가능)
        RepositoryMember member = memberRepository.findByTeamAndUser(team, uploader)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        // ADMIN은 키 관리만 담당, 데이터 업로드는 MEMBER 전용
        if (member.getRole() == Role.ADMIN) {
            throw new SecurityException("ADMIN은 데이터 업로드가 불가능합니다. MEMBER만 업로드할 수 있습니다.");
        }


        // 3. 암호화 데이터(Blob) 변환 및 생성
        byte[] blobData = Base64.getDecoder().decode(req.getEncryptedBlob());

        // 같은 이름의 파일이 있는지 확인
        Optional<Document> existingDoc = documentRepository.findByTeamAndOriginalFileName(team, req.getFileName());

        if (existingDoc.isPresent()) {
            // [Case A] 이미 존재함 -> 업데이트 (Version Up)
            Document document = existingDoc.get();
            EncryptedData data = document.getEncryptedData();

            data.updateContent(blobData);

        } else {
            // [Case B] 없음 -> 신규 생성 (Version 1)
            Document document = Document.builder()
                    .documentId(UUID.randomUUID().toString())
                    .team(team)
                    .uploader(uploader)
                    .originalFileName(req.getFileName())
                    .fileType(req.getFileType())
                    .build();

            EncryptedData encryptedData = EncryptedData.builder()
                    .dataId(UUID.randomUUID().toString())
                    .document(document)
                    .encryptedBlob(blobData)
                    .build();

            document.setEncryptedData(encryptedData);
            documentRepository.save(document);
        }
    }

    // 문서 목록 조회
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(String teamId, String userId) {
        // 기존: findByRepoId → findByTeamId
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소를 찾을 수 없습니다."));

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 멤버십 검증 (ADMIN과 MEMBER 모두 조회 가능)
        if (!memberRepository.existsByTeamAndUser(team, requester)) {
            throw new SecurityException("저장소 멤버가 아닙니다.");
        }

        // 기존: findAllByTeamRepository → findAllByTeam
        return documentRepository.findAllByTeam(team).stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

    // [DEPRECATED] 다운로드 기능 제거 - Federated Model에서는 동기화만 사용
    // 이 메서드는 더 이상 사용되지 않으며, GET /api/sync/chunks 엔드포인트로 대체되었습니다.
    // 기존 코드 호환성을 위해 남겨두었으나, 향후 삭제 예정
    @Deprecated
    @Transactional(readOnly = true)
    public EncryptedDataResponse getData(String docId, String requesterId) {
        throw new UnsupportedOperationException(
            "다운로드 기능은 Federated Model에서 지원하지 않습니다. " +
            "대신 GET /api/sync/chunks 엔드포인트를 사용하세요."
        );
    }

    // 기존 구현 (참고용, 삭제 예정)
    /*
    @Transactional(readOnly = true)
    public EncryptedDataResponse getData(String docId, String requesterId) {
        Document document = documentRepository.findByDocumentId(docId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        boolean hasPermission = false;

        if (userRepository.existsById(requesterId)) {
            User user = userRepository.findById(requesterId).get();
            hasPermission = memberRepository.existsByTeamAndUser(document.getTeam(), user);
        }
        else if (edgeNodeRepository.existsById(requesterId)) {
            EdgeNode robot = edgeNodeRepository.findById(requesterId).get();
            hasPermission = robot.getTeam().getTeamId().equals(document.getTeam().getTeamId());
        }

        if (!hasPermission) {
            throw new SecurityException("접근 권한이 없습니다 (멤버 아님).");
        }

        EncryptedData data = encryptedDataRepository.findByDocument(document)
                .orElseThrow(() -> new IllegalArgumentException("데이터가 존재하지 않습니다."));

        return EncryptedDataResponse.from(data);
    }
    */

    // 문서 삭제 (ADMIN 전용)
    @Transactional
    public void deleteDocument(String docId, String userId) {
        Document document = documentRepository.findByDocumentId(docId)
                .orElseThrow(() -> new IllegalArgumentException("문서를 찾을 수 없습니다."));

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // ADMIN 권한 체크 (Physical AI: 엣지 디바이스는 삭제 권한 없음)
        RepositoryMember memberInfo = memberRepository.findByTeamAndUser(document.getTeam(), requester)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        if (memberInfo.getRole() != Role.ADMIN) {
            throw new SecurityException("문서 삭제는 ADMIN 권한이 필요합니다.");
        }

        // 연관된 청크도 논리적 삭제 처리
        List<VectorChunk> chunks = vectorChunkRepository.findByDocumentId(docId);
        chunks.forEach(chunk -> chunk.markAsDeleted());

        documentRepository.delete(document);
    }

    /**
     * DEK 재암호화 (키 로테이션 시 사용)
     * - Envelope Encryption: 청크 데이터는 변경하지 않고 DEK만 새 팀 키로 재암호화
     * - ADMIN 권한 필요
     */
    @Transactional
    public void reencryptDocumentKeys(String teamId, String userId, ReencryptKeysRequest request) {
        // 1. Team 조회
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. ADMIN 권한 체크
        RepositoryMember member = memberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("저장소 멤버가 아닙니다."));

        if (member.getRole() != Role.ADMIN) {
            throw new SecurityException("DEK 재암호화는 ADMIN 권한이 필요합니다.");
        }

        // 3. 각 문서의 DEK 업데이트
        for (ReencryptKeysRequest.DocumentKeyUpdate update : request.getDocuments()) {
            Document document = documentRepository.findByDocumentId(update.getDocumentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "문서를 찾을 수 없습니다: " + update.getDocumentId()));

            // 팀 소속 검증
            if (!document.getTeam().getTeamId().equals(teamId)) {
                throw new SecurityException(
                        "다른 팀의 문서입니다: " + update.getDocumentId());
            }

            // DEK 업데이트 (Base64 디코딩)
            byte[] newEncryptedDEK = Base64.getDecoder().decode(update.getNewEncryptedDEK());
            document.setEncryptedDEK(newEncryptedDEK);
        }
    }
}
