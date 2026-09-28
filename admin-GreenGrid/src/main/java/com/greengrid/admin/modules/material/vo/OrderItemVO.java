package com.greengrid.admin.modules.material.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出入库记录出参，字段与前端 OrderItem 类型（src/types/index.ts）一一对应。
 */
@Data
public class OrderItemVO {

    private Long id;

    private String orderNo;

    private String materialName;

    private Integer quantity;

    /** 入库=仓库名 / 出库=去向 */
    private String target;

    private String operator;

    /** 单据时间 */
    private LocalDateTime time;
}
