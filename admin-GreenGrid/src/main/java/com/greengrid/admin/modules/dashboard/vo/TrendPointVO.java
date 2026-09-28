package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

/**
 * 趋势聚合中间结果（按天分组的入出库单量），仅在聚合查询内部使用，不直接出参。
 */
@Data
public class TrendPointVO {

    /** 日期，格式 MM-DD */
    private String day;

    private Long inbound;

    private Long outbound;
}
