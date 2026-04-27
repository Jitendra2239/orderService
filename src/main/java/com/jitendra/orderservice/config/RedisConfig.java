package com.jitendra.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RedisConfig {

//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//
//        // Key serializer
//        template.setKeySerializer(new StringRedisSerializer());
//
//        // Value serializer (JSON)
//        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//
//        return template;
//    }


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}