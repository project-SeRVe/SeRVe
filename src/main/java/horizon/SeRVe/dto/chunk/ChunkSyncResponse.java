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
    private String createdBy; // 업로더 정보 (감사 추적용)

    public static ChunkSyncResponse from(VectorChunk chunk) {
        return ChunkSyncResponse.builder()
                .documentId(chunk.getDocumentId())
                .chunkId(chunk.getChunkId())
                .chunkIndex(chunk.getChunkIndex())
                .encryptedBlob(chunk.getEncryptedBlob())
                .version(chunk.getVersion())
                .isDeleted(chunk.isDeleted())
                .createdBy(null) // Service 레이어에서 설정
                .build();
    }

    // Service 레이어에서 createdBy를 설정할 수 있도록 정적 팩토리 메서드 추가
    public static ChunkSyncResponse from(VectorChunk chunk, String createdBy) {
        return ChunkSyncResponse.builder()
                .documentId(chunk.getDocumentId())
                .chunkId(chunk.getChunkId())
                .chunkIndex(chunk.getChunkIndex())
                .encryptedBlob(chunk.getEncryptedBlob())
                .version(chunk.getVersion())
                .isDeleted(chunk.isDeleted())
                .createdBy(createdBy)
                .build();
    }
}
