package com.easymall.admin.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 管理后台的时间统一格式：{@code yyyy-MM-dd HH:mm:ss}。
 * <p>
 * Jackson 默认把 LocalDateTime 序列化成 {@code 2026-10-30T09:03:51}（带 T），
 * 管理后台所有列表、详情都直接展示这个字符串，运营看着别扭，也不符合国内习惯。
 * 这里统一在序列化层转换，前端不需要在每个表格列上写格式化函数。
 * <p>
 * 反序列化保持宽松：带 T 的 ISO、空格分隔、只到分钟、只有日期都能解析，
 * 这样历史前端代码（DatePicker 的 valueFormat 是 ISO）不会被这次改动打断。
 * <p>
 * 注意：只作用于 easy-mall-admin，小程序端（easy-mall-api）的输出格式保持不变，
 * 避免 iOS 小程序里 {@code new Date('2026-10-30 09:03:51')} 解析失败。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Bean
    public JsonMapperBuilderCustomizer easyMallDateTimeCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("easyMallDateTime");
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME));
            module.addSerializer(LocalDate.class, new LocalDateSerializer(DATE));
            module.addSerializer(LocalTime.class, new LocalTimeSerializer(TIME));
            module.addDeserializer(LocalDateTime.class, new LenientLocalDateTimeDeserializer());
            builder.addModule(module);
        };
    }

    /**
     * 宽松解析：兼容 {@code 2026-10-30T09:03:51} 与 {@code 2026-10-30 09:03:51} 两种写法，
     * 只给日期时按当天零点处理（营销活动表单里的结束时间常这么传）。
     */
    static final class LenientLocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
            String raw = p.getValueAsString();
            if (raw == null || raw.isBlank()) {
                return null;
            }
            // 数字时间戳（毫秒）也兼容，避免个别调用方直接传时间戳
            if (p.currentToken() != null && p.currentToken().isNumeric()) {
                return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(p.getLongValue()),
                        java.time.ZoneId.systemDefault());
            }
            String value = raw.trim().replace('T', ' ');
            try {
                if (value.length() <= 10) {
                    return LocalDate.parse(value, DATE).atStartOfDay();
                }
                if (value.length() <= 16) {
                    return LocalDateTime.parse(value, DATE_TIME_MINUTE);
                }
                // 带毫秒/时区后缀的，截到秒再解析
                return LocalDateTime.parse(value.substring(0, 19), DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss：" + raw, e);
            }
        }
    }
}
