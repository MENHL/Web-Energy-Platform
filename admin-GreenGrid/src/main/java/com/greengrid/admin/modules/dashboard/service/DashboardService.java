package com.greengrid.admin.modules.dashboard.service;

import com.greengrid.admin.modules.dashboard.mapper.DashboardMapper;
import com.greengrid.admin.modules.dashboard.vo.AnalyticsVO;
import com.greengrid.admin.modules.dashboard.vo.BoardSummaryVO;
import com.greengrid.admin.modules.dashboard.vo.HourlyVO;
import com.greengrid.admin.modules.dashboard.vo.KpiVO;
import com.greengrid.admin.modules.dashboard.vo.NameUsageVO;
import com.greengrid.admin.modules.dashboard.vo.NameValueVO;
import com.greengrid.admin.modules.dashboard.vo.OverviewVO;
import com.greengrid.admin.modules.dashboard.vo.RankVO;
import com.greengrid.admin.modules.dashboard.vo.TodoVO;
import com.greengrid.admin.modules.dashboard.vo.TotalItemsVO;
import com.greengrid.admin.modules.dashboard.vo.TrendPointVO;
import com.greengrid.admin.modules.dashboard.vo.TrendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 看板聚合服务：数据总览、运营看板、运营分析。
 * <p>能真实聚合的走 MySQL，无数据源的（今日发电量、设备利用率、仓库库容、项目运营单量等）用固定初始化值兜底，
 * 并在注释中标注，后续接入对应数据源后替换。</p>
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final DashboardMapper dashboardMapper;

    // ==================== 数据总览 ====================

    public OverviewVO overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        OverviewVO vo = new OverviewVO();
        vo.setKpis(buildOverviewKpis(todayStart, todayEnd));
        vo.setTrend(buildTrend(7));
        vo.setDeviceStatus(buildDeviceStatus());
        vo.setTopProjects(buildTopProjects());
        vo.setWarnings(dashboardMapper.selectWarnings());
        vo.setActivities(dashboardMapper.selectRecentActivities());
        return vo;
    }

    private List<KpiVO> buildOverviewKpis(LocalDateTime todayStart, LocalDateTime todayEnd) {
        List<KpiVO> kpis = new ArrayList<>();
        kpis.add(kpi("在建项目", dashboardMapper.countOngoingProjects(), "个", "实时统计", 0));
        kpis.add(kpi("运行设备", dashboardMapper.countOnlineDevices(), "台", "实时在线", 1));
        kpis.add(kpi("物料品类", dashboardMapper.countMaterials(), "种", "实时统计", 0));
        kpis.add(kpi("今日入库", dashboardMapper.countInboundBetween(todayStart, todayEnd), "单", "今日单量", 0));
        kpis.add(kpi("今日出库", dashboardMapper.countOutboundBetween(todayStart, todayEnd), "单", "今日单量", 0));
        kpis.add(kpi("库存预警", dashboardMapper.countWarningMaterials(), "项", "低库存 / 缺货", -1));
        return kpis;
    }

    // ==================== 运营看板 ====================

    public BoardSummaryVO board() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        BoardSummaryVO vo = new BoardSummaryVO();
        vo.setKpis(buildBoardKpis(todayStart, todayEnd));
        vo.setHourly(buildHourly());
        vo.setWarehouses(buildWarehouses());
        vo.setDeviceTypes(buildDeviceTypes());
        vo.setTodos(buildTodos());
        return vo;
    }

    private List<KpiVO> buildBoardKpis(LocalDateTime todayStart, LocalDateTime todayEnd) {
        long todayThroughput = dashboardMapper.countInboundBetween(todayStart, todayEnd)
                + dashboardMapper.countOutboundBetween(todayStart, todayEnd);
        long activeAlerts = dashboardMapper.countWarningMaterials() + dashboardMapper.countAlertDevices();

        List<KpiVO> kpis = new ArrayList<>();
        // 无数据源，固定兜底
        kpis.add(kpi("今日发电量", 128.6, "万kWh", "环比 +4.2%", 1));
        kpis.add(kpi("设备综合利用率", 91.8, "%", "环比 +1.6%", 1));
        // 真实聚合
        kpis.add(kpi("今日出入库吞吐", todayThroughput, "单", "今日单量", 0));
        kpis.add(kpi("活跃预警", activeAlerts, "项", "设备 · 库存", -1));
        return kpis;
    }

    // ==================== 运营分析 ====================

    public AnalyticsVO analytics(int range) {
        int days = Math.max(range, 1);
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime rangeStart = today.minusDays(days - 1L).atStartOfDay();
        LocalDateTime prevStart = today.minusDays(days * 2L - 1L).atStartOfDay();

        // 环比（按单量）
        long nearInbound = dashboardMapper.countInboundBetween(rangeStart, todayStart.plusDays(1));
        long prevInbound = dashboardMapper.countInboundBetween(prevStart, rangeStart);
        long nearOutbound = dashboardMapper.countOutboundBetween(rangeStart, todayStart.plusDays(1));
        long prevOutbound = dashboardMapper.countOutboundBetween(prevStart, rangeStart);

        // 完整趋势（inbound=入库、outbound=出库）
        TrendVO full = buildTrend(days);

        // 前端 renderTrendChart 统一读取 .inbound 字段：
        // 入库图读 inboundTrend.inbound，出库图读 outboundTrend.inbound。
        // 因此把出库序列放进 outboundTrend.inbound，让出库图正确显示出库量。
        TrendVO outboundTrend = new TrendVO();
        outboundTrend.setDates(full.getDates());
        outboundTrend.setInbound(full.getOutbound());
        outboundTrend.setOutbound(full.getInbound());

        // 库存结构
        List<NameValueVO> stockItems = dashboardMapper.selectStockStructure();
        long stockTotal = stockItems.stream().mapToLong(NameValueVO::getValue).sum();

        // 物料排行（补 rank）
        List<RankVO> materialRank = dashboardMapper.selectMaterialRank();
        int rank = 1;
        for (RankVO item : materialRank) {
            item.setRank(rank++);
        }

        AnalyticsVO vo = new AnalyticsVO();
        vo.setInboundRate(calcRate(nearInbound, prevInbound));
        vo.setOutboundRate(calcRate(nearOutbound, prevOutbound));
        vo.setInboundTrend(full);
        vo.setOutboundTrend(outboundTrend);

        TotalItemsVO stockStructure = new TotalItemsVO();
        stockStructure.setTotal(stockTotal);
        stockStructure.setItems(stockItems);
        vo.setStockStructure(stockStructure);
        vo.setMaterialRank(materialRank);
        return vo;
    }

    // ==================== 通用构建 ====================

    private TrendVO buildTrend(int days) {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);
        List<TrendPointVO> points = dashboardMapper.selectTrend(start.atStartOfDay());
        Map<String, TrendPointVO> byDay = points.stream()
                .collect(Collectors.toMap(TrendPointVO::getDay, p -> p, (a, b) -> a));

        TrendVO vo = new TrendVO();
        List<String> dates = new ArrayList<>();
        List<Integer> inbound = new ArrayList<>();
        List<Integer> outbound = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String day = start.plusDays(i).format(DAY_FORMATTER);
            dates.add(day);
            TrendPointVO point = byDay.get(day);
            inbound.add(point == null ? 0 : point.getInbound().intValue());
            outbound.add(point == null ? 0 : point.getOutbound().intValue());
        }
        vo.setDates(dates);
        vo.setInbound(inbound);
        vo.setOutbound(outbound);
        return vo;
    }

    private TotalItemsVO buildDeviceStatus() {
        List<NameValueVO> list = dashboardMapper.selectDeviceStatus();
        Map<String, Long> byName = list.stream()
                .collect(Collectors.toMap(NameValueVO::getName, NameValueVO::getValue, (a, b) -> a));

        String[] order = {"在线运行", "离线", "故障", "维护中"};
        List<NameValueVO> items = new ArrayList<>();
        long total = 0;
        for (String name : order) {
            long value = byName.getOrDefault(name, 0L);
            total += value;
            NameValueVO item = new NameValueVO();
            item.setName(name);
            item.setValue(value);
            items.add(item);
        }

        TotalItemsVO vo = new TotalItemsVO();
        vo.setTotal(total);
        vo.setItems(items);
        return vo;
    }

    /**
     * 近 24 小时产能与吞吐：按 Mock 的确定性算法生成（订单表无 24 小时密集数据，故生成保证图表有数据）。
     */
    private HourlyVO buildHourly() {
        List<String> hours = new ArrayList<>();
        List<Double> power = new ArrayList<>();
        List<Integer> orders = new ArrayList<>();
        double seed = 914;
        for (int h = 0; h < 24; h++) {
            hours.add(String.format("%02d:00", h));
            double curve = Math.sin(((h - 6) / 24.0) * Math.PI);
            seed = (seed * 9301 + 49297) % 233280;
            double r1 = seed / 233280.0;
            power.add(Math.max(0.4, Math.round((curve * 8 + r1 * 1.2) * 10) / 10.0));
            seed = (seed * 9301 + 49297) % 233280;
            double r2 = seed / 233280.0;
            orders.add(h > 20 || h < 6 ? (int) Math.round(r2 * 2) : (int) Math.round(2 + r2 * 6));
        }
        HourlyVO vo = new HourlyVO();
        vo.setHours(hours);
        vo.setPower(power);
        vo.setOrders(orders);
        return vo;
    }

    // ==================== 兜底固定数据（无数据源，后续接入替换） ====================

    /** 项目运营排行 Top5：项目-单据关联暂无数据源，暂用初始化值。 */
    private List<RankVO> buildTopProjects() {
        return List.of(
                rank(1, "华东储能基地一期", 1286),
                rank(2, "西北光伏组件仓", 1102),
                rank(3, "粤东风电运维", 954),
                rank(4, "华中新能源产业园", 871),
                rank(5, "华北氢能示范站", 756));
    }

    /** 仓库库容：暂无仓库表，暂用初始化值。 */
    private List<NameUsageVO> buildWarehouses() {
        return List.of(
                usage("华东一号仓", 82),
                usage("西北光伏组件仓", 64),
                usage("华南储能系统仓", 45));
    }

    /** 设备类型利用率：暂无利用率采集数据，暂用初始化值。 */
    private List<NameUsageVO> buildDeviceTypes() {
        return List.of(
                usage("风电机组", 88),
                usage("光伏阵列", 92),
                usage("储能系统", 76),
                usage("输配电", 84));
    }

    /** 今日待办：暂无待办数据源，暂用初始化值。 */
    private List<TodoVO> buildTodos() {
        return List.of(
                todo(1, "处理设备 DEV-0231 故障工单", "紧急"),
                todo(2, "审批入库单 RK202609140048", "今日"),
                todo(3, "补货审批：单晶硅光伏组件 550W", "今日"),
                todo(4, "复核 8 月运营汇总报表", "本周"));
    }

    // ==================== 工具 ====================

    private KpiVO kpi(String label, long value, String unit, String remark, int trend) {
        KpiVO vo = new KpiVO();
        vo.setLabel(label);
        vo.setValue(value);
        vo.setUnit(unit);
        vo.setRemark(remark);
        vo.setTrend(trend);
        return vo;
    }

    private KpiVO kpi(String label, double value, String unit, String remark, int trend) {
        KpiVO vo = new KpiVO();
        vo.setLabel(label);
        vo.setValue(value);
        vo.setUnit(unit);
        vo.setRemark(remark);
        vo.setTrend(trend);
        return vo;
    }

    private RankVO rank(int rank, String name, long amount) {
        RankVO vo = new RankVO();
        vo.setRank(rank);
        vo.setName(name);
        vo.setAmount(amount);
        return vo;
    }

    private NameUsageVO usage(String name, int usage) {
        NameUsageVO vo = new NameUsageVO();
        vo.setName(name);
        vo.setUsage(usage);
        return vo;
    }

    private TodoVO todo(long id, String content, String level) {
        TodoVO vo = new TodoVO();
        vo.setId(id);
        vo.setContent(content);
        vo.setLevel(level);
        return vo;
    }

    private String calcRate(long near, long prev) {
        if (prev == 0) {
            return "0.0%";
        }
        double rate = (near - prev) * 100.0 / prev;
        return rate > 0
                ? "+" + String.format("%.1f", rate) + "%"
                : String.format("%.1f", rate) + "%";
    }
}
