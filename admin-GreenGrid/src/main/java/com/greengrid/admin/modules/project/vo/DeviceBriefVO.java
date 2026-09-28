package com.greengrid.admin.modules.project.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 关联设备简要出参，字段与前端 DeviceItem 类型（src/types/index.ts）一一对应。
 * <p>用于项目详情里的"关联设备"列表。</p>
 */
@Data
public class DeviceBriefVO {

    private Long id;

    private String deviceCode;

    private String name;

    private String type;

    /** 所属项目名称（由 JOIN 得到） */
    private String projectName;

    /** 在线/离线/故障/维护中 */
    private String status;

    private LocalDate installDate;

    private LocalDateTime lastUpdateAt;
}
