package com.greengrid.admin.modules.device.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备出参，字段与前端 DeviceItem 类型（src/types/index.ts）一一对应。
 */
@Data
public class DeviceVO {

    private Long id;

    private String deviceCode;

    private String name;

    private String type;

    /** 所属项目名称（由项目表 JOIN 得到） */
    private String projectName;

    /** 在线/离线/故障/维护中 */
    private String status;

    private LocalDate installDate;

    private LocalDateTime lastUpdateAt;
}
