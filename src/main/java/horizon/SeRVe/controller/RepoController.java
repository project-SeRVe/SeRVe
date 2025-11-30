package horizon.SeRVe.controller;

import horizon.SeRVe.service.RepoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepoController {

    private final RepoService repoService;

    // DTO (내부 클래스로 간단히 정의)
    @Data
    public static class CreateRepoRequest {
        private String name;
        private String description;
        private String ownerId; // 임시: 원래는 토큰에서 꺼내야 함
        private String encryptedTeamKey;
    }

    @PostMapping
    public ResponseEntity<Long> createRepository(@RequestBody CreateRepoRequest request) {
        Long repoId = repoService.createRepository(
                request.getName(),
                request.getDescription(),
                request.getOwnerId(),
                request.getEncryptedTeamKey()
        );
        return ResponseEntity.ok(repoId);
    }
}