package com.backstage.xduchat.service;

import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.dto.FeedbackDTO;
import com.backstage.xduchat.domain.entity.Feedback;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author zyy
* @description 针对表【feedback(反馈表)】的数据库操作Service
* @createDate 2024-04-22 09:13:08
*/
public interface FeedbackService extends IService<Feedback> {

    Result<?> addFeedback(FeedbackDTO feedbackDTO);
}
