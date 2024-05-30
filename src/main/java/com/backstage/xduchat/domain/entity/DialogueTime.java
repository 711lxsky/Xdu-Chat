package com.backstage.xduchat.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 对话次数记录表，用以进行次数限制
 * @TableName dialogue_time
 */
@TableName(value ="dialogue_time")
@Data
public class DialogueTime implements Serializable {
    /**
     * 数据表检索id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @TableField(value = "uid")
    private String uid;

    /**
     * 对话id
     */
    @TableField(value = "dialogue_id")
    private String dialogueId;

    /**
     * 对话次数，用以记录且限制
     */
    @TableField(value = "time")
    private Integer time;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 逻辑删除标志， 1 删除， 0 未删
     */
    @TableField(value = "deleted")
    private Integer deleted;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}