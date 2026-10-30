package com.easymall.api.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 行政区域（region 表）只读查询，用于把收货地址的省市名称解析为区域编码。
 */
@Mapper
public interface RegionMapper {

    @Select("""
            SELECT area_code FROM region
            WHERE deleted = 0 AND level = 1 AND name = #{name}
            LIMIT 1
            """)
    String selectProvinceCodeByName(@Param("name") String name);

    @Select("""
            SELECT area_code FROM region
            WHERE deleted = 0 AND level = 2 AND name = #{name} AND parent_code = #{parentCode}
            LIMIT 1
            """)
    String selectCityCodeByNameAndParent(@Param("name") String name, @Param("parentCode") String parentCode);

    @Select("""
            SELECT area_code, name, level
            FROM region
            WHERE deleted = 0 AND parent_code = #{parentCode}
            ORDER BY id
            """)
    java.util.List<java.util.Map<String, Object>> selectChildren(@Param("parentCode") String parentCode);
}
