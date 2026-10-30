package com.easymall.admin.modules.trade.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.easymall.admin.common.api.ResultCode;
import com.easymall.admin.common.exception.BusinessException;
import com.easymall.admin.entity.FreightConfig;
import com.easymall.admin.entity.FreightRule;
import com.easymall.admin.mapper.FreightConfigMapper;
import com.easymall.admin.mapper.FreightRuleMapper;
import com.easymall.admin.mapper.RegionMapper;
import com.easymall.admin.modules.trade.dto.FreightConfigForm;
import com.easymall.admin.modules.trade.dto.FreightRuleForm;
import com.easymall.admin.modules.trade.vo.FreightDetailVO;
import com.easymall.admin.modules.trade.vo.FreightRuleVO;
import com.easymall.admin.modules.trade.vo.RegionNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 运费管理：全局配置 + 区域规则（省 / 市两级）。
 * 计费口径与 api 端 FreightService 保持一致：市规则 > 省规则 > 统一运费。
 */
@Service
@RequiredArgsConstructor
public class FreightService extends ServiceImpl<FreightRuleMapper, FreightRule> {

    /** 规则只允许配到省、市两级 */
    private static final int REGION_LEVEL_PROVINCE = 1;
    private static final int REGION_LEVEL_CITY = 2;

    private final FreightConfigMapper freightConfigMapper;
    private final RegionMapper regionMapper;

    /**
     * 运费管理详情：全局配置 + 全部区域规则。
     */
    public FreightDetailVO detail() {
        FreightConfig config = requireConfig();
        FreightDetailVO vo = new FreightDetailVO();
        vo.setDefaultFee(config.getDefaultFee());
        vo.setFreeAmount(config.getFreeAmount());
        vo.setVersion(config.getVersion());

        Map<String, Map<String, Object>> regionMap = allRegions().stream()
                .collect(Collectors.toMap(row -> (String) row.get("areaCode"), Function.identity()));

        List<FreightRuleVO> rules = list(Wrappers.<FreightRule>lambdaQuery()
                        .orderByAsc(FreightRule::getRegionLevel)
                        .orderByAsc(FreightRule::getRegionCode))
                .stream()
                .map(rule -> toVO(rule, regionMap))
                .toList();
        vo.setRules(rules);
        return vo;
    }

    /**
     * 更新全局运费配置（统一运费 + 全国满额包邮门槛）。
     */
    public void updateConfig(FreightConfigForm form) {
        FreightConfig config = requireConfig();
        if (form.freeAmount() != null
                && form.freeAmount().compareTo(BigDecimal.ZERO) > 0
                && form.defaultFee().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("统一运费为 0 时已全国包邮，无需设置满额包邮门槛");
        }
        config.setDefaultFee(form.defaultFee());
        // 0 视为不启用，归一为 NULL
        config.setFreeAmount(form.freeAmount() == null || form.freeAmount().compareTo(BigDecimal.ZERO) <= 0
                ? null : form.freeAmount());
        if (form.version() != null) {
            config.setVersion(form.version());
        }
        if (freightConfigMapper.updateById(config) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "配置已被其他人修改，请刷新后重试");
        }
    }

    /**
     * 新增区域规则：地区必须是省或市，且同一地区只能有一条规则。
     */
    @Transactional(rollbackFor = Exception.class)
    public FreightRuleVO createRule(FreightRuleForm form) {
        Map<String, Object> region = requireRegion(form.regionCode());
        if (count(Wrappers.<FreightRule>lambdaQuery()
                .eq(FreightRule::getRegionCode, form.regionCode())) > 0) {
            throw new BusinessException("该地区已存在运费规则，请直接编辑");
        }
        FreightRule rule = new FreightRule();
        applyForm(rule, form);
        rule.setRegionCode(form.regionCode());
        rule.setRegionName((String) region.get("name"));
        rule.setRegionLevel((Integer) region.get("level"));
        save(rule);
        return toVO(rule, allRegionMap());
    }

    public FreightRuleVO updateRule(Long id, FreightRuleForm form) {
        FreightRule rule = getById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        // 编辑时地区不可变更（要换地区请删除后新增），仅校验传值一致
        if (!rule.getRegionCode().equals(form.regionCode())) {
            throw new BusinessException("规则地区不可修改，请删除后重新新增");
        }
        applyForm(rule, form);
        if (form.version() != null) {
            rule.setVersion(form.version());
        }
        if (!updateById(rule)) {
            throw new BusinessException(ResultCode.DATA_CONFLICT, "规则已被其他人修改，请刷新后重试");
        }
        return toVO(rule, allRegionMap());
    }

    public void deleteRule(Long id) {
        if (getById(id) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND);
        }
        removeById(id);
    }

    /**
     * 省 / 市两级区域树（省 34 + 市 371，一次性返回）。
     */
    public List<RegionNodeVO> regionTree() {
        Map<String, List<Map<String, Object>>> citiesByParent = regionMapper.selectCities().stream()
                .collect(Collectors.groupingBy(row -> (String) row.get("parentCode")));

        List<RegionNodeVO> tree = new ArrayList<>();
        for (Map<String, Object> province : regionMapper.selectProvinces()) {
            RegionNodeVO node = toNode(province, false);
            List<RegionNodeVO> children = citiesByParent
                    .getOrDefault((String) province.get("areaCode"), List.of())
                    .stream()
                    .map(city -> toNode(city, true))
                    .toList();
            node.setChildren(children);
            tree.add(node);
        }
        return tree;
    }

    /* ---------------- private ---------------- */

    private FreightConfig requireConfig() {
        FreightConfig config = freightConfigMapper.selectById(1L);
        if (config == null) {
            // 兜底：迁移脚本未执行时自动补一行默认配置
            config = new FreightConfig();
            config.setId(1L);
            config.setDefaultFee(BigDecimal.ZERO);
            freightConfigMapper.insert(config);
        }
        return config;
    }

    private Map<String, Object> requireRegion(String areaCode) {
        Map<String, Object> region = regionMapper.selectByAreaCode(areaCode);
        if (region == null) {
            throw new BusinessException("地区不存在");
        }
        Integer level = (Integer) region.get("level");
        if (level == null || (level != REGION_LEVEL_PROVINCE && level != REGION_LEVEL_CITY)) {
            throw new BusinessException("运费规则只支持按省或市设置");
        }
        return region;
    }

    private void applyForm(FreightRule rule, FreightRuleForm form) {
        boolean deliverable = form.deliverable() != null && form.deliverable() == 1;
        rule.setDeliverable(deliverable ? 1 : 0);
        if (!deliverable) {
            // 不配送地区不产生运费语义
            rule.setFee(BigDecimal.ZERO);
            rule.setFreeAmount(null);
            return;
        }
        BigDecimal fee = form.fee() == null ? BigDecimal.ZERO : form.fee();
        if (fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("运费不能小于 0");
        }
        rule.setFee(fee);
        rule.setFreeAmount(form.freeAmount() == null || form.freeAmount().compareTo(BigDecimal.ZERO) <= 0
                ? null : form.freeAmount());
    }

    private List<Map<String, Object>> allRegions() {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.addAll(regionMapper.selectProvinces());
        rows.addAll(regionMapper.selectCities());
        return rows;
    }

    private Map<String, Map<String, Object>> allRegionMap() {
        return allRegions().stream()
                .collect(Collectors.toMap(row -> (String) row.get("areaCode"), Function.identity()));
    }

    private FreightRuleVO toVO(FreightRule rule, Map<String, Map<String, Object>> regionMap) {
        FreightRuleVO vo = new FreightRuleVO();
        vo.setId(rule.getId());
        vo.setRegionCode(rule.getRegionCode());
        vo.setRegionName(rule.getRegionName());
        vo.setRegionLevel(rule.getRegionLevel());
        vo.setDeliverable(rule.getDeliverable());
        vo.setFee(rule.getFee());
        vo.setFreeAmount(rule.getFreeAmount());
        vo.setUpdatedAt(rule.getUpdatedAt());
        vo.setVersion(rule.getVersion());
        vo.setRegionPath(regionPath(rule, regionMap));
        return vo;
    }

    /** 展示路径：省级「广东省」；市级「广东省 / 深圳市」 */
    private String regionPath(FreightRule rule, Map<String, Map<String, Object>> regionMap) {
        if (rule.getRegionLevel() != null && rule.getRegionLevel() == REGION_LEVEL_CITY) {
            Map<String, Object> region = regionMap.get(rule.getRegionCode());
            if (region != null) {
                Map<String, Object> parent = regionMap.get((String) region.get("parentCode"));
                if (parent != null) {
                    return parent.get("name") + " / " + rule.getRegionName();
                }
            }
        }
        return rule.getRegionName();
    }

    private RegionNodeVO toNode(Map<String, Object> row, boolean leaf) {
        RegionNodeVO node = new RegionNodeVO();
        node.setCode((String) row.get("areaCode"));
        node.setName((String) row.get("name"));
        node.setLevel((Integer) row.get("level"));
        node.setChildren(leaf ? List.of() : new ArrayList<>());
        return node;
    }
}
