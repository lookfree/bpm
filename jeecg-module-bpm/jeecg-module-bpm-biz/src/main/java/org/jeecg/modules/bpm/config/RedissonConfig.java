package org.jeecg.modules.bpm.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "bpm.lock.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${bpm.redis.address:redis://127.0.0.1:6379}") String address,
            @Value("${bpm.redis.password:}") String password) {
        Config cfg = new Config();
        SingleServerConfig single = cfg.useSingleServer().setAddress(address);
        if (password != null && !password.isEmpty()) single.setPassword(password);
        return Redisson.create(cfg);
    }
}
