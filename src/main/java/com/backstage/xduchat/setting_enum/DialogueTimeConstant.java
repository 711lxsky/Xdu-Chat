package com.backstage.xduchat.setting_enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DialogueTimeConstant {

    FLAG_ID_NULL("-1", "将字段 ID 标记为此值以表示数据库操作出现问题"),
    DEFAULT_TIME("0", "默认对话次数"),
    TIME_ERROR_FLAG("-1", "对话次数错误");

    private final String flag;

    private final String description;;

}
