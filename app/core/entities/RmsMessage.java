package core.entities;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.utils.Enums.RmsMessageType;
import messages.ConnId;

public class RmsMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	private final ConnId connId;
	private String jsonStr;
	private RmsMessageType rmsMessageType;	
	private String connectionId;
	private List<String> connectionIds;
	private String waguid;

	public RmsMessage(ConnId connId, JsonNode jsonNode, RmsMessageType rmsMessageType) {
		super();
		this.connId = connId;
		this.jsonStr = jsonNode.toString();
		this.rmsMessageType = rmsMessageType;
	}
	public RmsMessage(JsonNode jsonNode, RmsMessageType rmsMessageType) {
		super();
		this.connId = new ConnId(-1, "NA", "NA");
		this.jsonStr = jsonNode.toString();
		this.rmsMessageType = rmsMessageType;
	}
//	public RmsMessage(String jsonStr, RmsMessageType rmsMessageType) {
//		super();
//		this.jsonStr = jsonStr;
//		this.rmsMessageType = rmsMessageType;
//	}
//	public RmsMessage(JsonNode jsonNode, RmsMessageType rmsMessageType, List<String> connectionIds) {
//		super();
//		this.jsonStr = jsonNode.toString();
//		this.rmsMessageType = rmsMessageType;
//		this.connectionIds = connectionIds;
//	}
//	public RmsMessage(List<String> connectionIds, RmsMessageType rmsMessageType) {
//		super();
//		this.connectionIds = connectionIds;
//		this.rmsMessageType = rmsMessageType;
//	}

	public JsonNode getJsonNode() {
		//return jsonNode;
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode jsonNode = mapper.readTree(jsonStr);
			return jsonNode;
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void setJsonNode(JsonNode jsonNode) {
		if(jsonNode != null){
			this.jsonStr = jsonNode.toString();
		}
	}

	public RmsMessageType getRmsMessageType() {
		return rmsMessageType;
	}

	public void setRmsMessageType(RmsMessageType rmsMessageType) {
		this.rmsMessageType = rmsMessageType;
	}
	public String getJsonStr() {
		return jsonStr;
	}
	public void setJsonStr(String jsonStr) {
		this.jsonStr = jsonStr;
	}
	public List<String> getConnectionIds() {
		return connectionIds;
	}
	public void setConnectionIds(List<String> connectionIds) {
		this.connectionIds = connectionIds;
	}
	public String getConnectionId() {
		return connectionId;
	}
	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}
	public String getWaguid() {
		return waguid;
	}
	public void setWaguid(String waguid) {
		this.waguid = waguid;
	}

	public ConnId getConnId() {
		return connId;
	}
}
