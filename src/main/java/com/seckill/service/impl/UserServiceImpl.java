package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.dto.UserInfoDTO;
import com.seckill.dto.UserLoginDTO;
import com.seckill.dto.UserRegisterDTO;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.service.UserCacheService;
import com.seckill.service.UserService;
import com.seckill.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserCacheService userCacheService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
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

        userCacheService.cacheUsernameExists(dto.getUsername(), true);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleType());
        log.info("用户注册成功，userId={}, token={}", user.getId(), token.substring(0, Math.min(20, token.length())) + "...");

        UserInfoDTO userInfo = buildUserInfoDTO(user, token);
        userCacheService.cacheUserInfo(user.getId(), userInfo);
        
        return userInfo;
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

        UserInfoDTO userInfo = buildUserInfoDTO(user, token);
        userCacheService.cacheUserInfo(user.getId(), userInfo);
        userCacheService.cacheUserEntity(user.getId(), user);
        
        return userInfo;
    }

    @Override
    public UserInfoDTO getUserInfo(Long userId) {
        UserInfoDTO cachedInfo = userCacheService.getUserInfoFromCache(userId);
        if (cachedInfo != null) {
            log.debug("从缓存获取用户信息: userId={}", userId);
            return cachedInfo;
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getIsDeleted() == 1) {
            return null;
        }
        
        UserInfoDTO userInfo = buildUserInfoDTO(user, null);
        userCacheService.cacheUserInfo(userId, userInfo);
        
        return userInfo;
    }

    @Override
    public boolean checkUsernameExists(String username) {
        Boolean cachedExists = userCacheService.getUsernameExistsFromCache(username);
        if (cachedExists != null) {
            log.debug("从缓存获取用户名存在状态: username={}", username);
            return cachedExists;
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        queryWrapper.eq(User::getIsDeleted, 0);
        boolean exists = userMapper.selectCount(queryWrapper) > 0;
        
        userCacheService.cacheUsernameExists(username, exists);
        
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
