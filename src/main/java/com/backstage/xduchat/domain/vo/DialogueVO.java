package com.backstage.xduchat.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @Author: 711lxsky
 */
@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DialogueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(name = "id", value = "对话记录id", required = true)
    private String id;

    @ApiModelProperty(name = "uid", value = "用户id", required = true)
    private String uid;

    @ApiModelProperty(name = "datetime", value = "对话时间", required = true)
    private Date datetime;

    @ApiModelProperty(name = "record", value = "对话记录", required = true)
    private String record;

}
