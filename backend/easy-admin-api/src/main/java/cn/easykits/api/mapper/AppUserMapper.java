package cn.easykits.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.easykits.api.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}
