package com.greengrid.admin.modules.device.vo;

import lombok.Data;

/**
 * 项目简要信息（id + name），供设备列表批量反查项目名使用。
 */
@Data
public class ProjectRef {

    private Long id;

    private String name;
}
