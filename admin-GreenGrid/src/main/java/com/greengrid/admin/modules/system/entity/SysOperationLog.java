package com.greengrid.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表实体，对应表 sys_operation_log。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 id（系统操作为 null） */
    private Long userId;

    /** 操作人姓名（或"系统"） */
    private String operator;

    /** 所属模块 */
    private String module;

    /** 动作：新增/更新/删除/维护/登录 */
    private String action;

    /** 操作内容 */
    private String content;

    /** 客户端 IP */
    private String ip;

    /** 操作时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}
