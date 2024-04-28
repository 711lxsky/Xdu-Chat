package com.backstage.xduchat.service.impl;

import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.dto.DialogueDTO;
import com.backstage.xduchat.domain.entity.Dialogue;
import com.backstage.xduchat.domain.vo.DialogueVO;
import com.backstage.xduchat.mapper.DialoguesMapper;
import com.backstage.xduchat.service.DialogueService;
import com.backstage.xduchat.setting_enum.ExceptionConstant;
import com.backstage.xduchat.setting_enum.ResultCodeAndMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
* @author zyy
* @description 针对表【dialogues(对话记录表)】的数据库操作Service实现
* @createDate 2024-04-28 11:16:51
*/
@Log4j2
@Service
public class DialogueServiceImpl extends ServiceImpl<DialoguesMapper, Dialogue>
    implements DialogueService {

    @Override
    public Result<?> addDialogue(DialogueDTO dialogueDTO) {
        if(Objects.isNull(dialogueDTO) ||
                ! StringUtils.hasText(dialogueDTO.getId()) ||
                ! StringUtils.hasText(dialogueDTO.getUid()) ||
                ! StringUtils.hasText(dialogueDTO.getRecord()) ||
                Objects.isNull(dialogueDTO.getDatetime())
        ){
            return Result.fail(ExceptionConstant.ParameterError.getMessage_ZH());
        }
        LambdaQueryWrapper<Dialogue> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(Dialogue::getDialogueId, dialogueDTO.getId())
                .eq(Dialogue::getUserId, dialogueDTO.getUid());
        Dialogue newDialogue = this.parseDialogueDTOToDialogue(dialogueDTO);
        if(this.saveOrUpdate(newDialogue, updateWrapper)){
            return Result.success(ResultCodeAndMessage.InsertSuccess.getZhDescription());
        }
        return Result.fail(ResultCodeAndMessage.InsertFail.getZhDescription());

    }

    @Override
    public Result<?> getAllDialogueForUser(String uid) {
        if(! StringUtils.hasText(uid)){
            return Result.fail(ExceptionConstant.ParameterError.getMessage_ZH());
        }
        LambdaQueryWrapper<Dialogue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dialogue::getUserId, uid);
        List<Dialogue> dialogues = this.list(wrapper);
        if(dialogues.isEmpty()){
            return Result.fail(ExceptionConstant.DataNotFound.getMessage_ZH());
        }
        List<DialogueVO> dialogueVOS = this.parseDialoguesToDialogueVOs(dialogues);
        return Result.success(ResultCodeAndMessage.QuerySuccess.getZhDescription(), dialogueVOS);
    }

    @Override
    public Result<?> deleteDialogue(String dialogueId, String uid) {
        if(! StringUtils.hasText(dialogueId) || ! StringUtils.hasText(uid)){
            return Result.fail(ExceptionConstant.ParameterError.getMessage_ZH());
        }
        LambdaQueryWrapper<Dialogue> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(Dialogue::getDialogueId, dialogueId)
                .eq(Dialogue::getUserId, uid);
        if(this.baseMapper.delete(deleteWrapper) == 1){
            return Result.success(ResultCodeAndMessage.DeleteSuccess.getZhDescription());
        }
        return Result.fail(ExceptionConstant.DataNotFound.getMessage_ZH());
    }

    private Dialogue parseDialogueDTOToDialogue(DialogueDTO dialogueDTO) {
        Dialogue dialogue = new Dialogue();
        dialogue.setUserId(dialogueDTO.getUid());
        dialogue.setDialogueId(dialogueDTO.getId());
        dialogue.setTime(dialogueDTO.getDatetime());
        dialogue.setContent(dialogueDTO.getRecord());
        log.info("dialogue: {}", dialogue);
        return dialogue;
    }

    private List<DialogueVO> parseDialoguesToDialogueVOs(List<Dialogue> dialogues) {
        List<DialogueVO> dialogueVOList = new ArrayList<>();
        for(Dialogue dialogue : dialogues){
            dialogueVOList.add(this.parseDialogueToDialogueVO(dialogue));
        }
        return dialogueVOList;
    }

    private DialogueVO parseDialogueToDialogueVO(Dialogue dialogue) {
        DialogueVO dialogueVO = new DialogueVO();
        dialogueVO.setId(dialogue.getDialogueId());
        dialogueVO.setUid(dialogue.getUserId());
        dialogueVO.setDatetime(dialogue.getTime());
        dialogueVO.setRecord(dialogue.getContent());
        return dialogueVO;
    }
}




