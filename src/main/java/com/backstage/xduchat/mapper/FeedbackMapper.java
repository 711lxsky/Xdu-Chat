package com.backstage.xduchat.mapper;

import com.backstage.xduchat.domain.entity.Feedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author zyy
* @description 针对表【feedback(反馈表)】的数据库操作Mapper
* @createDate 2024-04-22 09:13:08
* @Entity com.backstage.xduchat.domain.entity.Feedback
*/
@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

}




