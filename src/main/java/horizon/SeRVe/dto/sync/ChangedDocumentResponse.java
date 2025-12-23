package horizon.SeRVe.dto.sync;

import horizon.SeRVe.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangedDocumentResponse {
    private String documentId;
    private String fileName;
    private String fileType;
    private int version;
    private String uploaderId;

    public static ChangedDocumentResponse from(Document document) {
        return ChangedDocumentResponse.builder()
                .documentId(document.getDocumentId())
                .fileName(document.getOriginalFileName())
                .fileType(document.getFileType())
                .version(document.getEncryptedData().getVersion())
                .uploaderId(document.getUploader().getUserId())
                .build();
    }
}
