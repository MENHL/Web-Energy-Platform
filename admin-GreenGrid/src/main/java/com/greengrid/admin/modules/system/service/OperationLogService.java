package com.greengrid.admin.modules.system.service;

import com.greengrid.admin.modules.system.entity.SysOperationLog;
import com.greengrid.admin.modules.system.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务：各业务模块在关键操作处显式调用 record 记录日志。
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final SysOperationLogMapper operationLogMapper;

    public void record(String module, String action, String content, String ip, Long userId, String operator) {
        SysOperationLog log = new SysOperationLog();
        log.setModule(module);
        log.setAction(action);
        log.setContent(content);
        log.setIp(ip);
        log.setUserId(userId);
        log.setOperator(operator);
        operationLogMapper.insert(log);
    }
}
