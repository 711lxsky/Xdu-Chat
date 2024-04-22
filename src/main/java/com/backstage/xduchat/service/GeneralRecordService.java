package com.backstage.xduchat.service;

import com.backstage.xduchat.domain.entity.GeneralRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author zyy
* @description 针对表【general_record(普通记录表，存放走代理进行对话的所有消息，且只插入，不更新)】的数据库操作Service
* @createDate 2024-04-22 09:13:38
*/
public interface GeneralRecordService extends IService<GeneralRecord> {

}
