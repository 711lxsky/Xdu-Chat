package com.backstage.xduchat.domain.dto;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Author: 711lxsky
 * @Description:
 */
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ParametersXDUCHAT implements Serializable {

    @Serial
    private static final long serialVersionUID = 4175708905491820331L;

    private List<MessageXDUCHAT> query;

    private List<String> params;

}
