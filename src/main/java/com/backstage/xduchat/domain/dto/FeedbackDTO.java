package com.backstage.xduchat.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: 711lxsky
 */
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("意见反馈")
public class FeedbackDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 417570890549182L;

    @ApiModelProperty(name = "user_id", value = "用户id, 从统一认证平台获取", required = true)
    private String uid;

    @ApiModelProperty(name = "type", value = "反馈类型", required = true)
    private String type;

    @ApiModelProperty(name = "record", value = "反馈所对应的记录", required = true)
    private String record;

    @ApiModelProperty(name = "content", value = "反馈内容", allowEmptyValue = true)
    private String content;

}
