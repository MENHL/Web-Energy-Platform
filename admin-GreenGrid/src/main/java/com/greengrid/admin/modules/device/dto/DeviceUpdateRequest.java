package com.greengrid.admin.modules.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 设备状态更新请求。前端只提交状态（维护操作），见 api/device.ts 的 updateDeviceStatus。
 */
@Data
public class DeviceUpdateRequest {

    @NotBlank(message = "设备状态不能为空")
    @Pattern(regexp = "在线|离线|故障|维护中", message = "设备状态不合法")
    private String status;
}
