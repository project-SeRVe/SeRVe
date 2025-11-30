package horizon.SeRVe.dto.repo;

import horizon.SeRVe.entity.RepoType;
import horizon.SeRVe.entity.TeamRepository;
import horizon.SeRVe.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RepoResponse {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String ownerId;
    private String ownerEmail;

    /**
     * Entity -> DTO 변환
     * TeamRepository에는 ownerId(String)만 있으므로,
     * Service에서 조회한 User 객체(owner)를 함께 받아야 이메일을 채울 수 있음.
     */
    public static RepoResponse of(TeamRepository repo, User owner) {
        return RepoResponse.builder()
                .id(repo.getId())
                .name(repo.getName())
                .description(repo.getDescription())
                .type(repo.getType() != null ? repo.getType().name() : RepoType.TEAM.name())
                .ownerId(repo.getOwnerId()) // String 그대로 사용
                .ownerEmail(owner.getEmail()) // 별도로 받은 User 객체에서 추출
                .build();
    }
}