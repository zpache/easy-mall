package cn.easykits.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.easykits.api.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
