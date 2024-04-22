package com.backstage.xduchat.service.impl;

import com.backstage.xduchat.domain.entity.GeneralRecord;
import com.backstage.xduchat.mapper.GeneralRecordMapper;
import com.backstage.xduchat.service.GeneralRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* @author zyy
* @description 针对表【general_record(普通记录表，存放走代理进行对话的所有消息，且只插入，不更新)】的数据库操作Service实现
* @createDate 2024-04-22 09:13:38
*/
@Service
public class GeneralRecordServiceImpl extends ServiceImpl<GeneralRecordMapper, GeneralRecord>
    implements GeneralRecordService{



}




