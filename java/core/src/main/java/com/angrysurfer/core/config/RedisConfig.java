// package com.angrysurfer.core.config;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.connection.RedisConnectionFactory;
// import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

// @Configuration
// @EnableRedisRepositories(basePackages = "com.angrysurfer.core.repo")
// public class RedisConfig {

//     @Bean
//     public RedisConnectionFactory connectionFactory() {
//         return new JedisConnectionFactory();
//     }

//     @Bean
//     public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
//         RedisTemplate<?, ?> template = new RedisTemplate<>();
//         template.setConnectionFactory(redisConnectionFactory);
//         return template;
//     }
// }
