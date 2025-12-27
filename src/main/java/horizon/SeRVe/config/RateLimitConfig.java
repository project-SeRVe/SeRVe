package horizon.SeRVe.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting 설정
 *
 * Caffeine Cache를 사용하여 사용자별 Token Bucket을 메모리에 저장합니다.
 *
 * 캐시 설정:
 * - 최대 10,000개 Bucket 저장 (동시 사용자 수)
 * - 5분간 미사용 시 자동 제거 (메모리 효율)
 */
@Configuration
public class RateLimitConfig {

    /**
     * Bucket 캐시 생성
     *
     * 각 사용자/IP별로 Token Bucket을 생성하고 캐싱합니다.
     * Caffeine은 고성능 인메모리 캐시로, 동시성 처리에 최적화되어 있습니다.
     */
    @Bean
    public Cache<String, Bucket> bucketCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)  // 최대 10,000개 Bucket
                .expireAfterAccess(Duration.ofMinutes(5))  // 5분간 미사용 시 제거
                .build();
    }
}
