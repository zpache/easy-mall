package cn.easykits.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.easykits.api.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}
