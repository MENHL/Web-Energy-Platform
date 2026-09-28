package com.greengrid.admin.modules.project.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目出参，字段与前端 ProjectItem 类型（src/types/index.ts）一一对应。
 */
@Data
public class ProjectVO {

    private Long id;

    private String projectCode;

    private String name;

    private String region;

    private String manager;

    /** 进行中/已暂停/已完成 */
    private String status;

    /** 进度 0-100 */
    private Integer progress;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDateTime updatedAt;
}
