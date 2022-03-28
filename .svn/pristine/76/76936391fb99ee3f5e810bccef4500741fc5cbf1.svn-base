package core.entities;

import java.io.Serializable;
import java.util.List;
import core.utils.Enums.RmsMessageType;

public class PubSubChannelMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	private String json;	
	private RmsMessageType rmsMessageType;	
	private List<String> connectionIds;

	public PubSubChannelMessage(String json, RmsMessageType rmsMessageType, List<String> connectionIds) {
		this.json = json;
		this.rmsMessageType = rmsMessageType;
		this.connectionIds = connectionIds;
	}
	public PubSubChannelMessage(RmsMessageType rmsMessageType, List<String> connectionIds) {	
		this.rmsMessageType = rmsMessageType;
		this.connectionIds = connectionIds;
	}
	public PubSubChannelMessage() {
		
	}

	public String getJson() {
		return json;
	}
	public void setJson(String json) {
		this.json = json;
	}
	public RmsMessageType getRmsMessageType() {
		return rmsMessageType;
	}
	public void setRmsMessageType(RmsMessageType rmsMessageType) {
		this.rmsMessageType = rmsMessageType;
	}
	public List<String> getConnectionIds() {
		return connectionIds;
	}
	public void setConnectionIds(List<String> connectionIds) {
		this.connectionIds = connectionIds;
	}
}
