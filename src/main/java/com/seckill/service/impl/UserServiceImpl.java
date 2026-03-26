package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.dto.UserInfoDTO;
import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.service.UserService;
import com.seckill.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String USER_CACHE_KEY = "user:info:";
    private static final String USER_EXISTS_KEY = "user:exists:";
    private static final long CACHE_EXPIRE_TIME = 30;
    private static final TimeUnit CACHE_EXPIRE_UNIT = TimeUnit.MINUTES;

    @Override
    public UserInfoDTO register(UserRegisterDTO dto) {
        log.info("开始注册用户：username={}", dto.getUsername());
        
        if (checkUsernameExists(dto.getUsername())) {
            log.warn("用户名已存在：username={}", dto.getUsername());
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        user.setPassword(encodedPassword);
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRoleType(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setIsDeleted(0);

        log.info("准备插入用户数据到数据库");
        userMapper.insert(user);
        log.info("用户插入成功，userId={}", user.getId());

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleType());
        log.info("用户注册成功，userId={}, token={}", user.getId(), token.substring(0, Math.min(20, token.length())) + "...");

        UserInfoDTO result = buildUserInfoDTO(user, token);
        try {
            String cacheKey = USER_CACHE_KEY + user.getId();
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_TIME, CACHE_EXPIRE_UNIT);
            log.debug("用户注册成功，写入用户信息缓存，userId={}", user.getId());
        } catch (Exception e) {
            log.warn("Redis写入失败，userId={}, error={}", user.getId(), e.getMessage());
        }

        return result;
    }

    @Override
    public UserInfoDTO login(UserLoginDTO dto) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, dto.getUsername());
        queryWrapper.eq(User::getIsDeleted, 0);

        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleType());

        UserInfoDTO result = buildUserInfoDTO(user, token);
        try {
            String cacheKey = USER_CACHE_KEY + user.getId();
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_TIME, CACHE_EXPIRE_UNIT);
            log.debug("用户登录成功，更新用户信息缓存，userId={}", user.getId());
        } catch (Exception e) {
            log.warn("Redis写入失败，userId={}, error={}", user.getId(), e.getMessage());
        }

        return result;
    }

    @Override
    public UserInfoDTO getUserInfo(Long userId) {
        String cacheKey = USER_CACHE_KEY + userId;
        
        UserInfoDTO cachedUser = null;
        try {
            cachedUser = (UserInfoDTO) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis读取失败，降级到数据库查询，userId={}, error={}", userId, e.getMessage());
        }
        
        if (cachedUser != null) {
            log.debug("缓存命中，从Redis获取用户信息，userId={}", userId);
            return cachedUser;
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return null;
        }

        UserInfoDTO result = buildUserInfoDTO(user, null);
        try {
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_TIME, CACHE_EXPIRE_UNIT);
            log.debug("缓存未命中，从数据库获取用户信息并写入缓存，userId={}", userId);
        } catch (Exception e) {
            log.warn("Redis写入失败，userId={}, error={}", userId, e.getMessage());
        }
        
        return result;
    }

    @Override
    public boolean checkUsernameExists(String username) {
        String cacheKey = USER_EXISTS_KEY + username;
        
        Boolean cachedExists = null;
        try {
            cachedExists = (Boolean) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis读取失败，降级到数据库查询，username={}, error={}", username, e.getMessage());
        }
        
        if (cachedExists != null) {
            log.debug("缓存命中，从Redis获取用户名存在状态，username={}", username);
            return cachedExists;
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        queryWrapper.eq(User::getIsDeleted, 0);
        boolean exists = userMapper.selectCount(queryWrapper) > 0;

        try {
            redisTemplate.opsForValue().set(cacheKey, exists, CACHE_EXPIRE_TIME, CACHE_EXPIRE_UNIT);
            log.debug("缓存未命中，从数据库获取用户名存在状态并写入缓存，username={}, exists={}", username, exists);
        } catch (Exception e) {
            log.warn("Redis写入失败，username={}, error={}", username, e.getMessage());
        }
        
        return exists;
    }

    private UserInfoDTO buildUserInfoDTO(User user, String token) {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRoleType(user.getRoleType());
        dto.setToken(token);
        return dto;
    }
}
