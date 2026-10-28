package com.easymall.admin.modules.marketing.support;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 优惠券对外随机码生成器。
 *
 * <p>对外标识不使用自增主键，避免通过 id=1、id=2 递增遍历所有券，
 * 暴露上架节奏与券总量。格式：{@code C} + {@code yyMMdd} + 10 位随机串，
 * 字符集剔除了易混淆的 I / O / 0 / 1，便于人工核对。</p>
 *
 * <p>10 位随机串的取值空间为 32^10 ≈ 1.1e15，配合库表唯一索引 uk_coupon_code，
 * 碰撞概率可忽略；仍保留查重重试作为兜底。</p>
 */
public final class CouponCodeGenerator {

    /** 去掉 I/O/0/1 的 32 个字符 */
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private static final int RANDOM_LENGTH = 10;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private static final SecureRandom RANDOM = new SecureRandom();

    private CouponCodeGenerator() {
    }

    public static String next() {
        StringBuilder builder = new StringBuilder(1 + 6 + RANDOM_LENGTH);
        builder.append('C').append(LocalDate.now().format(DATE_FORMAT));
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            builder.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}
