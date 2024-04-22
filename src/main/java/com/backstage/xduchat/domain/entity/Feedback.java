package com.backstage.xduchat.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 反馈表
 * @TableName feedback
 */
@TableName(value ="feedback")
@Data
public class Feedback implements Serializable {
    /**
     * 反馈记录id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id，从统一身份认证平台获取
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     * 反馈类型，like点赞，dislike点踩，feedback反馈
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 反馈时间
     */
    @TableField(value = "time")
    private Date time;

    /**
     * 反馈内容，如果是赞/踩则空，反馈则有内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 逻辑删除，"0"未删除，"1"删除
     */
    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}