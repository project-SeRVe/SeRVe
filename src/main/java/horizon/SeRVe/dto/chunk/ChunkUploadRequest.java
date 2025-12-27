package horizon.SeRVe.dto.chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadRequest {
    private String fileName; // 파일 이름 (예: "설비매뉴얼.pdf")
    private List<ChunkUploadItem> chunks; // 배치 업로드
    private String encryptedDEK; // Envelope Encryption: 팀 키로 암호화된 DEK (Base64)
}
