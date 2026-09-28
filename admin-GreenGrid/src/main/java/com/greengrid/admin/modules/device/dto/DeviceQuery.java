package com.greengrid.admin.modules.device.dto;

import com.greengrid.admin.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备分页查询参数，字段与前端 DeviceList.vue 的 query 一致。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceQuery extends PageQuery {

    /** 设备类型 */
    private String type;

    /** 设备状态 */
    private String status;
}
