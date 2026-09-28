package com.greengrid.admin.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greengrid.admin.common.exception.BizException;
import com.greengrid.admin.common.result.ErrorCode;
import com.greengrid.admin.modules.auth.dto.LoginRequest;
import com.greengrid.admin.modules.auth.vo.LoginVO;
import com.greengrid.admin.modules.auth.vo.UserInfoVO;
import com.greengrid.admin.modules.system.entity.SysUser;
import com.greengrid.admin.modules.system.mapper.SysUserMapper;
import com.greengrid.admin.modules.system.service.OperationLogService;
import com.greengrid.admin.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务：登录、获取当前用户。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OperationLogService operationLogService;

    /**
     * 账号密码登录，成功返回 token + 用户信息。
     */
    public LoginVO login(LoginRequest request, String ip) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("该账号已被禁用，请联系管理员");
        }

        // 更新最近登录时间（同时让返回的用户信息带最新值）
        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginAt(now);
        sysUserMapper.updateById(update);

        // 签发 token 并记录登录日志
        String token = jwtUtil.createToken(user.getId());
        operationLogService.record(
                "系统管理", "登录",
                "账号 " + user.getUsername() + " 登录平台成功",
                ip, user.getId(), user.getName());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(toUserInfoVO(user));
        return vo;
    }

    /**
     * 查询当前登录用户（供 /auth/me）。
     */
    public UserInfoVO currentUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return toUserInfoVO(user);
    }

    private UserInfoVO toUserInfoVO(SysUser user) {
        UserInfoVO vo = new UserInfoVO();
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
}
