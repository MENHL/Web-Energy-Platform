package com.greengrid.admin.modules.device.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备详情出参：设备信息 + 监控指标。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceDetailVO extends DeviceVO {

    private DeviceMetricsVO metrics;
}
