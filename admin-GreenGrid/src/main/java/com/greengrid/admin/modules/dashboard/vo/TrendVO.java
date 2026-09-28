package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 入出库趋势，字段与 Mock getTrendSeries 结构一致：{ dates, inbound, outbound }。
 */
@Data
public class TrendVO {

    /** 日期序列，格式 MM-DD */
    private List<String> dates;

    private List<Integer> inbound;

    private List<Integer> outbound;
}
