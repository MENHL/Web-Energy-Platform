package com.greengrid.admin.modules.material.entity;

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
 * 物料表实体，对应表 material。
 */
@Data
@TableName("material")
public class Material {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String name;

    /** 分类：电芯电池/光伏组件/电气元件/结构件辅材 */
    private String category;

    /** 规格型号 */
    private String spec;

    /** 计量单位 */
    private String unit;

    /** 当前库存 */
    private Integer stock;

    /** 安全库存 */
    private Integer safetyStock;

    /** 状态：正常/低库存/缺货（由服务层重算） */
    private String status;

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
