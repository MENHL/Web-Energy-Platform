package com.greengrid.admin.modules.project.controller;

import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.common.result.Result;
import com.greengrid.admin.common.util.IpUtil;
import com.greengrid.admin.modules.project.dto.ProjectQuery;
import com.greengrid.admin.modules.project.dto.ProjectSaveRequest;
import com.greengrid.admin.modules.project.service.ProjectService;
import com.greengrid.admin.modules.project.vo.ProjectDetailVO;
import com.greengrid.admin.modules.project.vo.ProjectVO;
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
 * 项目管理接口。
 * <p>完整路径 /api/projects（context-path 前缀由服务端统一加）。</p>
 * <p>权限：super / ops / project（与前端路由 meta.roles 一致）。</p>
 */
@Tag(name = "项目管理", description = "项目增删改查")
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@RequireRole({"super", "ops", "project"})
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "分页查询项目列表")
    @GetMapping
    public Result<PageResult<ProjectVO>> list(ProjectQuery query) {
        return Result.ok(projectService.page(query));
    }

    @Operation(summary = "查询项目详情（含关联设备）")
    @GetMapping("/{id}")
    public Result<ProjectDetailVO> detail(@PathVariable Long id) {
        return Result.ok(projectService.detail(id));
    }

    @Operation(summary = "新增项目")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody ProjectSaveRequest request, HttpServletRequest httpRequest) {
        projectService.create(request, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }

    @Operation(summary = "编辑项目")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ProjectSaveRequest request,
                               HttpServletRequest httpRequest) {
        projectService.update(id, request, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        projectService.delete(id, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }
}
