package com.backstage.xduchat.Handler;

import com.backstage.xduchat.domain.Result;
import com.backstage.xduchat.setting_enum.ExceptionConstant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        // 这里可以根据需要自定义错误消息
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(Result.fail(HttpStatus.BAD_REQUEST.value(), ExceptionConstant.ParametersFormatError.getMessage_ZH()));

    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingServletRequestPartException(MissingServletRequestParameterException ex) {
        // 这里可以根据需要自定义错误消息
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(Result.fail(HttpStatus.BAD_REQUEST.value(), ExceptionConstant.ParametersFormatError.getMessage_ZH()));

    }

    // 可以添加更多@ExceptionHandler来处理其他类型的异常
}
