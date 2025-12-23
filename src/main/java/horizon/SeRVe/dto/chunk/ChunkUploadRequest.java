package horizon.SeRVe.dto.chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadRequest {
    private List<ChunkUploadItem> chunks; // 배치 업로드
}
