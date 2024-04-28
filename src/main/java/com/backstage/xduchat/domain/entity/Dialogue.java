package com.backstage.xduchat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 对话记录表
 * @TableName dialogue
 */
@TableName(value ="dialogue")
@Data
public class Dialogue implements Serializable {
    /**
     * 表id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 对话id
     */
    @TableField(value = "dialogue_id")
    private String dialogueId;

    /**
     * 对话时间
     */
    @TableField(value = "time")
    private Date time;

    /**
     * 对话内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}