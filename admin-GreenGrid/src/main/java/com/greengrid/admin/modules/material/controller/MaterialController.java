package com.greengrid.admin.modules.material.controller;

import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.common.result.Result;
import com.greengrid.admin.common.util.IpUtil;
import com.greengrid.admin.modules.material.dto.MaterialQuery;
import com.greengrid.admin.modules.material.dto.MaterialSaveRequest;
import com.greengrid.admin.modules.material.service.MaterialService;
import com.greengrid.admin.modules.material.vo.MaterialDetailVO;
import com.greengrid.admin.modules.material.vo.MaterialVO;
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
 * 物料管理接口。权限：super / ops / warehouse（与前端路由 meta.roles 一致）。
 */
@Tag(name = "物料管理", description = "物料增删改查 + 出入库追溯")
@RestController
@RequestMapping("/materials")
@RequiredArgsConstructor
@RequireRole({"super", "ops", "warehouse"})
public class MaterialController {

    private final MaterialService materialService;

    @Operation(summary = "分页查询物料列表")
    @GetMapping
    public Result<PageResult<MaterialVO>> list(MaterialQuery query) {
        return Result.ok(materialService.page(query));
    }

    @Operation(summary = "查询物料详情（含出入库记录）")
    @GetMapping("/{id}")
    public Result<MaterialDetailVO> detail(@PathVariable Long id) {
        return Result.ok(materialService.detail(id));
    }

    @Operation(summary = "新增物料")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody MaterialSaveRequest request, HttpServletRequest httpRequest) {
        materialService.create(request, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }

    @Operation(summary = "编辑物料")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody MaterialSaveRequest request,
                               HttpServletRequest httpRequest) {
        materialService.update(id, request, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }

    @Operation(summary = "删除物料")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        materialService.delete(id, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }
}
