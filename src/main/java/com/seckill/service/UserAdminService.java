package com.seckill.service;

import com.seckill.dto.*;

public interface UserAdminService {

    UserPageResultDTO listUsers(UserQueryDTO queryDTO);

    UserInfoDTO getUserById(Long id);

    UserInfoDTO createUser(UserCreateDTO dto);

    UserInfoDTO updateUser(Long id, UserUpdateDTO dto);

    void deleteUser(Long id);

    void restoreUser(Long id);
}
