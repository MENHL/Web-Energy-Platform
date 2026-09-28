package com.greengrid.admin.modules.material.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 物料新增 / 编辑请求参数。字段与前端提交的 payload 一致（status 由后端重算）。
 */
@Data
public class MaterialSaveRequest {

    @NotBlank(message = "物料名称不能为空")
    private String name;

    @NotBlank(message = "物料编码不能为空")
    private String materialCode;

    @NotBlank(message = "物料分类不能为空")
    @Pattern(regexp = "电芯电池|光伏组件|电气元件|结构件辅材", message = "物料分类不合法")
    private String category;

    @NotBlank(message = "单位不能为空")
    private String unit;

    /** 规格型号（可选） */
    private String spec;

    @NotNull(message = "当前库存不能为空")
    @Min(value = 0, message = "库存不能小于 0")
    private Integer stock;

    @NotNull(message = "安全库存不能为空")
    @Min(value = 0, message = "安全库存不能小于 0")
    private Integer safetyStock;
}
