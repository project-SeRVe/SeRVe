package horizon.SeRVe.repository;

import horizon.SeRVe.entity.VectorChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VectorChunkRepository extends JpaRepository<VectorChunk, String> {

    /**
     * 문서별 청크 조회 (삭제 여부 필터링, chunk_index 순 정렬)
     */
    List<VectorChunk> findByDocumentIdAndIsDeletedOrderByChunkIndexAsc(String documentId, boolean isDeleted);

    /**
     * 문서별 증분 동기화 (version > lastVersion)
     */
    List<VectorChunk> findByDocumentIdAndVersionGreaterThanOrderByChunkIndexAsc(String documentId, int lastVersion);

    /**
     * 팀별 증분 동기화 (version > lastVersion)
     */
    List<VectorChunk> findByTeamIdAndVersionGreaterThanOrderByVersionAsc(String teamId, int lastVersion);

    /**
     * 특정 청크 조회 (업데이트 시 사용)
     */
    Optional<VectorChunk> findByDocumentIdAndChunkIndex(String documentId, int chunkIndex);

    /**
     * 문서의 모든 청크 조회 (문서 삭제 시 사용)
     */
    List<VectorChunk> findByDocumentId(String documentId);
}
