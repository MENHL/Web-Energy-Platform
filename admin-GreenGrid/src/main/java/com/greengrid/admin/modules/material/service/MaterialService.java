package com.greengrid.admin.modules.material.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greengrid.admin.common.exception.BizException;
import com.greengrid.admin.common.result.PageResult;
import com.greengrid.admin.modules.material.dto.MaterialQuery;
import com.greengrid.admin.modules.material.dto.MaterialSaveRequest;
import com.greengrid.admin.modules.material.entity.Material;
import com.greengrid.admin.modules.material.mapper.MaterialMapper;
import com.greengrid.admin.modules.material.vo.MaterialDetailVO;
import com.greengrid.admin.modules.material.vo.MaterialVO;
import com.greengrid.admin.modules.system.service.OperationLogService;
import com.greengrid.admin.security.LoginUser;
import com.greengrid.admin.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 物料服务：分页查询、详情（含出入库追溯）、新增、编辑、删除。
 * <p>库存状态（正常/低库存/缺货）由服务层在写入时统一重算，前端不传。</p>
 */
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialMapper materialMapper;
    private final OperationLogService operationLogService;

    public PageResult<MaterialVO> page(MaterialQuery query) {
        LambdaQueryWrapper<Material> wrapper = new LambdaQueryWrapper<>();
        if (query.hasKeyword()) {
            String keyword = query.getKeyword();
            wrapper.and(w -> w.like(Material::getName, keyword)
                    .or().like(Material::getMaterialCode, keyword));
        }
        wrapper.eq(StringUtils.hasText(query.getCategory()), Material::getCategory, query.getCategory());
        wrapper.eq(StringUtils.hasText(query.getStatus()), Material::getStatus, query.getStatus());
        wrapper.orderByDesc(Material::getUpdatedAt);

        Page<Material> result = materialMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);

        List<MaterialVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal());
    }

    public MaterialDetailVO detail(Long id) {
        Material material = getMaterialOrThrow(id);

        MaterialDetailVO vo = new MaterialDetailVO();
        copyToVO(material, vo);
        vo.setInboundRecords(materialMapper.selectOrdersByMaterial(id, "INBOUND"));
        vo.setOutboundRecords(materialMapper.selectOrdersByMaterial(id, "OUTBOUND"));
        return vo;
    }

    public void create(MaterialSaveRequest request, String ip) {
        checkMaterialCodeUnique(request.getMaterialCode(), null);

        Material material = new Material();
        material.setMaterialCode(request.getMaterialCode());
        material.setName(request.getName());
        material.setCategory(request.getCategory());
        material.setSpec(request.getSpec());
        material.setUnit(request.getUnit());
        material.setStock(request.getStock());
        material.setSafetyStock(request.getSafetyStock());
        material.setStatus(calcStatus(request.getStock(), request.getSafetyStock()));
        materialMapper.insert(material);

        operationLogService.record(
                "物料管理", "新增",
                "新增物料「" + request.getName() + "」（" + request.getMaterialCode() + "）",
                ip, currentUserId(), currentOperatorName());
    }

    public void update(Long id, MaterialSaveRequest request, String ip) {
        Material exist = getMaterialOrThrow(id);
        checkMaterialCodeUnique(request.getMaterialCode(), id);

        Material material = new Material();
        material.setId(id);
        material.setMaterialCode(request.getMaterialCode());
        material.setName(request.getName());
        material.setCategory(request.getCategory());
        material.setSpec(request.getSpec());
        material.setUnit(request.getUnit());
        material.setStock(request.getStock());
        material.setSafetyStock(request.getSafetyStock());
        material.setStatus(calcStatus(request.getStock(), request.getSafetyStock()));
        materialMapper.updateById(material);

        operationLogService.record(
                "物料管理", "更新",
                "更新物料「" + exist.getName() + "」",
                ip, currentUserId(), currentOperatorName());
    }

    public void delete(Long id, String ip) {
        Material exist = getMaterialOrThrow(id);
        materialMapper.deleteById(id);

        operationLogService.record(
                "物料管理", "删除",
                "删除物料「" + exist.getName() + "」（" + exist.getMaterialCode() + "）",
                ip, currentUserId(), currentOperatorName());
    }

    private Material getMaterialOrThrow(Long id) {
        Material material = materialMapper.selectById(id);
        if (material == null) {
            throw new BizException("物料不存在或已被删除");
        }
        return material;
    }

    private void checkMaterialCodeUnique(String materialCode, Long excludeId) {
        Long count = materialMapper.selectCount(new LambdaQueryWrapper<Material>()
                .eq(Material::getMaterialCode, materialCode)
                .ne(excludeId != null, Material::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException("物料编码已存在");
        }
    }

    /**
     * 库存状态派生规则：stock==0 -> 缺货；stock&lt;safetyStock -> 低库存；否则正常。
     */
    private String calcStatus(Integer stock, Integer safetyStock) {
        if (stock == null || stock == 0) {
            return "缺货";
        }
        if (safetyStock != null && stock < safetyStock) {
            return "低库存";
        }
        return "正常";
    }

    private MaterialVO toVO(Material material) {
        MaterialVO vo = new MaterialVO();
        copyToVO(material, vo);
        return vo;
    }

    private void copyToVO(Material material, MaterialVO vo) {
        vo.setId(material.getId());
        vo.setMaterialCode(material.getMaterialCode());
        vo.setName(material.getName());
        vo.setCategory(material.getCategory());
        vo.setSpec(material.getSpec());
        vo.setUnit(material.getUnit());
        vo.setStock(material.getStock());
        vo.setSafetyStock(material.getSafetyStock());
        vo.setStatus(material.getStatus());
    }

    private Long currentUserId() {
        return UserContext.getUserId();
    }

    private String currentOperatorName() {
        LoginUser user = UserContext.get();
        return user == null ? "未知" : user.getName();
    }
}
