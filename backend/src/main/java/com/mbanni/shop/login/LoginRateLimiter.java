package com.mbanni.shop.login;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class LoginRateLimiter {

    private final ProxyManager<String> proxyManager;

    public LoginRateLimiter(ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
    }

    public void checkAllowed(String email, String ipAddress) {
        String normalizedEmail = email.toLowerCase().trim();

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

        Bucket bucket = proxyManager.getProxy(key, () -> configuration);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        long secondsToWait = TimeUnit.NANOSECONDS.toSeconds(
                probe.getNanosToWaitForRefill()
        );

        System.out.println("=================================");
        System.out.println("Bucket key: " + key);
        System.out.println("Allowed: " + probe.isConsumed());
        System.out.println("Remaining tokens: " + probe.getRemainingTokens());
        System.out.println("Seconds to wait: " + secondsToWait);
        System.out.println("Capacity: " + capacity);
        System.out.println("Refill period: " + refillPeriod);
        System.out.println("=================================");

        if (!probe.isConsumed()) {
            throw new BusinessException(ErrorCode.TOO_MANY_ATTEMPTS);
        }
    }
}