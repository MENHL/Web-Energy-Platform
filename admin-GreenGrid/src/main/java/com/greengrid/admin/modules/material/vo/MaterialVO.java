package com.greengrid.admin.modules.material.vo;

import lombok.Data;

/**
 * 物料出参，字段与前端 MaterialItem 类型（src/types/index.ts）一一对应。
 */
@Data
public class MaterialVO {

    private Long id;

    private String materialCode;

    private String name;

    private String category;

    private String spec;

    private String unit;

    private Integer stock;

    private Integer safetyStock;

    /** 正常/低库存/缺货 */
    private String status;
}
