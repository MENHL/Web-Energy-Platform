package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 首页数据总览出参，字段与 Mock dashboardOverview 结构完全一致。
 */
@Data
public class OverviewVO {

    private List<KpiVO> kpis;

    private TrendVO trend;

    private TotalItemsVO deviceStatus;

    private List<RankVO> topProjects;

    private List<WarningVO> warnings;

    private List<ActivityVO> activities;
}
