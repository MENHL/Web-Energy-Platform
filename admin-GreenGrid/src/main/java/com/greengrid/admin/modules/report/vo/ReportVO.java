package com.greengrid.admin.modules.report.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报表出参，字段与前端 ReportItem 类型（src/types/index.ts）一一对应。
 */
@Data
public class ReportVO {

    private Long id;

    private String type;

    private String name;

    /** 生成中/已生成/已取消 */
    private String status;

    private LocalDateTime createdAt;

    private String creator;
}
