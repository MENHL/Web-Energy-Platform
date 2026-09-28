package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

/**
 * 名称 + 利用率项，用于仓库库容、设备类型利用率。
 */
@Data
public class NameUsageVO {

    private String name;

    private Integer usage;
}
