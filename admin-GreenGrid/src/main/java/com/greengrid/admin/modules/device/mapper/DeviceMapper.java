package com.greengrid.admin.modules.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greengrid.admin.modules.device.entity.Device;
import com.greengrid.admin.modules.device.vo.ProjectRef;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 设备表 Mapper。
 */
public interface DeviceMapper extends BaseMapper<Device> {

    /**
     * 批量反查项目名称（避免列表 N+1，用 IN 一次查出）。
     */
    @Select("<script>" +
            "SELECT id, name FROM project WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND deleted = 0" +
            "</script>")
    List<ProjectRef> selectProjectNames(@Param("ids") List<Long> ids);
}
