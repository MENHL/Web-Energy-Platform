package com.greengrid.admin.modules.material.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 物料详情出参：物料信息 + 入库记录 + 出库记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialDetailVO extends MaterialVO {

    private List<OrderItemVO> inboundRecords;

    private List<OrderItemVO> outboundRecords;
}
