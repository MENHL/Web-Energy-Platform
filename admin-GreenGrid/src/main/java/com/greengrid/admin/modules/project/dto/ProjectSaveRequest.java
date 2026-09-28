package com.greengrid.admin.modules.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * 项目新增 / 编辑请求参数。字段与前端提交的 payload 一致。
 */
@Data
public class ProjectSaveRequest {

    @NotBlank(message = "项目名称不能为空")
    private String name;

    @NotBlank(message = "项目编号不能为空")
    private String projectCode;

    @NotBlank(message = "所属区域不能为空")
    @Pattern(regexp = "华东|华北|华南|华中|西北|西南|东北", message = "所属区域不合法")
    private String region;

    @NotBlank(message = "负责人不能为空")
    private String manager;

    @NotBlank(message = "项目状态不能为空")
    @Pattern(regexp = "进行中|已暂停|已完成", message = "项目状态不合法")
    private String status;

    @NotNull(message = "项目进度不能为空")
    @Min(value = 0, message = "进度不能小于 0")
    @Max(value = 100, message = "进度不能大于 100")
    private Integer progress;

    /** 计划开始日期（前端未选时传空串，由 Jackson 转 null） */
    private LocalDate startDate;

    /** 计划结束日期 */
    private LocalDate endDate;
}
