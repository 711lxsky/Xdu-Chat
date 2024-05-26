package com.backstage.xduchat.mapper;

import com.backstage.xduchat.domain.entity.DialogueTime;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author YuleyoungZoe
* @description 针对表【dialogue_time(对话次数记录表，用以进行次数限制)】的数据库操作Mapper
* @createDate 2024-05-26 13:21:38
* @Entity com.backstage.xduchat.domain.entity.DialogueTime
*/
@Mapper
public interface DialogueTimeMapper extends BaseMapper<DialogueTime> {

}




