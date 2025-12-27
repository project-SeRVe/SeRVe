package horizon.SeRVe.config;

import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Rate Limiting 필터 (Token Bucket 알고리즘)
 *
 * 각 사용자/IP별로 API 호출 빈도를 제한하여 DDoS 공격을 방지합니다.
 *
 * Rate Limit 정책:
 * - 일반 API: 60 req/min per user
 * - 업로드 API: 20 req/min per user
 * - 다운로드 API: 40 req/min per user
 */
@Slf4j
// @Component  // 테스트를 위해 임시 비활성화
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> bucketCache;

    /**
     * Rate Limit 정책 정의
     */
    private static final int DEFAULT_CAPACITY = 60;
    private static final int DEFAULT_REFILL = 60;
    private static final int UPLOAD_CAPACITY = 20;
    private static final int UPLOAD_REFILL = 20;
    private static final int DOWNLOAD_CAPACITY = 40;
    private static final int DOWNLOAD_REFILL = 40;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 인증 불필요 엔드포인트는 Rate Limit 제외
        String path = request.getRequestURI();
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 사용자 식별자 추출 (JWT 토큰의 userId 또는 IP 주소)
        String identifier = getUserIdentifier(request);

        // Bucket 가져오기 또는 생성
        Bucket bucket = bucketCache.get(identifier, key -> createBucket(path));

        // Rate Limit 체크
        if (bucket.tryConsume(1)) {
            // 허용
            filterChain.doFilter(request, response);
        } else {
            // 거부 (Too Many Requests)
            log.warn("[Rate Limit] 요청 거부 - User: {}, Path: {}", identifier, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Too many requests\", \"message\": \"Rate limit exceeded. Please try again later.\"}"
            );
        }
    }

    /**
     * Rate Limit 제외 경로 체크
     */
    private boolean isExcludedPath(String path) {
        return path.startsWith("/auth/") ||
               path.startsWith("/api/security/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/h2-console/");
    }

    /**
     * 사용자 식별자 추출 (JWT에서 userId 추출 또는 IP 주소 사용)
     */
    private String getUserIdentifier(HttpServletRequest request) {
        // 1. JWT 토큰에서 userId 추출 시도
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // JwtTokenProvider에서 userId를 추출할 수 있지만, 여기서는 간단히 IP 기반으로 처리
            // 실제로는 JwtTokenProvider를 주입받아서 사용해야 함
        }

        // 2. IP 주소 기반 (Fallback)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    /**
     * 경로별 Bucket 생성 (Token Bucket 알고리즘)
     */
    private Bucket createBucket(String path) {
        Bandwidth bandwidth;

        if (path.contains("/documents") && path.contains("upload")) {
            // 업로드 API: 20 req/min
            bandwidth = Bandwidth.classic(UPLOAD_CAPACITY, Refill.intervally(UPLOAD_REFILL, Duration.ofMinutes(1)));
        } else if (path.contains("/documents") && path.contains("download")) {
            // 다운로드 API: 40 req/min
            bandwidth = Bandwidth.classic(DOWNLOAD_CAPACITY, Refill.intervally(DOWNLOAD_REFILL, Duration.ofMinutes(1)));
        } else {
            // 일반 API: 60 req/min
            bandwidth = Bandwidth.classic(DEFAULT_CAPACITY, Refill.intervally(DEFAULT_REFILL, Duration.ofMinutes(1)));
        }

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }
}
