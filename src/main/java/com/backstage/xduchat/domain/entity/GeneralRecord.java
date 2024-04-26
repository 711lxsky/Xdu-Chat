package com.backstage.xduchat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 普通记录表，存放走代理进行对话的所有消息，且只插入，不更新
 * @TableName general_record
 */
@TableName(value ="general_record")
@Data
public class GeneralRecord implements Serializable {
    /**
     * 记录id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id，从统一身份认证平台获取
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 记录id
     */
    @TableField(value = "record_id")
    private String recordId;

    /**
     * 记录保存时间
     */
    @TableField(value = "time")
    private Date time;

    /**
     * 记录内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 逻辑删除标志，"0"未删除，"1"删除
     */
    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public GeneralRecord(String userId, String recordId, Date time, String content) {
        this.userId = userId;
        this.recordId = recordId;
        this.time = time;
        this.content = content;
    }
}