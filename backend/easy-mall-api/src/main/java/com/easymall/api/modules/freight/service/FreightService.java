package com.easymall.api.modules.freight.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.api.common.exception.BusinessException;
import com.easymall.api.common.api.ResultCode;
import com.easymall.api.entity.Address;
import com.easymall.api.entity.FreightConfig;
import com.easymall.api.entity.FreightRule;
import com.easymall.api.mapper.FreightConfigMapper;
import com.easymall.api.mapper.FreightRuleMapper;
import com.easymall.api.mapper.RegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运费计算（小程序侧，只读）。
 * 计费口径：
 * 1. 区域规则按「市规则 > 省规则」的优先级命中；
 * 2. 命中不配送规则 → 不允许下单；
 * 3. 命中配送规则 → 满额包邮门槛满足时包邮，否则收规则运费（0 即无条件包邮）；
 * 4. 未命中任何规则 → 按统一运费计费，全国满额包邮门槛满足时包邮。
 */
@Service
@RequiredArgsConstructor
public class FreightService {

    private static final int REGION_LEVEL_PROVINCE = 1;
    private static final int REGION_LEVEL_CITY = 2;

    /** 直辖市等地址里城市列的通用占位名，无法定位到具体市规则，按省级规则处理 */
    private static final List<String> GENERIC_CITY_NAMES = List.of("市辖区", "县", "省直辖县级行政区划");

    private final FreightConfigMapper freightConfigMapper;
    private final FreightRuleMapper freightRuleMapper;
    private final RegionMapper regionMapper;

    /** 运费计算结果 */
    public record FreightResult(BigDecimal freightAmount, boolean deliverable, String message) {
        public static FreightResult of(BigDecimal amount) {
            return new FreightResult(amount, true, null);
        }

        public static FreightResult undeliverable() {
            return new FreightResult(BigDecimal.ZERO, false, "该地区暂不提供配送");
        }
    }

    /**
     * 按收货地址与商品总额计算运费。地址缺失视为未命中规则，按统一运费处理。
     */
    public FreightResult calculate(Address address, BigDecimal totalAmount) {
        BigDecimal amount = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        if (address == null || !StringUtils.hasText(address.getProvince())) {
            return FreightResult.of(defaultFee(amount));
        }

        FreightRule rule = matchRule(address.getProvince(), address.getCity());
        if (rule != null) {
            if (!Integer.valueOf(1).equals(rule.getDeliverable())) {
                return FreightResult.undeliverable();
            }
            return FreightResult.of(ruleFee(rule, amount));
        }
        return FreightResult.of(defaultFee(amount));
    }

    /**
     * 下单时使用：不可配送直接抛业务异常，阻断下单。
     */
    public BigDecimal calculateOrThrow(Address address, BigDecimal totalAmount) {
        FreightResult result = calculate(address, totalAmount);
        if (!result.deliverable()) {
            throw new BusinessException(ResultCode.FREIGHT_NOT_DELIVERABLE);
        }
        return result.freightAmount();
    }

    /* ---------------- private ---------------- */

    /** 市规则优先，其次省规则 */
    private FreightRule matchRule(String provinceName, String cityName) {
        List<FreightRule> rules = freightRuleMapper.selectList(Wrappers.<FreightRule>lambdaQuery());
        if (rules.isEmpty()) {
            return null;
        }

        String provinceCode = regionMapper.selectProvinceCodeByName(provinceName);
        if (!StringUtils.hasText(provinceCode)) {
            return null;
        }

        if (StringUtils.hasText(cityName) && !GENERIC_CITY_NAMES.contains(cityName)) {
            String cityCode = regionMapper.selectCityCodeByNameAndParent(cityName, provinceCode);
            if (StringUtils.hasText(cityCode)) {
                FreightRule cityRule = findByCode(rules, cityCode);
                if (cityRule != null) {
                    return cityRule;
                }
            }
        }
        return findByCode(rules, provinceCode);
    }

    private FreightRule findByCode(List<FreightRule> rules, String regionCode) {
        return rules.stream()
                .filter(rule -> regionCode.equals(rule.getRegionCode()))
                .findFirst()
                .orElse(null);
    }

    /** 命中配送规则的运费：满额包邮门槛满足时包邮，否则收规则运费（0 即无条件包邮） */
    private BigDecimal ruleFee(FreightRule rule, BigDecimal totalAmount) {
        if (rule.getFreeAmount() != null && totalAmount.compareTo(rule.getFreeAmount()) >= 0) {
            return BigDecimal.ZERO;
        }
        return rule.getFee() == null ? BigDecimal.ZERO : rule.getFee();
    }

    /** 未命中规则的统一运费：全国满额包邮门槛满足时包邮 */
    private BigDecimal defaultFee(BigDecimal totalAmount) {
        FreightConfig config = freightConfigMapper.selectById(1L);
        if (config == null) {
            return BigDecimal.ZERO;
        }
        if (config.getFreeAmount() != null && totalAmount.compareTo(config.getFreeAmount()) >= 0) {
            return BigDecimal.ZERO;
        }
        return config.getDefaultFee() == null ? BigDecimal.ZERO : config.getDefaultFee();
    }
}
