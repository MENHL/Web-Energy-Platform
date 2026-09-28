package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 运营分析出参，字段与 Mock buildAnalytics 结构完全一致。
 */
@Data
public class AnalyticsVO {

    private String inboundRate;

    private String outboundRate;

    private TrendVO inboundTrend;

    private TrendVO outboundTrend;

    private TotalItemsVO stockStructure;

    private List<RankVO> materialRank;
}
