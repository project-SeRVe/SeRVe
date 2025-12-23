package horizon.SeRVe.service;

import horizon.SeRVe.dto.edge.RegisterEdgeNodeRequest;
import horizon.SeRVe.entity.EdgeNode;
import horizon.SeRVe.entity.Team;
import horizon.SeRVe.repository.EdgeNodeRepository;
import horizon.SeRVe.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EdgeNodeService {

    private final EdgeNodeRepository edgeNodeRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 로봇(EdgeNode) 등록
     *
     * @param request 등록 정보 (serialNumber, apiToken, publicKey, teamId)
     * @return 등록된 NodeId
     */
    @Transactional
    public String registerEdgeNode(RegisterEdgeNodeRequest request) {
        // 1. 시리얼 번호 중복 체크
        if (edgeNodeRepository.findBySerialNumber(request.getSerialNumber()).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 시리얼 번호입니다.");
        }

        // 2. 팀 존재 확인
        Team team = teamRepository.findByTeamId(request.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀입니다."));

        // 3. API 토큰 해싱
        String hashedToken = passwordEncoder.encode(request.getApiToken());

        // 4. EdgeNode 생성 및 저장
        EdgeNode edgeNode = EdgeNode.builder()
                .nodeId(UUID.randomUUID().toString())
                .serialNumber(request.getSerialNumber())
                .hashedToken(hashedToken)
                .publicKey(request.getPublicKey())
                .encryptedTeamKey(request.getEncryptedTeamKey()) // Optional
                .team(team)
                .build();

        edgeNodeRepository.save(edgeNode);

        return edgeNode.getNodeId();
    }

    /**
     * 로봇의 팀 키 조회
     *
     * @param nodeId 로봇 ID
     * @return 암호화된 팀 키
     */
    @Transactional(readOnly = true)
    public String getTeamKey(String nodeId) {
        EdgeNode edgeNode = edgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 로봇입니다."));

        if (edgeNode.getEncryptedTeamKey() == null) {
            throw new IllegalStateException("팀 키가 설정되지 않았습니다.");
        }

        return edgeNode.getEncryptedTeamKey();
    }
}
