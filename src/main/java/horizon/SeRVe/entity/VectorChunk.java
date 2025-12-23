package horizon.SeRVe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vector_chunks", indexes = {
    @Index(name = "idx_document_chunk", columnList = "document_id, chunk_index"),
    @Index(name = "idx_team_version", columnList = "team_id, version"),
    @Index(name = "idx_document_deleted", columnList = "document_id, is_deleted")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class VectorChunk {

    @Id
    @Column(name = "chunk_id", length = 36)
    private String chunkId;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(name = "team_id", nullable = false)
    private String teamId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Lob
    @Column(name = "encrypted_blob", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] encryptedBlob;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateContent(byte[] newBlob) {
        this.encryptedBlob = newBlob;
        // version은 @Version에 의해 자동 증가
    }

    public void markAsDeleted() {
        this.isDeleted = true;
        // version은 @Version에 의해 자동 증가
    }
}
