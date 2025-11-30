package horizon.SeRVe.dto.member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    // 변경할 권한 역할 (예: "ADMIN", "MEMBER")
    private String role;
}