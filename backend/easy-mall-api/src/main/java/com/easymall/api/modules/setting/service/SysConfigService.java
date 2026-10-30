package com.easymall.api.modules.setting.service;

import com.easymall.api.entity.SysConfig;
import com.easymall.api.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置读取（只读）。
 * 配置由管理端写库，小程序端读库；为避免每次请求打库，按 30 秒缓存整表，
 * 后台改完配置最迟 30 秒对小程序生效。
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private static final long TTL_MS = 30_000L;

    private final SysConfigMapper sysConfigMapper;

    private volatile Map<String, Map<String, String>> cache = Map.of();

    private volatile long loadedAt = 0L;

    /**
     * 读取某个分组的全部配置，缺失的键不出现在结果里。
     */
    public Map<String, String> group(String groupCode) {
        if (System.currentTimeMillis() - loadedAt > TTL_MS) {
            reload();
        }
        return cache.getOrDefault(groupCode, Map.of());
    }

    public String get(String groupCode, String configKey, String defaultValue) {
        String value = group(groupCode).get(configKey);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public boolean getBoolean(String groupCode, String configKey, boolean defaultValue) {
        String value = group(groupCode).get(configKey);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    public int getInt(String groupCode, String configKey, int defaultValue) {
        String value = group(groupCode).get(configKey);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /* ---------------- private ---------------- */

    private synchronized void reload() {
        long now = System.currentTimeMillis();
        if (now - loadedAt <= TTL_MS) {
            return;
        }
        List<SysConfig> list = sysConfigMapper.selectList(null);
        Map<String, Map<String, String>> grouped = new HashMap<>();
        for (SysConfig config : list) {
            Map<String, String> values = grouped.computeIfAbsent(config.getGroupCode(), key -> new HashMap<>());
            values.put(config.getConfigKey(), config.getConfigValue() == null ? "" : config.getConfigValue());
        }
        Map<String, Map<String, String>> snapshot = new HashMap<>(grouped.size());
        grouped.forEach((key, value) -> snapshot.put(key, Collections.unmodifiableMap(value)));
        cache = Collections.unmodifiableMap(snapshot);
        loadedAt = now;
    }
}
