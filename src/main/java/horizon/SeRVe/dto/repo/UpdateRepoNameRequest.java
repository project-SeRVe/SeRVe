package horizon.SeRVe.dto.repo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRepoNameRequest {

    // 변경할 새로운 저장소 이름
    private String name;
}