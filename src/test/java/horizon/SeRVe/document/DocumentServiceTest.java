package horizon.SeRVe.document;

import horizon.SeRVe.dto.document.DocumentResponse;
import horizon.SeRVe.dto.document.EncryptedDataResponse;
import horizon.SeRVe.dto.document.UploadDocumentRequest;
import horizon.SeRVe.entity.*;
import horizon.SeRVe.repository.*;
import horizon.SeRVe.service.DocumentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Mockito 프레임워크 사용 설정
class DocumentServiceTest {

    @InjectMocks
    private DocumentService documentService; // 테스트할 대상 (우리가 짠 서비스)

    // 가짜(Mock) 저장소들 (실제 DB 대신 동작함)
    @Mock private DocumentRepository documentRepository;
    @Mock private EncryptedDataRepository encryptedDataRepository;
    @Mock private TeamRepository teamRepository; // 기존: TeamRepoRepository
    @Mock private UserRepository userRepository;
    @Mock private MemberRepository memberRepository;

    @Test
    @DisplayName("문서 업로드 성공 테스트")
    void uploadDocument_Success() {
        // given (준비)
        String teamId = "team-1"; // 기존: repoId → teamId
        String userId = "user-1";
        String sampleBase64 = Base64.getEncoder().encodeToString("test-content".getBytes());
        UploadDocumentRequest request = new UploadDocumentRequest("test.pdf", "pdf", sampleBase64);

        // 가짜 객체 행동 정의 (Stubbing): "이런 ID로 찾으면 이런 객체를 줘라"
        Team mockTeam = mock(Team.class); // 기존: TeamRepository → Team
        User mockUser = mock(User.class);
        RepositoryMember mockMember = mock(RepositoryMember.class);

        // 기존: findByRepoId → findByTeamId
        given(teamRepository.findByTeamId(teamId)).willReturn(Optional.of(mockTeam));
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(memberRepository.findByTeamAndUser(mockTeam, mockUser)).willReturn(Optional.of(mockMember));
        given(mockMember.getRole()).willReturn(Role.ADMIN); // ADMIN 권한 부여

        // when (실행)
        documentService.uploadDocument(teamId, userId, request);

        // then (검증)
        // documentRepository.save()가 딱 1번 호출되었는지 확인
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    @DisplayName("문서 목록 조회 성공 테스트")
    void getDocuments_Success() {
        // given
        String teamId = "team-1"; // 기존: repoId
        String userId = "user-1";
        Team mockTeam = mock(Team.class); // 기존: TeamRepository
        User mockRequester = mock(User.class);
        User mockUploader = mock(User.class);
        given(mockUploader.getUserId()).willReturn("uploader-1"); // 업로더 ID 요청 시 반환값 설정

        // 문서 엔티티 가짜 데이터 생성
        Document doc1 = Document.builder()
                .documentId("doc-1")
                .originalFileName("file1.pdf")
                .fileType("pdf")
                .uploader(mockUploader) // 가짜 업로더 주입
                .build();

        // 기존: findByRepoId → findByTeamId
        given(teamRepository.findByTeamId(teamId)).willReturn(Optional.of(mockTeam));
        given(userRepository.findById(userId)).willReturn(Optional.of(mockRequester));
        given(memberRepository.existsByTeamAndUser(mockTeam, mockRequester)).willReturn(true);
        // 기존: findAllByTeamRepository → findAllByTeam
        given(documentRepository.findAllByTeam(mockTeam)).willReturn(List.of(doc1));

        // when
        List<DocumentResponse> result = documentService.getDocuments(teamId, userId);

        // then
        assertEquals(1, result.size());
        assertEquals("file1.pdf", result.get(0).getFileName());
        assertEquals("uploader-1", result.get(0).getUploaderId());
    }

    @Test
    @DisplayName("데이터 다운로드 성공 - 멤버일 경우")
    void getData_Success() {
        // given
        String docId = "doc-1";
        String userId = "user-1";

        Team mockTeam = mock(Team.class);
        Document mockDoc = mock(Document.class);
        User mockUser = mock(User.class);
        EncryptedData mockData = EncryptedData.builder()
                .document(mockDoc)
                .encryptedBlob("secret".getBytes())
                .version(1)
                .build();

        given(documentRepository.findByDocumentId(docId)).willReturn(Optional.of(mockDoc));
        given(mockDoc.getTeam()).willReturn(mockTeam); // Document의 Team 설정
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(userRepository.existsById(userId)).willReturn(true); // User 존재 확인
        // ★ 핵심: 멤버십 확인 결과가 'True'라고 가정
        // 기존: existsByTeamRepositoryAndUser → existsByTeamAndUser
        given(memberRepository.existsByTeamAndUser(mockTeam, mockUser)).willReturn(true);
        given(encryptedDataRepository.findByDocument(mockDoc)).willReturn(Optional.of(mockData));

        // when
        EncryptedDataResponse response = documentService.getData(docId, userId);

        // then
        assertNotNull(response);
        assertArrayEquals("secret".getBytes(), response.getEncryptedBlob());
    }

    @Test
    @DisplayName("데이터 다운로드 실패 - 멤버가 아닐 경우")
    void getData_Fail_NotMember() {
        // given
        String docId = "doc-1";
        String userId = "intruder"; // 침입자

        Team mockTeam = mock(Team.class);
        Document mockDoc = mock(Document.class);
        User mockUser = mock(User.class);

        given(documentRepository.findByDocumentId(docId)).willReturn(Optional.of(mockDoc));
        given(mockDoc.getTeam()).willReturn(mockTeam); // Document의 Team 설정
        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(userRepository.existsById(userId)).willReturn(true); // User 존재 확인

        // ★ 핵심: 멤버십 확인 결과가 'False'라고 가정
        // 기존: existsByTeamRepositoryAndUser → existsByTeamAndUser
        given(memberRepository.existsByTeamAndUser(mockTeam, mockUser)).willReturn(false);

        // when & then (예외 발생 확인)
        assertThrows(SecurityException.class, () -> {
            documentService.getData(docId, userId);
        });
    }
}
