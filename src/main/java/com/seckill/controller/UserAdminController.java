package com.seckill.controller;

import com.seckill.dto.*;
import com.seckill.service.UserAdminService;
import com.seckill.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    @Autowired
    private UserAdminService userAdminService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<Result<UserPageResultDTO>> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer roleType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.ok(Result.fail("无权限访问"));
        }

        try {
            UserQueryDTO queryDTO = new UserQueryDTO();
            queryDTO.setUsername(username);
            queryDTO.setNickname(nickname);
            queryDTO.setRoleType(roleType);
            queryDTO.setStatus(status);
            queryDTO.setPageNum(pageNum);
            queryDTO.setPageSize(pageSize);

            UserPageResultDTO result = userAdminService.listUsers(queryDTO);
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            log.error("查询用户列表失败", e);
            return ResponseEntity.ok(Result.fail(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<UserInfoDTO>> getUserById(
            @PathVariable Long id,
            HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.ok(Result.fail("无权限访问"));
        }

        try {
            UserInfoDTO result = userAdminService.getUserById(id);
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            log.error("查询用户详情失败", e);
            return ResponseEntity.ok(Result.fail(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<Result<UserInfoDTO>> createUser(
            @Valid @RequestBody UserCreateDTO dto,
            HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.ok(Result.fail("无权限访问"));
        }

        try {
            UserInfoDTO result = userAdminService.createUser(dto);
            return ResponseEntity.ok(Result.success("创建用户成功", result));
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return ResponseEntity.ok(Result.fail(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<UserInfoDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDTO dto,
            HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.ok(Result.fail("无权限访问"));
        }

        try {
            UserInfoDTO result = userAdminService.updateUser(id, dto);
            return ResponseEntity.ok(Result.success("更新用户成功", result));
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return ResponseEntity.ok(Result.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.ok(Result.fail("无权限访问"));
        }

        try {
            userAdminService.deleteUser(id);
            return ResponseEntity.ok(Result.success("删除用户成功", null));
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return ResponseEntity.ok(Result.fail(e.getMessage()));
        }
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Result<Void>> restoreUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        if (!isAdmin(request)) {
            return ResponseEntity.ok(Result.fail("无权限访问"));
        }

        try {
            userAdminService.restoreUser(id);
            return ResponseEntity.ok(Result.success("恢复用户成功", null));
        } catch (Exception e) {
            log.error("恢复用户失败", e);
            return ResponseEntity.ok(Result.fail(e.getMessage()));
        }
    }

    private boolean isAdmin(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || !jwtUtil.validateToken(token)) {
            return false;
        }

        Integer roleType = jwtUtil.getRoleType(token);
        return roleType != null && roleType == 1;
    }
}
