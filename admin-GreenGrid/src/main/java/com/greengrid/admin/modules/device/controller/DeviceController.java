package com.greengrid.admin.modules.device.controller;

import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.common.result.Result;
import com.greengrid.admin.common.util.IpUtil;
import com.greengrid.admin.modules.device.dto.DeviceQuery;
import com.greengrid.admin.modules.device.dto.DeviceUpdateRequest;
import com.greengrid.admin.modules.device.service.DeviceService;
import com.greengrid.admin.modules.device.vo.DeviceDetailVO;
import com.greengrid.admin.modules.device.vo.DeviceVO;
import com.greengrid.admin.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备管理接口。权限：super / ops / project（与前端路由 meta.roles 一致）。
 */
@Tag(name = "设备管理", description = "设备查询、状态维护、删除")
@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@RequireRole({"super", "ops", "project"})
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "分页查询设备列表")
    @GetMapping
    public Result<PageResult<DeviceVO>> list(DeviceQuery query) {
        return Result.ok(deviceService.page(query));
    }

    @Operation(summary = "查询设备详情（含监控指标）")
    @GetMapping("/{id}")
    public Result<DeviceDetailVO> detail(@PathVariable Long id) {
        return Result.ok(deviceService.detail(id));
    }

    @Operation(summary = "更新设备状态（维护操作）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody DeviceUpdateRequest request,
                               HttpServletRequest httpRequest) {
        deviceService.update(id, request, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        deviceService.delete(id, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }
}
