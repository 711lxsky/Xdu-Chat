package com.backstage.xduchat.Utils;

import com.backstage.xduchat.Exception.HttpException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * @Author: 711lxsky
 * @Description: JSON工具类
 */

@Getter
@Component
public class JsonUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String toJson(Object object) throws HttpException{
        try {
            return this.objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new HttpException(e.getMessage());
        }
    }

    public <T> T parseJsonToObject(String jsonStr, Class<T> clazz) throws HttpException{
        try {
            return this.objectMapper.readValue(jsonStr, clazz);
        }
        catch (JsonMappingException e) {
            throw new HttpException(e.getMessage());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode getJsonNode(String jsonStr) throws HttpException{
        try {
            return this.objectMapper.readTree(jsonStr);
        } catch (JsonProcessingException e) {
            throw new HttpException(e.getMessage());
        }
    }

    public <T> T parseObjectToClass(Object jsonNode, Class<T> clazz){
        return this.objectMapper.convertValue(jsonNode, clazz);
    }

    public String parseJsonNodeToString(JsonNode jsonNode) throws HttpException{
        try {
            return this.objectMapper.writeValueAsString(jsonNode);
        }
        catch (JsonProcessingException e){
            throw new HttpException(e.getMessage());
        }
    }

}
