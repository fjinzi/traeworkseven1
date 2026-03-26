package com.seckill.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserPageResultDTO {

    private List<UserInfoDTO> list;

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private Integer totalPages;
}
