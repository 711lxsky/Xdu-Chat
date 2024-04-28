package com.backstage.xduchat.mapper;

import com.backstage.xduchat.domain.entity.Dialogue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author zyy
* @description 针对表【dialogues(对话记录表)】的数据库操作Mapper
* @createDate 2024-04-28 11:16:51
* @Entity com.backstage.xduchat.domain.entity.Dialogue
*/

@Mapper
public interface DialoguesMapper extends BaseMapper<Dialogue> {

}




