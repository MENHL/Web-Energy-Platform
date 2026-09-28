package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

/**
 * KPI 指标项，字段与前端 KpiItem（api/dashboard.ts）一致。
 * <p>value 用 Number 类型：整数指标序列化为整数、小数指标序列化为小数，与 Mock 数值形态一致。</p>
 */
@Data
public class KpiVO {

    private String label;

    private Number value;

    private String unit;

    private String remark;

    /** 1 上升 / 0 持平 / -1 下降 */
    private Integer trend;
}
