package com.greengrid.admin.modules.device.vo;

import lombok.Data;

import java.util.List;

/**
 * 设备监控指标（12 个采样点），字段与前端 DeviceMetrics 类型一致。
 */
@Data
public class DeviceMetricsVO {

    /** 时间点，如 "0:00" ~ "22:00" */
    private List<String> times;

    /** 负载率 % */
    private List<Integer> load;

    /** 温度 ℃ */
    private List<Integer> temp;
}
