package core.entities;

import messages.UserConnection;
import play.libs.Json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonInclude(Include.NON_NULL)
public class ConnectionInfo {
	private String			uuid;
	private String			clientId;
	private String			ip;
	private String			instanceId;	
	private Integer			status	= -1;
	private Integer			type	= -1;
	private Long			time	= 0L;
	private long			pingTime;
	private User			user;
	private Integer			userId;
	private byte			mapOpen = 0;
	
	@JsonIgnore
	private UserConnection	connection;

	public ConnectionInfo() {

	}

	public ConnectionInfo(UserConnection connection, Long pingTime) {
		this.uuid = connection.getUuid();
		this.userId = connection.getUserContext().getUser().getId();
		this.connection = connection;
		this.pingTime = pingTime;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}
    
	public long getPingTime() {
		return pingTime;
	}

	public void setPingTime(long pingTime) {
		this.pingTime = pingTime;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public UserConnection getConnection() {
		return connection;
	}

	public void setConnection(UserConnection connection) {
		this.connection = connection;
	}

	public byte getMapOpen() {
		return mapOpen;
	}

	public void setMapOpen(byte mapOpen) {
		this.mapOpen = mapOpen;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public ObjectNode getJson() {
		ObjectNode node = Json.newObject();
		node.put("uuid", uuid);
		node.put("cid", clientId);
		node.put("ip", ip);
		node.put("instanceId", instanceId);	
		node.put("status", status);
		node.put("type", type);
		node.put("time", time);
		node.put("pingTime", pingTime);
		return node;
	}

	public ConnectionInfo(String conStr) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(conStr);
		this.uuid = json.findPath("uuid").asText();
		this.clientId = json.findPath("cid").asText();
		this.ip = json.findPath("ip").asText();
		this.instanceId = json.findPath("instanceId").asText();		
		this.status = json.findPath("status").asInt();
		this.type = json.findPath("type").asInt();
		this.time = json.findPath("time").asLong();
		this.pingTime = json.findPath("pingTime").asLong();
	}

	public ConnectionInfo(JsonNode json) {
		this.uuid = json.findPath("uuid").asText();
		this.clientId = json.findPath("cid").asText();
		this.ip = json.findPath("ip").asText();
		this.instanceId = json.findPath("instanceId").asText();
		//this.port = json.findPath("port").asInt();
		this.status = json.findPath("status").asInt();
		this.type = json.findPath("type").asInt();
		this.time = json.findPath("time").asLong();
		this.pingTime = json.findPath("pingTime").asLong();
	}
}
