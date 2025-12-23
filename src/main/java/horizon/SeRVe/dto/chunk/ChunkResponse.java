package horizon.SeRVe.dto.chunk;

import horizon.SeRVe.entity.VectorChunk;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChunkResponse {
    private String chunkId;
    private int chunkIndex;
    private byte[] encryptedBlob;
    private int version;

    public static ChunkResponse from(VectorChunk chunk) {
        return ChunkResponse.builder()
                .chunkId(chunk.getChunkId())
                .chunkIndex(chunk.getChunkIndex())
                .encryptedBlob(chunk.getEncryptedBlob())
                .version(chunk.getVersion())
                .build();
    }
}
