package arc.expenses.config.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@EnableCaching
@PropertySource("classpath:application.properties")
public class CachingConfiguration {

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
   public CacheManager cacheManager(){
      RedisCacheManager redisCacheManager = new RedisCacheManager(redisTemplate());
      redisCacheManager.setDefaultExpiration(60 * 60 * 24); //every 24 hours
      return new RedisCacheManager(redisTemplate());
   }

}
