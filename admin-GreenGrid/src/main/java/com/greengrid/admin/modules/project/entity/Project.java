package com.greengrid.admin.modules.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
 * 项目表实体，对应表 project。
 */
@Data
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目编号 */
    private String projectCode;

    /** 项目名称 */
    private String name;

    /** 区域：华东/华北/华南/华中/西北/西南/东北 */
    private String region;

    /** 负责人 */
    private String manager;

    /** 状态：进行中/已暂停/已完成 */
    private String status;

    /** 进度 0-100 */
    private Integer progress;

    /** 计划开始日期（updateStrategy=ALWAYS：编辑时清空日期也能把库里的值置空） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate startDate;

    /** 计划结束日期（同上，清空日期也能生效） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDate endDate;

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
