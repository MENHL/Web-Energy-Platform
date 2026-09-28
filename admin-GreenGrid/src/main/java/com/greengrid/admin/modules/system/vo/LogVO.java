package com.greengrid.admin.modules.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志出参，字段与前端 LogItem 类型（src/types/index.ts）一一对应。
 */
@Data
public class LogVO {

    private Long id;

    private String operator;

    private String module;

    /** 新增/更新/删除/维护/登录 */
    private String action;

    private String content;

    private String ip;

    private LocalDateTime createdAt;
}
