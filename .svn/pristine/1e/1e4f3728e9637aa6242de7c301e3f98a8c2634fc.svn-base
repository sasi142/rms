package core.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class UserContext {
	private String	clientId;
	private Integer	versionId;
	private String	token;
	private String	requestId;
	private String apiKey;
	private User	user;
	private String clientIPAddress;
	private String groupShorturl;
	private String sessionId;
	private String waguid;

	public UserContext() {
	}

	public UserContext(String clientId, User user) {
		this.clientId = clientId;
		this.user = user;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public Integer getVersionId() {
		return versionId;
	}

	public void setVersionId(Integer versionId) {
		this.versionId = versionId;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public String toString() {
		return "UserContext [clientId=" + clientId + ",token=******, requestId=" + requestId + ", user=" + user + "]";
	}

	public String getClientIPAddress() {
		return clientIPAddress;
	}

	public void setClientIPAddress(String clientIPAddress) {
		this.clientIPAddress = clientIPAddress;
	}

	public String getGroupShorturl() {
		return groupShorturl;
	}

	public void setGroupShorturl(String groupShorturl) {
		this.groupShorturl = groupShorturl;
	}
	
	
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String userSessionId) {
		this.sessionId = userSessionId;
	}

	public String getWaguid() {
		return waguid;
	}

	public void setWaguid(String waguid) {
		this.waguid = waguid;
	}
}
