package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

/**
 * 最近动态项，字段与 Mock activities 一致（time 为 HH:mm）。
 */
@Data
public class ActivityVO {

    private Long id;

    private String content;

    private String time;
}
