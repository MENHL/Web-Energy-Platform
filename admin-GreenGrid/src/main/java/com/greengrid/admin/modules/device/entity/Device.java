package com.greengrid.admin.modules.device.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备表实体，对应表 device。
 */
@Data
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备编号 */
    private String deviceCode;

    /** 设备名称 */
    private String name;

    /** 类型：逆变器/风电机组/储能系统/光伏阵列/输配电 */
    private String type;

    /** 所属项目 id（关联 project.id，可能为空） */
    private Long projectId;

    /** 状态：在线/离线/故障/维护中 */
    private String status;

    /** 安装日期 */
    private LocalDate installDate;

    /** 最后上报/维护时间 */
    private LocalDateTime lastUpdateAt;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 逻辑删除标记 */
    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
