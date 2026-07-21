package com.mbanni.shop.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Bucket4jRedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient redisClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port
    ) {
        return RedisClient.create("redis://" + host + ":" + port);
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(
            RedisClient redisClient
    ) {
        return redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
    }

    @Bean
    public ProxyManager<String> bucket4jProxyManager(
            StatefulRedisConnection<String, byte[]> connection
    ) {
        return Bucket4jLettuce
                .casBasedBuilder(connection)
                .expirationAfterWrite(
                        ExpirationAfterWriteStrategy
                                .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10))
                )
                .requestTimeout(Duration.ofSeconds(2))
                .maxRetries(5)
                .build();
    }
}