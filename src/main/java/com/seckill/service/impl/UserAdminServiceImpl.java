package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.dto.*;
import com.seckill.entity.User;
import com.seckill.mapper.UserMapper;
import com.seckill.service.UserAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserAdminServiceImpl implements UserAdminService {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserPageResultDTO listUsers(UserQueryDTO queryDTO) {
        Page<User> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(User::getIsDeleted, 0);

        if (StringUtils.hasText(queryDTO.getUsername())) {
            queryWrapper.like(User::getUsername, queryDTO.getUsername());
        }

        if (StringUtils.hasText(queryDTO.getNickname())) {
            queryWrapper.like(User::getNickname, queryDTO.getNickname());
        }

        if (queryDTO.getRoleType() != null) {
            queryWrapper.eq(User::getRoleType, queryDTO.getRoleType());
        }

        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(User::getStatus, queryDTO.getStatus());
        }

        queryWrapper.orderByDesc(User::getCreateTime);

        Page<User> userPage = userMapper.selectPage(page, queryWrapper);

        UserPageResultDTO result = new UserPageResultDTO();
        result.setList(userPage.getRecords().stream()
                .map(this::buildUserInfoDTO)
                .collect(Collectors.toList()));
        result.setTotal(userPage.getTotal());
        result.setPageNum((int) userPage.getCurrent());
        result.setPageSize((int) userPage.getSize());
        result.setTotalPages((int) userPage.getPages());

        return result;
    }

    @Override
    public UserInfoDTO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }
        return buildUserInfoDTO(user);
    }

    @Override
    @Transactional
    public UserInfoDTO createUser(UserCreateDTO dto) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, dto.getUsername());
        queryWrapper.eq(User::getIsDeleted, 0);

        if (userMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRoleType(dto.getRoleType() != null ? dto.getRoleType() : 0);
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
        log.info("管理员创建用户成功，userId={}, username={}", user.getId(), user.getUsername());

        return buildUserInfoDTO(user);
    }

    @Override
    @Transactional
    public UserInfoDTO updateUser(Long id, UserUpdateDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }

        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }

        if (StringUtils.hasText(dto.getEmail())) {
            user.setEmail(dto.getEmail());
        }

        if (StringUtils.hasText(dto.getPhone())) {
            user.setPhone(dto.getPhone());
        }

        if (dto.getRoleType() != null) {
            user.setRoleType(dto.getRoleType());
        }

        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("管理员更新用户成功，userId={}", id);
        return buildUserInfoDTO(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getIsDeleted() == 1) {
            throw new RuntimeException("用户不存在");
        }

        if ("admin".equals(user.getUsername())) {
            throw new RuntimeException("不能删除系统管理员账户");
        }

        user.setIsDeleted(1);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("管理员逻辑删除用户成功，userId={}", id);
    }

    @Override
    @Transactional
    public void restoreUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (user.getIsDeleted() == 0) {
            throw new RuntimeException("用户未删除，无需恢复");
        }

        user.setIsDeleted(0);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("管理员恢复用户成功，userId={}", id);
    }

    private UserInfoDTO buildUserInfoDTO(User user) {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRoleType(user.getRoleType());
        return dto;
    }
}
