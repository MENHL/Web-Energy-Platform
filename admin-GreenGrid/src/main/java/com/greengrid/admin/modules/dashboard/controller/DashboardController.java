package com.greengrid.admin.modules.dashboard.controller;

import com.greengrid.admin.common.result.Result;
import com.greengrid.admin.modules.dashboard.service.DashboardService;
import com.greengrid.admin.modules.dashboard.vo.AnalyticsVO;
import com.greengrid.admin.modules.dashboard.vo.BoardSummaryVO;
import com.greengrid.admin.modules.dashboard.vo.OverviewVO;
import com.greengrid.admin.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 看板聚合接口：数据总览、运营看板、运营分析。
 * <p>三个接口路径前缀不同，故不使用类级 @RequestMapping，各方法声明完整路径。</p>
 */
@Tag(name = "看板聚合", description = "首页总览 / 运营看板 / 运营分析")
@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "首页数据总览")
    @GetMapping("/dashboard/overview")
    @RequireRole({"super", "ops", "guest"})
    public Result<OverviewVO> overview() {
        return Result.ok(dashboardService.overview());
    }

    @Operation(summary = "运营看板汇总")
    @GetMapping("/board/summary")
    @RequireRole({"super", "ops", "guest"})
    public Result<BoardSummaryVO> board() {
        return Result.ok(dashboardService.board());
    }

    @Operation(summary = "运营分析汇总（range：近 N 日）")
    @GetMapping("/analytics/summary")
    @RequireRole({"super", "ops", "warehouse"})
    public Result<AnalyticsVO> analytics(@RequestParam(defaultValue = "30") int range) {
        return Result.ok(dashboardService.analytics(range));
    }
}
