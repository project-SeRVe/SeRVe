package horizon.SeRVe.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryMemberId implements Serializable {
    private Long repoId;
    private String userId;
}