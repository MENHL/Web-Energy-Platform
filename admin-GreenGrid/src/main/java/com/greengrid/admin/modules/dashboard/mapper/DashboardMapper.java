package com.greengrid.admin.modules.dashboard.mapper;

import com.greengrid.admin.modules.dashboard.vo.ActivityVO;
import com.greengrid.admin.modules.dashboard.vo.NameValueVO;
import com.greengrid.admin.modules.dashboard.vo.RankVO;
import com.greengrid.admin.modules.dashboard.vo.TrendPointVO;
import com.greengrid.admin.modules.dashboard.vo.WarningVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 看板聚合查询 Mapper。只做聚合读取，不绑定单一实体表。
 */
public interface DashboardMapper {

    // ==================== KPI 计数 ====================

    @Select("SELECT COUNT(*) FROM project WHERE status = '进行中' AND deleted = 0")
    long countOngoingProjects();

    @Select("SELECT COUNT(*) FROM device WHERE status = '在线' AND deleted = 0")
    long countOnlineDevices();

    @Select("SELECT COUNT(*) FROM material WHERE deleted = 0")
    long countMaterials();

    @Select("SELECT COUNT(*) FROM stock_order WHERE order_type = 'INBOUND' AND order_time >= #{start} AND order_time < #{end} AND deleted = 0")
    long countInboundBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM stock_order WHERE order_type = 'OUTBOUND' AND order_time >= #{start} AND order_time < #{end} AND deleted = 0")
    long countOutboundBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM material WHERE status IN ('缺货','低库存') AND deleted = 0")
    long countWarningMaterials();

    @Select("SELECT COUNT(*) FROM device WHERE status IN ('故障','维护中') AND deleted = 0")
    long countAlertDevices();

    // ==================== 设备状态分布 ====================

    @Select("SELECT CASE WHEN status = '在线' THEN '在线运行' ELSE status END AS name, COUNT(*) AS value " +
            "FROM device WHERE deleted = 0 GROUP BY status")
    List<NameValueVO> selectDeviceStatus();

    // ==================== 库存预警明细 ====================

    @Select("SELECT id, name, spec, stock, safety_stock AS safetyStock, status AS level " +
            "FROM material WHERE status IN ('缺货','低库存') AND deleted = 0 " +
            "ORDER BY FIELD(status, '缺货', '低库存'), stock ASC LIMIT 5")
    List<WarningVO> selectWarnings();

    // ==================== 最近动态 ====================

    @Select("SELECT id, content, DATE_FORMAT(created_at, '%H:%i') AS time " +
            "FROM sys_operation_log WHERE deleted = 0 ORDER BY created_at DESC LIMIT 4")
    List<ActivityVO> selectRecentActivities();

    // ==================== 入出库趋势（按天聚合） ====================

    @Select("SELECT DATE_FORMAT(order_time, '%m-%d') AS day, " +
            "COUNT(CASE WHEN order_type = 'INBOUND' THEN 1 END) AS inbound, " +
            "COUNT(CASE WHEN order_type = 'OUTBOUND' THEN 1 END) AS outbound " +
            "FROM stock_order WHERE order_time >= #{start} AND deleted = 0 " +
            "GROUP BY DATE_FORMAT(order_time, '%m-%d') ORDER BY MIN(order_time)")
    List<TrendPointVO> selectTrend(@Param("start") LocalDateTime start);

    // ==================== 库存结构（按分类聚合） ====================

    @Select("SELECT category AS name, CAST(SUM(stock) AS UNSIGNED) AS value " +
            "FROM material WHERE deleted = 0 GROUP BY category ORDER BY value DESC")
    List<NameValueVO> selectStockStructure();

    // ==================== 物料入库量 Top5 ====================

    @Select("SELECT material_name AS name, CAST(SUM(quantity) AS UNSIGNED) AS amount " +
            "FROM stock_order WHERE order_type = 'INBOUND' AND deleted = 0 " +
            "GROUP BY material_id, material_name ORDER BY amount DESC LIMIT 5")
    List<RankVO> selectMaterialRank();
}
