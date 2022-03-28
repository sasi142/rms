package core.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import messages.ConnId;

import java.util.Map;
import java.util.UUID;

@JsonInclude(Include.NON_NULL)
public class Event {

    private final ConnId connId;
    private final String id;
    private Integer type;
    private Map<String, String> data;

    public Event(ConnId connId) {
        this.connId = connId;
        this.id = UUID.randomUUID().toString();
    }

    public Event() {
        this.connId = new ConnId(-1, "NA", "NA");
        this.id = UUID.randomUUID().toString();
    }

//    public Event(@JsonProperty("id") String id,
//                 @JsonProperty("type") Integer type,
//                 @JsonProperty("data") Map<String, String> data) {
//        this.id = id;
//        this.type = type;
//        this.data = data;
//    }

    public String getId() {
        return id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    public ConnId getConnId() {
        return connId;
    }

    @Override
    public String toString() {
        return "Event [id=" + ", type=" + type + ", data=" + data + "]";
    }
}
