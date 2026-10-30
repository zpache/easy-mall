package com.easymall.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 行政区域（region 表）只读查询。
 * region 表的审计字段名与 BaseEntity 不一致，这里直接用 SQL 查询返回 Map，不走实体。
 */
@Mapper
public interface RegionMapper {

    @Select("""
            SELECT area_code AS areaCode, name, level, parent_code AS parentCode
            FROM region
            WHERE deleted = 0 AND level = 1
            ORDER BY id
            """)
    List<Map<String, Object>> selectProvinces();

    @Select("""
            SELECT area_code AS areaCode, name, level, parent_code AS parentCode
            FROM region
            WHERE deleted = 0 AND level = 2
            ORDER BY id
            """)
    List<Map<String, Object>> selectCities();

    @Select("""
            SELECT area_code AS areaCode, name, level, parent_code AS parentCode
            FROM region
            WHERE deleted = 0 AND area_code = #{areaCode}
            LIMIT 1
            """)
    Map<String, Object> selectByAreaCode(String areaCode);
}
