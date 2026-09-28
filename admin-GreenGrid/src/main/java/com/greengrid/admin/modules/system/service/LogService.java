package com.greengrid.admin.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.modules.system.dto.LogQuery;
import com.greengrid.admin.modules.system.entity.SysOperationLog;
import com.greengrid.admin.modules.system.mapper.SysOperationLogMapper;
import com.greengrid.admin.modules.system.vo.LogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 操作日志服务：分页查询。
 */
@Service
@RequiredArgsConstructor
public class LogService {

    private final SysOperationLogMapper operationLogMapper;

    public PageResult<LogVO> page(LogQuery query) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (query.hasKeyword()) {
            String keyword = query.getKeyword();
            wrapper.and(w -> w.like(SysOperationLog::getOperator, keyword)
                    .or().like(SysOperationLog::getContent, keyword));
        }
        wrapper.eq(StringUtils.hasText(query.getModule()), SysOperationLog::getModule, query.getModule());
        wrapper.eq(StringUtils.hasText(query.getAction()), SysOperationLog::getAction, query.getAction());
        wrapper.orderByDesc(SysOperationLog::getCreatedAt);

        Page<SysOperationLog> result = operationLogMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<LogVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal());
    }

    private LogVO toVO(SysOperationLog log) {
        LogVO vo = new LogVO();
        vo.setId(log.getId());
        vo.setOperator(log.getOperator());
        vo.setModule(log.getModule());
        vo.setAction(log.getAction());
        vo.setContent(log.getContent());
        vo.setIp(log.getIp());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
