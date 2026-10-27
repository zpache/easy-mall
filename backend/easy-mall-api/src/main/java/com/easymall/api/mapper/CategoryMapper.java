package com.easymall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easymall.api.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
