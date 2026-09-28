package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

/**
 * 库存预警项，字段与 Mock warnings 一致。
 */
@Data
public class WarningVO {

    private Long id;

    private String name;

    private String spec;

    private Integer stock;

    private Integer safetyStock;

    /** 预警级别：缺货 / 低库存 */
    private String level;
}
