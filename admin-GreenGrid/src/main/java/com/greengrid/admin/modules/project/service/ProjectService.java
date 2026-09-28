package com.greengrid.admin.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greengrid.admin.common.exception.BizException;
import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.modules.project.dto.ProjectQuery;
import com.greengrid.admin.modules.project.dto.ProjectSaveRequest;
import com.greengrid.admin.modules.project.entity.Project;
import com.greengrid.admin.modules.project.mapper.ProjectMapper;
import com.greengrid.admin.modules.project.vo.ProjectDetailVO;
import com.greengrid.admin.modules.project.vo.ProjectVO;
import com.greengrid.admin.modules.system.service.OperationLogService;
import com.greengrid.admin.security.LoginUser;
import com.greengrid.admin.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 项目服务：分页查询、详情、新增、编辑、删除。
 * <p>控制器只做参数与响应，业务规则与日志记录都在这里。</p>
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final OperationLogService operationLogService;

    /**
     * 分页查询项目列表。
     * <p>keyword 匹配项目名称或编号；status / region 精确匹配；按更新时间倒序。</p>
     */
    public PageResult<ProjectVO> page(ProjectQuery query) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (query.hasKeyword()) {
            String keyword = query.getKeyword();
            wrapper.and(w -> w.like(Project::getName, keyword)
                    .or().like(Project::getProjectCode, keyword));
        }
        wrapper.eq(StringUtils.hasText(query.getStatus()), Project::getStatus, query.getStatus());
        wrapper.eq(StringUtils.hasText(query.getRegion()), Project::getRegion, query.getRegion());
        wrapper.orderByDesc(Project::getUpdatedAt);

        Page<Project> result = projectMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<ProjectVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal());
    }

    /**
     * 查询项目详情（含关联设备）。
     */
    public ProjectDetailVO detail(Long id) {
        Project project = getProjectOrThrow(id);
        ProjectDetailVO vo = new ProjectDetailVO();
        copyToVO(project, vo);
        vo.setDevices(projectMapper.selectDevicesByProjectId(id));
        return vo;
    }

    /**
     * 新增项目。
     */
    public void create(ProjectSaveRequest request, String ip) {
        checkProjectCodeUnique(request.getProjectCode(), null);

        Project project = new Project();
        project.setProjectCode(request.getProjectCode());
        project.setName(request.getName());
        project.setRegion(request.getRegion());
        project.setManager(request.getManager());
        project.setStatus(request.getStatus());
        project.setProgress(request.getProgress());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        projectMapper.insert(project);

        operationLogService.record(
                "项目管理", "新增",
                "创建项目「" + request.getName() + "」（" + request.getProjectCode() + "）",
                ip, currentUserId(), currentOperatorName());
    }

    /**
     * 编辑项目（部分字段可能为空，日期清空也能生效）。
     */
    public void update(Long id, ProjectSaveRequest request, String ip) {
        Project exist = getProjectOrThrow(id);
        checkProjectCodeUnique(request.getProjectCode(), id);

        Project project = new Project();
        project.setId(id);
        project.setProjectCode(request.getProjectCode());
        project.setName(request.getName());
        project.setRegion(request.getRegion());
        project.setManager(request.getManager());
        project.setStatus(request.getStatus());
        project.setProgress(request.getProgress());
        project.setStartDate(request.getStartDate());
        project.setEndDate(request.getEndDate());
        projectMapper.updateById(project);

        operationLogService.record(
                "项目管理", "更新",
                "更新项目「" + exist.getName() + "」",
                ip, currentUserId(), currentOperatorName());
    }

    /**
     * 删除项目（逻辑删除；存在关联设备时拒绝删除）。
     */
    public void delete(Long id, String ip) {
        Project project = getProjectOrThrow(id);

        long deviceCount = projectMapper.countDevicesByProjectId(id);
        if (deviceCount > 0) {
            throw new BizException("该项目下存在 " + deviceCount + " 台设备，请先解绑后再删除");
        }
        projectMapper.deleteById(id);

        operationLogService.record(
                "项目管理", "删除",
                "删除项目「" + project.getName() + "」（" + project.getProjectCode() + "）",
                ip, currentUserId(), currentOperatorName());
    }

    private Project getProjectOrThrow(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BizException("项目不存在或已被删除");
        }
        return project;
    }

    private void checkProjectCodeUnique(String projectCode, Long excludeId) {
        Long count = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getProjectCode, projectCode)
                .ne(excludeId != null, Project::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("项目编号已存在");
        }
    }

    private ProjectVO toVO(Project project) {
        ProjectVO vo = new ProjectVO();
        copyToVO(project, vo);
        return vo;
    }

    private void copyToVO(Project project, ProjectVO vo) {
        vo.setId(project.getId());
        vo.setProjectCode(project.getProjectCode());
        vo.setName(project.getName());
        vo.setRegion(project.getRegion());
        vo.setManager(project.getManager());
        vo.setStatus(project.getStatus());
        vo.setProgress(project.getProgress());
        vo.setStartDate(project.getStartDate());
        vo.setEndDate(project.getEndDate());
        vo.setUpdatedAt(project.getUpdatedAt());
    }

    private Long currentUserId() {
        return UserContext.getUserId();
    }

    private String currentOperatorName() {
        LoginUser user = UserContext.get();
        return user == null ? "未知" : user.getName();
    }
}
