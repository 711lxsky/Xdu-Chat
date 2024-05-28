package com.backstage.xduchat.Utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BuildResponseJsonObject {

    @Resource
    private JsonUtil jsonUtil;

    public JsonNode buildResponseForSSE(String responseStr, String content){
        JsonNode jsonNode = jsonUtil.getJsonNode(responseStr);
        ArrayNode choices = (ArrayNode) jsonNode.get("choices");
        ObjectNode delta = (ObjectNode) choices.get(0).get("delta");
        delta.put("content", content);
        return jsonNode;
    }


    

}
