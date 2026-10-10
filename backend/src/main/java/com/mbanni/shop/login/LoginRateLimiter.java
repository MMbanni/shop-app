package com.mbanni.shop.login;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.common.exception.RateLimiterUnavailableException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class LoginRateLimiter {

    private final ProxyManager<String> proxyManager;
    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    public LoginRateLimiter(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    public void checkAllowed(String email, String ipAddress) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT).trim();

        consumeToken(
                "login:ip:" + ipAddress,
                20,
                Duration.ofMinutes(20)
        );

        consumeToken(
                "login:email:" + normalizedEmail,
                5,
                Duration.ofMinutes(15)
        );

        consumeToken(
                "login:combo:" + normalizedEmail + ":" + ipAddress,
                5,
                Duration.ofMinutes(10)
        );
    }

    private void consumeToken(String key, long capacity, Duration refillPeriod) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit -> limit
                        .capacity(capacity)
                        .refillGreedy(capacity, refillPeriod))
                .build();

        try {

            Bucket bucket = proxyManager.getProxy(key, () -> configuration);

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            if (!probe.isConsumed()) {
                throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS);
            }

        } catch (io.lettuce.core.RedisException e) {
            log.error("Redis rate limiter unavailable", e);
            throw new RateLimiterUnavailableException(e);
        }
    }
}