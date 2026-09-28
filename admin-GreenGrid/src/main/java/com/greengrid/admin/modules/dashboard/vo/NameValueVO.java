package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

/**
 * 名称 + 数值项，用于设备状态分布 items、库存结构 items。
 */
@Data
public class NameValueVO {

    private String name;

    private Long value;
}
