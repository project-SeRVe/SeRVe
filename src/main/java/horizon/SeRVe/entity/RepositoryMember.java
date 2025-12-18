package horizon.SeRVe.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "repository_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// @IdClass(RepositoryMemberId.class) // 복합키 설정은 복잡하니까 임시로 생략하거나 간단히 처리
public class RepositoryMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private TeamRepository teamRepository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Role role; // ADMIN, MEMBER
}