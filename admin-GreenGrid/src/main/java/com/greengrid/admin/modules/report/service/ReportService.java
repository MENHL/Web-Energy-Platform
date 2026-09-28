package com.greengrid.admin.modules.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greengrid.admin.common.exception.BizException;
import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.modules.report.dto.ReportQuery;
import com.greengrid.admin.modules.report.dto.ReportSaveRequest;
import com.greengrid.admin.modules.report.dto.ReportStatusRequest;
import com.greengrid.admin.modules.report.entity.Report;
import com.greengrid.admin.modules.report.mapper.ReportMapper;
import com.greengrid.admin.modules.report.vo.ReportVO;
import com.greengrid.admin.modules.system.service.OperationLogService;
import com.greengrid.admin.security.LoginUser;
import com.greengrid.admin.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 报表服务：分页查询、生成、取消、删除。
 * <p>一期只做状态流转，不真实跑导出任务。</p>
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final OperationLogService operationLogService;

    public PageResult<ReportVO> page(ReportQuery query) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (query.hasKeyword()) {
            wrapper.like(Report::getName, query.getKeyword());
        }
        wrapper.eq(StringUtils.hasText(query.getType()), Report::getType, query.getType());
        wrapper.orderByDesc(Report::getCreatedAt);

        Page<Report> result = reportMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<ReportVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal());
    }

    public void create(ReportSaveRequest request, String ip) {
        Report report = new Report();
        report.setType(request.getType());
        report.setName(request.getName());
        report.setStatus("生成中");
        report.setCreator(currentOperatorName());
        reportMapper.insert(report);

        operationLogService.record(
                "系统管理", "新增",
                "生成报表「" + request.getName() + "」（" + request.getType() + "）",
                ip, currentUserId(), currentOperatorName());
    }

    public void updateStatus(Long id, ReportStatusRequest request, String ip) {
        Report exist = getReportOrThrow(id);

        Report report = new Report();
        report.setId(id);
        report.setStatus(request.getStatus());
        reportMapper.updateById(report);

        operationLogService.record(
                "系统管理", "更新",
                "更新报表「" + exist.getName() + "」状态为" + request.getStatus(),
                ip, currentUserId(), currentOperatorName());
    }

    public void delete(Long id, String ip) {
        Report exist = getReportOrThrow(id);
        reportMapper.deleteById(id);

        operationLogService.record(
                "系统管理", "删除",
                "删除报表「" + exist.getName() + "」",
                ip, currentUserId(), currentOperatorName());
    }

    private Report getReportOrThrow(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException("报表不存在或已被删除");
        }
        return report;
    }

    private ReportVO toVO(Report report) {
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setType(report.getType());
        vo.setName(report.getName());
        vo.setStatus(report.getStatus());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setCreator(report.getCreator());
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
