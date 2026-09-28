package com.greengrid.admin.modules.report.controller;

import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.common.result.Result;
import com.greengrid.admin.common.util.IpUtil;
import com.greengrid.admin.modules.report.dto.ReportQuery;
import com.greengrid.admin.modules.report.dto.ReportSaveRequest;
import com.greengrid.admin.modules.report.dto.ReportStatusRequest;
import com.greengrid.admin.modules.report.service.ReportService;
import com.greengrid.admin.modules.report.vo.ReportVO;
import com.greengrid.admin.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据报表接口。权限：super / ops（与前端路由 meta.roles 一致）。
 */
@Tag(name = "数据报表", description = "报表查询、生成、取消、删除")
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@RequireRole({"super", "ops"})
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "分页查询报表列表")
    @GetMapping
    public Result<PageResult<ReportVO>> list(ReportQuery query) {
        return Result.ok(reportService.page(query));
    }

    @Operation(summary = "生成报表")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ReportSaveRequest request, HttpServletRequest httpRequest) {
        reportService.create(request, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }

    @Operation(summary = "更新报表状态（取消生成中的报表）")
    @PutMapping("/{id}")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody ReportStatusRequest request,
                                     HttpServletRequest httpRequest) {
        reportService.updateStatus(id, request, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }

    @Operation(summary = "删除报表")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        reportService.delete(id, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }
}
