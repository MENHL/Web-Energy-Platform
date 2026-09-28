package com.greengrid.admin.modules.system.controller;

import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.common.result.Result;
import com.greengrid.admin.common.util.IpUtil;
import com.greengrid.admin.modules.system.dto.UserQuery;
import com.greengrid.admin.modules.system.dto.UserSaveRequest;
import com.greengrid.admin.modules.system.dto.UserUpdateRequest;
import com.greengrid.admin.modules.system.service.UserService;
import com.greengrid.admin.modules.system.vo.UserVO;
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
 * 用户与权限接口。权限：super（与前端路由 meta.roles 一致）。
 */
@Tag(name = "用户与权限", description = "用户增删改查、禁用启用")
@RestController
@RequestMapping("/system/users")
@RequiredArgsConstructor
@RequireRole({"super"})
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    public Result<PageResult<UserVO>> list(UserQuery query) {
        return Result.ok(userService.page(query));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserSaveRequest request, HttpServletRequest httpRequest) {
        userService.create(request, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }

    @Operation(summary = "编辑用户（含禁用/启用）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody UserUpdateRequest request,
                               HttpServletRequest httpRequest) {
        userService.update(id, request, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        userService.delete(id, IpUtil.getClientIp(httpRequest));
        return Result.ok();
    }
}
