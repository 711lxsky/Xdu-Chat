package com.backstage.xduchat.service;

import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.dto.DialogueDTO;
import com.backstage.xduchat.domain.entity.Dialogue;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author zyy
* @description 针对表【dialogues(对话记录表)】的数据库操作Service
* @createDate 2024-04-28 11:16:51
*/
public interface DialogueService extends IService<Dialogue> {

    Result<?> addDialogue(DialogueDTO dialogueDTO);

    Result<?> getAllDialogueForUser(String uid);

    Result<?> deleteDialogue(String dialogueId, String uid);
}
