package horizon.SeRVe.repository;

import horizon.SeRVe.entity.RepositoryMember;
import horizon.SeRVe.entity.TeamRepository;
import horizon.SeRVe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<RepositoryMember, Long> {

    // 멤버십 확인용
    boolean existsByTeamRepositoryAndUser(TeamRepository teamRepository, User user);

    // 권한 확인용
    Optional<RepositoryMember> findByTeamRepositoryAndUser(TeamRepository teamRepository, User user);
}