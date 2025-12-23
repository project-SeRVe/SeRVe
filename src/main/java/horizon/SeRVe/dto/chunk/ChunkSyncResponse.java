package horizon.SeRVe.dto.chunk;

import horizon.SeRVe.entity.VectorChunk;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChunkSyncResponse {
    private String documentId; // 팀 동기화 시 필요
    private String chunkId;
    private int chunkIndex;
    private byte[] encryptedBlob;
    private int version;
    private boolean isDeleted;

    public static ChunkSyncResponse from(VectorChunk chunk) {
        return ChunkSyncResponse.builder()
                .documentId(chunk.getDocumentId())
                .chunkId(chunk.getChunkId())
                .chunkIndex(chunk.getChunkIndex())
                .encryptedBlob(chunk.getEncryptedBlob())
                .version(chunk.getVersion())
                .isDeleted(chunk.isDeleted())
                .build();
    }
}
