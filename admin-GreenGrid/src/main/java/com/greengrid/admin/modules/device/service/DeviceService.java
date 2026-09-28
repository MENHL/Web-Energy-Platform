package com.greengrid.admin.modules.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greengrid.admin.common.exception.BizException;
import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.modules.device.dto.DeviceQuery;
import com.greengrid.admin.modules.device.dto.DeviceUpdateRequest;
import com.greengrid.admin.modules.device.entity.Device;
import com.greengrid.admin.modules.device.mapper.DeviceMapper;
import com.greengrid.admin.modules.device.vo.DeviceDetailVO;
import com.greengrid.admin.modules.device.vo.DeviceMetricsVO;
import com.greengrid.admin.modules.device.vo.DeviceVO;
import com.greengrid.admin.modules.device.vo.ProjectRef;
import com.greengrid.admin.modules.system.service.OperationLogService;
import com.greengrid.admin.security.LoginUser;
import com.greengrid.admin.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备服务：分页查询、详情（含监控指标）、状态维护、删除。
 */
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;
    private final OperationLogService operationLogService;

    /**
     * 分页查询设备列表，projectName 通过 IN 批量反查项目名（无 N+1）。
     */
    public PageResult<DeviceVO> page(DeviceQuery query) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        if (query.hasKeyword()) {
            String keyword = query.getKeyword();
            wrapper.and(w -> w.like(Device::getName, keyword)
                    .or().like(Device::getDeviceCode, keyword));
        }
        wrapper.eq(StringUtils.hasText(query.getType()), Device::getType, query.getType());
        wrapper.eq(StringUtils.hasText(query.getStatus()), Device::getStatus, query.getStatus());
        wrapper.orderByDesc(Device::getUpdatedAt);

        Page<Device> result = deviceMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        Map<Long, String> projectNameMap = loadProjectNames(result.getRecords());
        List<DeviceVO> list = result.getRecords().stream()
                .map(device -> toVO(device, projectNameMap.get(device.getProjectId())))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    /**
     * 查询设备详情（含 12 点监控指标，确定性生成保证刷新一致）。
     */
    public DeviceDetailVO detail(Long id) {
        Device device = getDeviceOrThrow(id);

        DeviceDetailVO vo = new DeviceDetailVO();
        DeviceVO base = toVO(device, loadProjectNames(List.of(device)).get(device.getProjectId()));
        copy(vo, base);
        vo.setMetrics(buildMetrics(device.getId()));
        return vo;
    }

    /**
     * 更新设备状态（维护操作），并刷新最后更新时间。
     */
    public void update(Long id, DeviceUpdateRequest request, String ip) {
        Device exist = getDeviceOrThrow(id);

        Device device = new Device();
        device.setId(id);
        device.setStatus(request.getStatus());
        device.setLastUpdateAt(LocalDateTime.now());
        deviceMapper.updateById(device);

        operationLogService.record(
                "设备管理", "维护",
                "提交设备 " + exist.getDeviceCode() + " 维护工单（状态置为" + request.getStatus() + "）",
                ip, currentUserId(), currentOperatorName());
    }

    /**
     * 删除设备（逻辑删除）。
     */
    public void delete(Long id, String ip) {
        Device exist = getDeviceOrThrow(id);
        deviceMapper.deleteById(id);

        operationLogService.record(
                "设备管理", "删除",
                "删除设备 " + exist.getDeviceCode() + "（" + exist.getName() + "）",
                ip, currentUserId(), currentOperatorName());
    }

    private Device getDeviceOrThrow(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BizException("设备不存在或已被删除");
        }
        return device;
    }

    private Map<Long, String> loadProjectNames(List<Device> devices) {
        List<Long> projectIds = devices.stream()
                .map(Device::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        return deviceMapper.selectProjectNames(projectIds).stream()
                .collect(Collectors.toMap(ProjectRef::getId, ProjectRef::getName, (a, b) -> a));
    }

    private DeviceVO toVO(Device device, String projectName) {
        DeviceVO vo = new DeviceVO();
        vo.setId(device.getId());
        vo.setDeviceCode(device.getDeviceCode());
        vo.setName(device.getName());
        vo.setType(device.getType());
        vo.setProjectName(projectName);
        vo.setStatus(device.getStatus());
        vo.setInstallDate(device.getInstallDate());
        vo.setLastUpdateAt(device.getLastUpdateAt());
        return vo;
    }

    private void copy(DeviceDetailVO target, DeviceVO source) {
        target.setId(source.getId());
        target.setDeviceCode(source.getDeviceCode());
        target.setName(source.getName());
        target.setType(source.getType());
        target.setProjectName(source.getProjectName());
        target.setStatus(source.getStatus());
        target.setInstallDate(source.getInstallDate());
        target.setLastUpdateAt(source.getLastUpdateAt());
    }

    /**
     * 生成 12 点监控指标（负载/温度），采用确定性伪随机：同一设备每次刷新曲线一致，
     * 与前端 Mock 的观感对齐。二期接入真实采集后改为查 device_metric 表。
     */
    private DeviceMetricsVO buildMetrics(Long deviceId) {
        List<String> times = new ArrayList<>();
        List<Integer> load = new ArrayList<>();
        List<Integer> temp = new ArrayList<>();

        // 用设备 id 扰动相位，让不同设备曲线不同
        double loadSeed = 1.7 + (deviceId % 10) * 0.11;
        double tempSeed = 2.3 + (deviceId % 7) * 0.13;

        for (int i = 0; i < 12; i++) {
            times.add((i * 2) + ":00");
            load.add((int) Math.round(60 + Math.abs(Math.sin(i * loadSeed)) * 35));
            temp.add((int) Math.round(60 + Math.abs(Math.sin(i * tempSeed)) * 35));
        }

        DeviceMetricsVO metrics = new DeviceMetricsVO();
        metrics.setTimes(times);
        metrics.setLoad(load);
        metrics.setTemp(temp);
        return metrics;
    }

    private Long currentUserId() {
        return UserContext.getUserId();
    }

    private String currentOperatorName() {
        LoginUser user = UserContext.get();
        return user == null ? "未知" : user.getName();
    }
}
