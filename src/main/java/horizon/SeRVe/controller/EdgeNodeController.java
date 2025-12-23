package horizon.SeRVe.controller;

import horizon.SeRVe.dto.edge.RegisterEdgeNodeRequest;
import horizon.SeRVe.service.EdgeNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/edge-nodes")
@RequiredArgsConstructor
public class EdgeNodeController {

    private final EdgeNodeService edgeNodeService;

    /**
     * 로봇(EdgeNode) 등록
     *
     * POST /edge-nodes/register
     *
     * Request Body:
     * {
     *   "serialNumber": "ROBOT-001",
     *   "apiToken": "secret-token",
     *   "publicKey": "...",
     *   "teamId": "team-uuid"
     * }
     *
     * @param request 등록 정보
     * @return 등록된 NodeId
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerEdgeNode(@Valid @RequestBody RegisterEdgeNodeRequest request) {
        String nodeId = edgeNodeService.registerEdgeNode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nodeId);
    }

    /**
     * 로봇의 팀 키 조회
     *
     * GET /edge-nodes/{nodeId}/team-key
     *
     * @param nodeId 로봇 ID
     * @return 암호화된 팀 키
     */
    @GetMapping("/{nodeId}/team-key")
    public ResponseEntity<String> getTeamKey(@PathVariable String nodeId) {
        String encryptedTeamKey = edgeNodeService.getTeamKey(nodeId);
        return ResponseEntity.ok(encryptedTeamKey);
    }
}
