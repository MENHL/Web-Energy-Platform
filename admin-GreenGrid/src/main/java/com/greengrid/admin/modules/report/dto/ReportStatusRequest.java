package com.greengrid.admin.modules.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 报表状态更新请求（取消生成中的报表时前端传 status=已取消）。
 */
@Data
public class ReportStatusRequest {

    @NotBlank(message = "报表状态不能为空")
    @Pattern(regexp = "生成中|已生成|已取消", message = "报表状态不合法")
    private String status;
}
