package com.backstage.xduchat.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: 711lxsky
 */

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "data-settings")
public class DataConfig {

    private String parameterUserid;

    private String parameterRecordId;

    private String parameterStream;

    private String divideLineMD;

    private String divideLineHTML;

    private String responseJsonFormatFirst = """
            {
                "id": "1",
                "object": "2",
                "created": "3",
                "model": "gpt-3.5-turbo",
                "system_fingerprint": "4",
                "choices": [
                    {
                        "index": 0,
                        "delta": {
                            "role": "assistant",
                            "content": ""
                        },
                        "logprobs": null,
                        "finish_reason": null
                    }
                ]
            }
            """;

    private String responseJsonFormatCommon = """
            {
                "id": "1",
                "object": "2",
                "created": "3",
                "model": "gpt-3.5-turbo",
                "system_fingerprint": "4",
                "choices": [
                    {
                        "index": 0,
                        "delta": {
                            "content": ""
                        },
                        "logprobs": null,
                        "finish_reason": null
                    }
                ]
            }
            """;
}
