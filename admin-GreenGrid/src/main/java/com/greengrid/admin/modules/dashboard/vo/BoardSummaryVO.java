package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 运营看板出参，字段与 Mock boardSummary 结构完全一致。
 */
@Data
public class BoardSummaryVO {

    private List<KpiVO> kpis;

    private HourlyVO hourly;

    private List<NameUsageVO> warehouses;

    private List<NameUsageVO> deviceTypes;

    private List<TodoVO> todos;
}
