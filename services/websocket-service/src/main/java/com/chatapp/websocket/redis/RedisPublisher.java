package com.chatapp.websocket.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String channel, Object message) {
        try {
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize message for channel {}: {}", channel, e.getMessage());
        }
    }
}
