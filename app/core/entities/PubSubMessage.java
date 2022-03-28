package core.entities;

import play.libs.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

import core.utils.Enums.DataOperation;

public class PubSubMessage {
    private Integer userId;
    private DataOperation ops;
    private ConnectionInfo info;
    
	public PubSubMessage(Integer userId, DataOperation ops, ConnectionInfo info) {
		super();
		this.userId = userId;
		this.ops = ops;
		this.info = info;
	}
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	
	public DataOperation getOps() {
		return ops;
	}
	public void setOps(DataOperation ops) {
		this.ops = ops;
	}
	public ConnectionInfo getInfo() {
		return info;
	}
	public void setInfo(ConnectionInfo info) {
		this.info = info;
	}
    
	public ObjectNode getJson() {
		ObjectNode node = Json.newObject();
		node.put("userId", userId);
		node.put("data", info.getJson().toString());
		return node;
	}
}
