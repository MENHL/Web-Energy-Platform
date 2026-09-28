package com.greengrid.admin.modules.project.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 项目详情出参：项目信息 + 关联设备列表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectDetailVO extends ProjectVO {

    private List<DeviceBriefVO> devices;
}
