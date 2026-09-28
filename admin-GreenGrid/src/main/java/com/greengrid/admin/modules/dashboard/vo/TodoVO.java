package com.greengrid.admin.modules.dashboard.vo;

import lombok.Data;

/**
 * 今日待办项，字段与 Mock todos 一致。
 */
@Data
public class TodoVO {

    private Long id;

    private String content;

    /** 紧急 / 今日 / 本周 */
    private String level;
}
