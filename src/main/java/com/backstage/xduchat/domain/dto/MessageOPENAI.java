package com.backstage.xduchat.domain.dto;

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
public class MessageOPENAI implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String role;

    private String content;
}
