package com.backstage.xduchat.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author: 711lxsky
 * @Description: 服务健康信息
 */

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("服务健康信息")
public class HealthInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "服务名称")
    private String serviceName;

    @ApiModelProperty(value = "服务运行时间, 单位ms")
    private long serviceTime;

}
