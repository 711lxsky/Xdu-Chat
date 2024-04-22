package com.backstage.xduchat.Exception;

import com.backstage.xduchat.setting_enum.ExceptionConstant;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * @Author: 711lxsky
 */

@Getter
public class HttpException extends RuntimeException {

    /**
     * HTTP状态码
     */
    private int httpStatusCode;

    /**
     * 错误消息
     */
    private String message;

    public HttpException() {
        this.message = ExceptionConstant.InternalServerError.getMessage_EN();
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
    }


    public HttpException(String message) {
        this.message = message;
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    public HttpException(String message, int httpStatusCode) {
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }
}