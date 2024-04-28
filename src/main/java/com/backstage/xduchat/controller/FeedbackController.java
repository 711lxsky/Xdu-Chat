package com.backstage.xduchat.controller;

import com.backstage.xduchat.Exception.HttpException;
import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.domain.dto.FeedbackDTO;
import com.backstage.xduchat.service.FeedbackService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: 711lxsky
 */

@Api(tags = "意见反馈")
@RestController("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService){
        this.feedbackService = feedbackService;
    }

    @ApiOperation(value = "添加意见反馈")
    @PostMapping("/add")
    public Result<?> addFeedback(@RequestBody FeedbackDTO feedbackDTO){
        try {
            return feedbackService.addFeedback(feedbackDTO);
        }
        catch (HttpException e){
            return Result.fail(e);
        }
    }

}
