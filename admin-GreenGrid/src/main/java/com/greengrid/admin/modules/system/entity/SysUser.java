package com.greengrid.admin.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表实体，对应表 sys_user。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** BCrypt 密码哈希，永不出参 */
    @JsonIgnore
    private String password;

    /** 姓名 */
    private String name;

    /** 角色编码：super/ops/warehouse/project/guest */
    private String role;

    /** 角色名称 */
    private String roleName;

    /** 所属部门 */
    private String dept;

    /** 1 启用 / 0 禁用 */
    private Integer status;

    /** 最近登录时间 */
    private LocalDateTime lastLoginAt;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记：0 未删 / 1 已删 */
    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
