package horizon.SeRVe.dto.chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadItem {
    private int chunkIndex;
    private String encryptedBlob; // Base64 String
}
