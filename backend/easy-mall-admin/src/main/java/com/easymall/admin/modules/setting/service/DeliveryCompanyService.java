package com.easymall.admin.modules.setting.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.DeliveryCompany;
import com.easymall.admin.mapper.DeliveryCompanyMapper;
import com.easymall.admin.modules.setting.dto.DeliveryCompanyForm;
import com.easymall.admin.modules.setting.vo.DeliveryCompanyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 快递公司：维护 + 按名称解析编码。
 * <p>
 * 解析走 60 秒进程内缓存——物流轨迹查询在每次打开订单详情时都会调用，
 * 而这份名单几乎不变；新增/修改/删除时立即失效，不会读到旧数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryCompanyService {

    private static final long CACHE_TTL_MS = 60_000L;

    private final DeliveryCompanyMapper deliveryCompanyMapper;

    /** 名称 → 快递公司，含已停用的（历史订单的轨迹还要能查） */
    private volatile Map<String, DeliveryCompany> nameCache = Map.of();
    private volatile long cacheExpireAt = 0L;

    /** 列表：可不传 status 查全部 */
    public List<DeliveryCompanyVO> list(Integer status) {
        return deliveryCompanyMapper.selectList(Wrappers.<DeliveryCompany>lambdaQuery()
                        .eq(status != null, DeliveryCompany::getStatus, status)
                        .orderByAsc(DeliveryCompany::getSort)
                        .orderByAsc(DeliveryCompany::getId))
                .stream()
                .map(DeliveryCompanyVO::of)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(DeliveryCompanyForm form) {
        checkDuplicate(form.name(), form.code(), null);
        DeliveryCompany company = new DeliveryCompany();
        apply(company, form);
        deliveryCompanyMapper.insert(company);
        evictCache();
        return company.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DeliveryCompanyForm form) {
        DeliveryCompany company = require(id);
        checkDuplicate(form.name(), form.code(), id);
        apply(company, form);
        if (deliveryCompanyMapper.updateById(company) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "快递公司已被其他人修改，请刷新后重试");
        }
        evictCache();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        require(id);
        deliveryCompanyMapper.deleteById(id);
        evictCache();
    }

    /**
     * 按展示名解析（发货写编码、查询轨迹都用它）；未配置的自定义快递返回 null。
     */
    public DeliveryCompany resolveByName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return loadCache().get(name.trim());
    }

    /** 按展示名取快递100 编码；未配置返回 null，由调用方决定走单号识别 */
    public String resolveCode(String name) {
        DeliveryCompany company = resolveByName(name);
        return company == null ? null : company.getCode();
    }

    /* ---------------- private ---------------- */

    private Map<String, DeliveryCompany> loadCache() {
        if (System.currentTimeMillis() < cacheExpireAt) {
            return nameCache;
        }
        List<DeliveryCompany> all = deliveryCompanyMapper.selectList(Wrappers.<DeliveryCompany>lambdaQuery());
        nameCache = all.stream().collect(Collectors.toMap(
                DeliveryCompany::getName, Function.identity(), (a, b) -> a));
        cacheExpireAt = System.currentTimeMillis() + CACHE_TTL_MS;
        return nameCache;
    }

    private void evictCache() {
        cacheExpireAt = 0L;
    }

    private DeliveryCompany require(Long id) {
        DeliveryCompany company = deliveryCompanyMapper.selectById(id);
        if (company == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "快递公司不存在");
        }
        return company;
    }

    /** 名称与编码都不允许重复（表上没做唯一索引，见迁移脚本说明） */
    private void checkDuplicate(String name, String code, Long excludeId) {
        String trimmedName = name.trim();
        String trimmedCode = code.trim();

        DeliveryCompany sameName = deliveryCompanyMapper.selectOne(Wrappers.<DeliveryCompany>lambdaQuery()
                .eq(DeliveryCompany::getName, trimmedName)
                .ne(excludeId != null, DeliveryCompany::getId, excludeId)
                .last("LIMIT 1"));
        if (sameName != null) {
            throw new BusinessException("快递公司「" + trimmedName + "」已存在");
        }

        DeliveryCompany sameCode = deliveryCompanyMapper.selectOne(Wrappers.<DeliveryCompany>lambdaQuery()
                .eq(DeliveryCompany::getCode, trimmedCode)
                .ne(excludeId != null, DeliveryCompany::getId, excludeId)
                .last("LIMIT 1"));
        if (sameCode != null) {
            throw new BusinessException("快递100 编码「" + trimmedCode + "」已被「" + sameCode.getName() + "」占用");
        }
    }

    private void apply(DeliveryCompany company, DeliveryCompanyForm form) {
        company.setName(form.name().trim());
        company.setCode(form.code().trim());
        company.setPhoneRequired(Boolean.TRUE.equals(form.phoneRequired()) ? 1 : 0);
        company.setSort(form.sort() == null ? 0 : form.sort());
        company.setStatus(form.status() == null ? 1 : form.status());
        company.setRemark(StringUtils.hasText(form.remark()) ? form.remark().trim() : null);
    }
}
