package com.greengrid.admin.modules.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 生成报表请求参数。前端提交 type / name（range 字段后端不接收）。
 */
@Data
public class ReportSaveRequest {

    @NotBlank(message = "报表类型不能为空")
    @Pattern(regexp = "运营汇总|库存分析|设备分析|物料分析", message = "报表类型不合法")
    private String type;

    @NotBlank(message = "报表名称不能为空")
    private String name;
}
