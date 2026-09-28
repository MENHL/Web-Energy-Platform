package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 总数 + 明细项，用于设备状态分布、库存结构。
 */
@Data
public class TotalItemsVO {

    private Long total;

    private List<NameValueVO> items;
}
