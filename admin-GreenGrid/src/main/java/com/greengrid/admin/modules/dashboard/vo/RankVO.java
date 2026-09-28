package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

/**
 * 排行项，用于项目运营排行 Top5、物料入库量 Top5。
 */
@Data
public class RankVO {

    private Integer rank;

    private String name;

    private Long amount;
}
