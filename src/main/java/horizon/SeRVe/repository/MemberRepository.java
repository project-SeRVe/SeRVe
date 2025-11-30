package horizon.SeRVe.repository;

import horizon.SeRVe.entity.RepositoryMember;
import horizon.SeRVe.entity.RepositoryMemberId; // 복합키 클래스 Import
import horizon.SeRVe.entity.TeamRepository;
import horizon.SeRVe.entity.User;
import org.springframework.data.jpa.repository.EntityGraph; // 성능 최적화 Import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<RepositoryMember, RepositoryMemberId> {

    /**
     * 1. 내가 속한 모든 저장소 조회 (Join 최적화)
     * 멤버 정보 조회 시 연관된 teamRepository 정보를 함께 가져옵니다.
     */
    @EntityGraph(attributePaths = {"teamRepository"})
    List<RepositoryMember> findAllByUser(User user);

    /**
     * 2. 특정 저장소의 멤버 목록 조회 (Join 최적화)
     * 멤버 목록 조회 시 연관된 User 정보를 함께 가져옵니다 (이메일, ID 등).
     */
    @EntityGraph(attributePaths = {"user"})
    List<RepositoryMember> findAllByTeamRepository(TeamRepository teamRepository);

    /**
     * 3. 특정 저장소에서 특정 유저의 멤버 정보 확인 (권한 체크용)
     */
    Optional<RepositoryMember> findByTeamRepositoryAndUser(TeamRepository teamRepository, User user);

    /**
     * 4. 이미 해당 저장소의 멤버인지 확인 (중복 초대 방지)
     */
    boolean existsByTeamRepositoryAndUser(TeamRepository teamRepository, User user);
}