package horizon.SeRVe.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 키 로테이션 시 DEK 재암호화 요청 DTO
 * Envelope Encryption: 청크 데이터는 변경하지 않고 DEK만 새 팀 키로 재암호화
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReencryptKeysRequest {
    private List<DocumentKeyUpdate> documents;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentKeyUpdate {
        private String documentId;
        private String newEncryptedDEK; // Base64 encoded, 새 팀 키로 암호화된 DEK
    }
}
