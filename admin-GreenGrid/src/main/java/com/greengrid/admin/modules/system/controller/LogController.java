package com.greengrid.admin.modules.system.controller;

import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.common.result.Result;
import com.greengrid.admin.modules.system.dto.LogQuery;
import com.greengrid.admin.modules.system.service.LogService;
import com.greengrid.admin.modules.system.vo.LogVO;
import com.greengrid.admin.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口。权限：super（与前端路由 meta.roles 一致）。
 */
@Tag(name = "操作日志", description = "日志分页查询")
@RestController
@RequestMapping("/system/logs")
@RequiredArgsConstructor
@RequireRole({"super"})
public class LogController {

    private final LogService logService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping
    public Result<PageResult<LogVO>> list(LogQuery query) {
        return Result.ok(logService.page(query));
    }
}
