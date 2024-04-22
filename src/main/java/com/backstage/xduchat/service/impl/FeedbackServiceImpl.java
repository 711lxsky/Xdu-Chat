package com.backstage.xduchat.service.impl;

import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.dto.FeedbackDTO;
import com.backstage.xduchat.domain.entity.Feedback;
import com.backstage.xduchat.mapper.FeedbackMapper;
import com.backstage.xduchat.service.FeedbackService;
import com.backstage.xduchat.setting_enum.ExceptionConstant;
import com.backstage.xduchat.setting_enum.FeedbackConstant;
import com.backstage.xduchat.setting_enum.ResultCodeAndMessage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Objects;

/**
* @author zyy
* @description 针对表【feedback(反馈表)】的数据库操作Service实现
* @createDate 2024-04-22 09:13:08
*/
@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback>
    implements FeedbackService{

    @Override
    public Result<?> addFeedback(FeedbackDTO feedbackDTO) throws HttpException{
        // 数据判空
        if(Objects.isNull(feedbackDTO)
                || !StringUtils.hasText(feedbackDTO.getUid())
                || !StringUtils.hasText(feedbackDTO.getType())){
            throw new HttpException(ExceptionConstant.DataNull.getMessage_EN());
        }
        // 转换并校验
        Feedback feedback = parseFeedbackDTOToFeedback(feedbackDTO);
        if(this.save(feedback)){
            // 插入成功
            return Result.success(ResultCodeAndMessage.InsertSuccess.getDescription());
        }
        return Result.fail(ResultCodeAndMessage.Fail.getCode(), ResultCodeAndMessage.Fail.getDescription());
    }

    private Feedback parseFeedbackDTOToFeedback(FeedbackDTO feedbackDTO) throws HttpException{
        Feedback feedback = new Feedback();
        feedback.setUserId(feedbackDTO.getUid());
        FeedbackConstant feedbackConstant = FeedbackConstant.fromCodeToName(feedbackDTO.getType());
        feedback.setType(feedbackConstant.getTypeCode());
        if(StringUtils.pathEquals(feedbackConstant.getTypeName(), FeedbackConstant.Feedback.getTypeName())){
            // 这里拿到对应的反馈枚举
            if(! StringUtils.hasText(feedbackDTO.getContent())) {
                throw new HttpException(ExceptionConstant.DataNull.getMessage_EN());
            }
            feedback.setContent(feedbackDTO.getContent());
        }
        feedback.setTime(new Date(System.currentTimeMillis()));
        return feedback;
    }

}




