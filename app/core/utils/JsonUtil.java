package core.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();
    static{
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public JsonNode readTree(String json){
        try {
            return mapper.readTree(json);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to parse json for data %s", json));
        }
    }

    public <T> T read(String json, Class<T> tClass){
        try {
            return mapper.readValue(json, tClass);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to read as %s for data %s", tClass.getSimpleName(), json));
        }
    }

    public String write(Object data){
        try {
            return mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Failed to write for data %s", data));
        }
    }
}
