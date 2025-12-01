package horizon.SeRVe.service;

import horizon.SeRVe.entity.TeamRepository;
import horizon.SeRVe.repository.TeamRepoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RepoServiceTest {

    @InjectMocks
    private RepoService repoService;

    @Mock
    private TeamRepoRepository teamRepoRepository;

    @Test
    @DisplayName("저장소 생성 성공")
    void createRepository_Success() {
        // given
        String name = "MyProject";
        String description = "Test Repo";
        String ownerId = "user1";

        TeamRepository mockRepo = new TeamRepository(name, description, ownerId);
        mockRepo.setId(1L); // ID가 생성되었다고 가정

        given(teamRepoRepository.findByName(name)).willReturn(Optional.empty()); // 중복 없음
        given(teamRepoRepository.save(any(TeamRepository.class))).willReturn(mockRepo);

        // when
        Long repoId = repoService.createRepository(name, description, ownerId);

        // then
        assertEquals(1L, repoId);
    }

    @Test
    @DisplayName("중복된 이름으로 생성 시 실패")
    void createRepository_DuplicateName_Fail() {
        // given
        String name = "MyProject";
        given(teamRepoRepository.findByName(name)).willReturn(Optional.of(new TeamRepository()));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            repoService.createRepository(name, "desc", "user1");
        });
    }
}