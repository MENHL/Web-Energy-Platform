package com.greengrid.admin.modules.material.dto;

import com.greengrid.admin.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料分页查询参数，字段与前端 MaterialList.vue 的 query 一致。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialQuery extends PageQuery {

    /** 物料分类 */
    private String category;

    /** 预警状态：正常/低库存/缺货 */
    private String status;
}
