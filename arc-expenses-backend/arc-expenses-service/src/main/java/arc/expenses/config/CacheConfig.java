package arc.expenses.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.HashMap;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${redis.url}")
    private String redisUrl;

    @Value("${redis.password}")
    private String redisPassword;

    @Value("${redis.port}")
    private int redisPort;

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(redisUrl);
        jedisConnectionFactory.setPort(redisPort);
        jedisConnectionFactory.setPassword(redisPassword);
        jedisConnectionFactory.setUsePool(true);

        return jedisConnectionFactory;
    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplate() {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<Object, Object>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        return redisTemplate;
    }

    @Bean
    public CacheManager cacheManager() {

        RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate());
        HashMap<String, Long> expiresIn = new HashMap<>();
        expiresIn.put("arc", 60*5L);
        cacheManager.setExpires(expiresIn);
        cacheManager.setCacheNames(Collections.singleton("arc"));
        return cacheManager;
    }


}
