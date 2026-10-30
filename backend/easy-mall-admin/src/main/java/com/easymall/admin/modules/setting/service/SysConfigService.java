package com.easymall.admin.modules.setting.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.easymall.admin.entity.SysConfig;
import com.easymall.admin.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置通用读写：按「分组 + 键」存放，值为字符串。
 * 商城设置下的各配置页（支付 / 小程序 / 交易）共用本服务。
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    /**
     * 读取一个分组下的全部配置（缺失的键不会出现在结果里，调用方用默认值兜底）。
     */
    public Map<String, String> group(String groupCode) {
        List<SysConfig> list = sysConfigMapper.selectList(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getGroupCode, groupCode));
        Map<String, String> map = new HashMap<>(list.size());
        for (SysConfig config : list) {
            map.put(config.getConfigKey(), config.getConfigValue() == null ? "" : config.getConfigValue());
        }
        return map;
    }

    public String get(String groupCode, String configKey, String defaultValue) {
        SysConfig config = sysConfigMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getGroupCode, groupCode)
                .eq(SysConfig::getConfigKey, configKey)
                .last("limit 1"));
        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }
        return config.getConfigValue();
    }

    /**
     * 按需写入：值未变化时不产生 update，避免无谓的版本号漂移。
     */
    @Transactional(rollbackFor = Exception.class)
    public void upsert(String groupCode, String configKey, String value) {
        SysConfig exists = sysConfigMapper.selectOne(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getGroupCode, groupCode)
                .eq(SysConfig::getConfigKey, configKey)
                .last("limit 1"));
        if (exists == null) {
            SysConfig config = new SysConfig();
            config.setGroupCode(groupCode);
            config.setConfigKey(configKey);
            config.setConfigValue(value);
            config.setValueType("string");
            config.setIsSecret(0);
            sysConfigMapper.insert(config);
            return;
        }
        String current = exists.getConfigValue() == null ? "" : exists.getConfigValue();
        if (current.equals(value == null ? "" : value)) {
            return;
        }
        exists.setConfigValue(value);
        sysConfigMapper.updateById(exists);
    }
}
