package horizon.SeRVe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "user_id")
    private String userId; // UUID 등 고유 식별자

    @Column(nullable = false, unique = true)
    private String email;  // DTO의 ownerEmail 매핑용

    // 실제 서비스에선 Password, PublicKey 등 필드 추가 필요
}