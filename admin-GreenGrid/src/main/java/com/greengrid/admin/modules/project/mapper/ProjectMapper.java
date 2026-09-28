package com.greengrid.admin.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greengrid.admin.modules.project.entity.Project;
import com.greengrid.admin.modules.project.vo.DeviceBriefVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 项目表 Mapper。基础 CRUD 由 BaseMapper 提供，复杂查询在此追加。
 */
public interface ProjectMapper extends BaseMapper<Project> {

    /**
     * 查询项目下关联的设备（LEFT JOIN 项目表得到项目名），供详情"关联设备"使用。
     */
    @Select("SELECT d.id, d.device_code AS deviceCode, d.name, d.type, " +
            "       p.name AS projectName, d.status, d.install_date AS installDate, " +
            "       d.last_update_at AS lastUpdateAt " +
            "FROM device d " +
            "LEFT JOIN project p ON d.project_id = p.id " +
            "WHERE d.project_id = #{projectId} AND d.deleted = 0 " +
            "ORDER BY d.id")
    List<DeviceBriefVO> selectDevicesByProjectId(@Param("projectId") Long projectId);

    /**
     * 统计项目下未删除的设备数量，用于删除前的关联校验。
     */
    @Select("SELECT COUNT(*) FROM device WHERE project_id = #{projectId} AND deleted = 0")
    long countDevicesByProjectId(@Param("projectId") Long projectId);
}
