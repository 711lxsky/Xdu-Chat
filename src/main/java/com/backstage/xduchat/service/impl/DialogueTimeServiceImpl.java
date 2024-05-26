package com.backstage.xduchat.service.impl;

import com.backstage.xduchat.config.DataConfig;
import com.backstage.xduchat.config.ProxyConfig;
import com.backstage.xduchat.setting_enum.DialogueTimeConstant;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backstage.xduchat.domain.entity.DialogueTime;
import com.backstage.xduchat.service.DialogueTimeService;
import com.backstage.xduchat.mapper.DialogueTimeMapper;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
* @author YuleyoungZoe
* @description 针对表【dialogue_time(对话次数记录表，用以进行次数限制)】的数据库操作Service实现
* @createDate 2024-05-26 13:21:38
*/
@Service
public class DialogueTimeServiceImpl extends ServiceImpl<DialogueTimeMapper, DialogueTime>
    implements DialogueTimeService{

    private final DataConfig dataConfig;

    private final ProxyConfig proxyConfig;

    public DialogueTimeServiceImpl(DataConfig dataConfig, ProxyConfig proxyConfig) {
        this.dataConfig = dataConfig;
        this.proxyConfig = proxyConfig;
    }

    @Override
    public DialogueTime getByUidAndDialogueId(String uid, String dialogueId) {
        try {
            LambdaQueryWrapper<DialogueTime> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DialogueTime::getUid, uid)
                    .eq(DialogueTime::getDialogueId, dialogueId);
            return this.baseMapper.selectOne(queryWrapper);
        }catch (Exception e){
            DialogueTime dialogueTimeForException = new DialogueTime();
            dialogueTimeForException.setId(Long.valueOf(DialogueTimeConstant.FLAG_ID_NULL.getFlag()));
            return dialogueTimeForException;
        }
    }

    @Override
    public void insertOne(String uid, String dialogueId) {
        DialogueTime dialogueTime = new DialogueTime();
        dialogueTime.setUid(uid);
        dialogueTime.setDialogueId(dialogueId);
        dialogueTime.setTime(0);
        dialogueTime.setCreateTime(new Date(System.currentTimeMillis()));
        dialogueTime.setUpdateTime(new Date(System.currentTimeMillis()));
        this.baseMapper.insert(dialogueTime);
    }

    @Override
    public void addTime(String userId, String recordId, int oldTime) {
        UpdateWrapper<DialogueTime> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().eq(DialogueTime::getUid, userId)
                .eq(DialogueTime::getDialogueId, recordId)
                .set(DialogueTime::getUpdateTime, new Date(System.currentTimeMillis()))
                .set(DialogueTime::getTime, oldTime + 1);
        this.update(updateWrapper);
    }

    @Override
    public String getInformationForDialogueTime(int curDialogueTime) {
        System.out.println("次数限制 :" + proxyConfig.getDialogueTimeMax());
        return dataConfig.getDivideLineMD() + " 当前次数：" + curDialogueTime + 1 + "/" + proxyConfig.getDialogueTimeMax();
    }

    @Override
    public void deleteOne(String uid, String dialogueId) {
        LambdaQueryWrapper<DialogueTime> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DialogueTime::getUid, uid)
                .eq(DialogueTime::getDialogueId, dialogueId);
        this.baseMapper.delete(wrapper);
    }
}




