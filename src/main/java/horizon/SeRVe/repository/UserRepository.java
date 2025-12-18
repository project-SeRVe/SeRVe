package horizon.SeRVe.repository;

import horizon.SeRVe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // 기본 findById 사용
}