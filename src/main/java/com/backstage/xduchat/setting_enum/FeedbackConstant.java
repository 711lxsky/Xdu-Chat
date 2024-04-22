package com.backstage.xduchat.setting_enum;

import com.backstage.xduchat.Exception.HttpException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

/**
 * @Author: 711lxsky
 * @Description:
 */

@AllArgsConstructor
@Getter
public enum FeedbackConstant {

    Like(1, "like"),
    Dislike(2, "dislike"),
    Feedback(3, "feedback");

    private final Integer typeCode;

    private final String typeName;

    public static FeedbackConstant fromCodeToName(String typeName) throws HttpException{
        String lowerCaseTypeName = typeName.toLowerCase(Locale.ROOT);
        for( FeedbackConstant feedbackConstant : FeedbackConstant.values()){
            if(feedbackConstant.getTypeName().equals(lowerCaseTypeName)){
                return feedbackConstant;
            }
        }
        throw new HttpException(ExceptionConstant.PasswordError.getMessage_EN());
    }

}
