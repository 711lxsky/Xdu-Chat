package com.backstage.xduchat.service;

import com.backstage.xduchat.domain.entity.DialogueTime;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author YuleyoungZoe
* @description 针对表【dialogue_time(对话次数记录表，用以进行次数限制)】的数据库操作Service
* @createDate 2024-05-26 13:21:37
*/
public interface DialogueTimeService extends IService<DialogueTime> {

    DialogueTime getByUidAndDialogueId(String uid, String dialogueId);

    void insertOne(String uid, String dialogueId);

    void addTime(String userId, String recordId, int oldTime);

    String getInformationForDialogueTime(int curDialogueTime);

    void deleteOne(String uid, String dialogueId);
}
