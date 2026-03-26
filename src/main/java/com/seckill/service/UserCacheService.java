package com.seckill.service;

import com.seckill.dto.UserInfoDTO;
import com.seckill.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserCacheService {

    private static final String USER_INFO_KEY_PREFIX = "user:info:";
    private static final String USER_ENTITY_KEY_PREFIX = "user:entity:";
    private static final String USERNAME_EXISTS_KEY_PREFIX = "user:exists:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void cacheUserInfo(Long userId, UserInfoDTO userInfo) {
        if (userId == null || userInfo == null) {
            return;
        }
        String key = USER_INFO_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, userInfo, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("缓存用户信息: userId={}", userId);
    }

    public UserInfoDTO getUserInfoFromCache(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = USER_INFO_KEY_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof UserInfoDTO) {
            log.debug("从缓存获取用户信息: userId={}", userId);
            return (UserInfoDTO) value;
        }
        return null;
    }

    public void cacheUserEntity(Long userId, User user) {
        if (userId == null || user == null) {
            return;
        }
        String key = USER_ENTITY_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, user, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("缓存用户实体: userId={}", userId);
    }

    public User getUserEntityFromCache(Long userId) {
        if (userId == null) {
            return null;
        }
        String key = USER_ENTITY_KEY_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof User) {
            log.debug("从缓存获取用户实体: userId={}", userId);
            return (User) value;
        }
        return null;
    }

    public void evictUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        String infoKey = USER_INFO_KEY_PREFIX + userId;
        String entityKey = USER_ENTITY_KEY_PREFIX + userId;
        redisTemplate.delete(infoKey);
        redisTemplate.delete(entityKey);
        log.debug("清除用户缓存: userId={}", userId);
    }

    public void cacheUsernameExists(String username, boolean exists) {
        if (username == null || username.isEmpty()) {
            return;
        }
        String key = USERNAME_EXISTS_KEY_PREFIX + username;
        redisTemplate.opsForValue().set(key, exists, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        log.debug("缓存用户名存在状态: username={}, exists={}", username, exists);
    }

    public Boolean getUsernameExistsFromCache(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        String key = USERNAME_EXISTS_KEY_PREFIX + username;
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Boolean) {
            log.debug("从缓存获取用户名存在状态: username={}", username);
            return (Boolean) value;
        }
        return null;
    }

    public void evictUsernameExistsCache(String username) {
        if (username == null || username.isEmpty()) {
            return;
        }
        String key = USERNAME_EXISTS_KEY_PREFIX + username;
        redisTemplate.delete(key);
        log.debug("清除用户名存在状态缓存: username={}", username);
    }
}
