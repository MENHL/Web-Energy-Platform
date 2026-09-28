package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 近 24 小时产能与吞吐趋势，字段与 Mock getHourlySeries 结构一致。
 */
@Data
public class HourlyVO {

    /** 小时序列，格式 HH:00 */
    private List<String> hours;

    /** 发电功率（小数） */
    private List<Double> power;

    /** 出入库单量（整数） */
    private List<Integer> orders;
}
