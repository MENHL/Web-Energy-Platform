package com.greengrid.admin.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greengrid.admin.common.exception.BizException;
import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.modules.system.dto.UserQuery;
import com.greengrid.admin.modules.system.dto.UserSaveRequest;
import com.greengrid.admin.modules.system.dto.UserUpdateRequest;
import com.greengrid.admin.modules.system.entity.SysUser;
import com.greengrid.admin.modules.system.mapper.SysUserMapper;
import com.greengrid.admin.modules.system.vo.UserVO;
import com.greengrid.admin.security.LoginUser;
import com.greengrid.admin.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户服务：分页查询、新增、编辑、禁用/启用、删除。
 * <p>新增用户默认密码 123456（BCrypt），roleName 按 role 映射。</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public PageResult<UserVO> page(UserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (query.hasKeyword()) {
            String keyword = query.getKeyword();
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getName, keyword));
        }
        wrapper.eq(StringUtils.hasText(query.getRole()), SysUser::getRole, query.getRole());
        wrapper.eq(query.getStatus() != null, SysUser::getStatus, query.getStatus());
        wrapper.orderByAsc(SysUser::getId);

        Page<SysUser> result = userMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<UserVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal());
    }

    public void create(UserSaveRequest request, String ip) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count != null && count > 0) {
            throw new BizException("账号 / 工号已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setRole(request.getRole());
        user.setRoleName(roleNameOf(request.getRole()));
        user.setDept(request.getDept());
        user.setStatus(1);
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.insert(user);

        operationLogService.record(
                "系统管理", "新增",
                "新增用户「" + request.getName() + "」（" + request.getUsername() + "）",
                ip, currentUserId(), currentOperatorName());
    }

    public void update(Long id, UserUpdateRequest request, String ip) {
        SysUser exist = getUserOrThrow(id);
        checkProtection(exist, request.getStatus());

        SysUser user = new SysUser();
        user.setId(id);
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
            user.setRoleName(roleNameOf(request.getRole()));
        }
        if (request.getDept() != null) {
            user.setDept(request.getDept());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        userMapper.updateById(user);

        operationLogService.record(
                "系统管理", "更新",
                "更新用户「" + exist.getName() + "」（" + exist.getUsername() + "）",
                ip, currentUserId(), currentOperatorName());
    }

    public void delete(Long id, String ip) {
        SysUser exist = getUserOrThrow(id);
        checkProtection(exist, null);

        userMapper.deleteById(id);

        operationLogService.record(
                "系统管理", "删除",
                "删除用户「" + exist.getName() + "」（" + exist.getUsername() + "）",
                ip, currentUserId(), currentOperatorName());
    }

    private SysUser getUserOrThrow(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在或已被删除");
        }
        return user;
    }

    /**
     * 保护规则：不能删除/禁用自己；系统至少保留一个超级管理员。
     */
    private void checkProtection(SysUser target, Integer newStatus) {
        boolean willDisableOrDelete = newStatus == null || newStatus == 0;
        if (!willDisableOrDelete) {
            return;
        }
        if (target.getId().equals(currentUserId())) {
            throw new BizException("不能删除或禁用自己");
        }
        if ("super".equals(target.getRole())) {
            Long superCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRole, "super"));
            if (superCount != null && superCount <= 1) {
                throw new BizException("系统至少需要保留一个超级管理员");
            }
        }
    }

    private String roleNameOf(String role) {
        return switch (role) {
            case "super" -> "超级管理员";
            case "ops" -> "运营管理员";
            case "warehouse" -> "仓储管理员";
            case "project" -> "项目管理员";
            case "guest" -> "普通用户";
            default -> "普通用户";
        };
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setRole(user.getRole());
        vo.setRoleName(user.getRoleName());
        vo.setDept(user.getDept());
        vo.setStatus(user.getStatus());
        vo.setLastLoginAt(user.getLastLoginAt());
        return vo;
    }

    private Long currentUserId() {
        return UserContext.getUserId();
    }

    private String currentOperatorName() {
        LoginUser user = UserContext.get();
        return user == null ? "未知" : user.getName();
    }
}
