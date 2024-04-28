package com.backstage.xduchat.Exception;

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


    public HttpException(String message) {
        this.message = message;
        this.httpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

}