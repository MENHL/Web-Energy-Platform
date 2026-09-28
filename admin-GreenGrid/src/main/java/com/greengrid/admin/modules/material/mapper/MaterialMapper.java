package com.greengrid.admin.modules.material.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greengrid.admin.modules.material.entity.Material;
import com.greengrid.admin.modules.material.vo.OrderItemVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 物料表 Mapper。
 */
public interface MaterialMapper extends BaseMapper<Material> {

    /**
     * 查询某物料的入库 / 出库记录（追溯）。
     */
    @Select("SELECT id, order_no AS orderNo, material_name AS materialName, quantity, " +
            "       target, operator, order_time AS time " +
            "FROM stock_order " +
            "WHERE material_id = #{materialId} AND order_type = #{orderType} AND deleted = 0 " +
            "ORDER BY order_time DESC")
    List<OrderItemVO> selectOrdersByMaterial(@Param("materialId") Long materialId,
                                             @Param("orderType") String orderType);
}
