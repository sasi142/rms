package core.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

//@Entity
//@Table(name = "api_request")
public class ApiRequest extends BaseEntity {
	private static final long serialVersionUID = 1L;

	//@Id
	//@GeneratedValue(strategy = GenerationType.AUTO)
	protected Long Id;
	
	//@Column
	private String uri;
	
	//@Column
	private Integer userId;
	
	//@Column
	private String httpMethod;
	
	//@Column
	private Long createdDate;
	
	//@Column
	private Integer responseTime;
	
	//@Column
	private Short statusCode;
	
	//@Column
	private String clientId;
	
	//@Column
	private String requestId;
	
	//@Column
	private String clientIP;
	
	//@Column
	private String serverIP;
	
	private String componentName;
	
	public ApiRequest() {
		
	}
	
	public ApiRequest(String uri, Integer userId,
			String httpMethod, Long createdDate, Integer responseTime, Short statusCode, String clientId, String requestId, String clientIP, String serverIP) {
		super();
		this.uri = uri;
		this.userId = userId;
		this.httpMethod = httpMethod;
		this.createdDate = createdDate;
		this.responseTime = responseTime;
		this.statusCode = statusCode;
		this.clientId = clientId;
		this.requestId = requestId;
		this.clientIP = clientIP;
		this.serverIP = serverIP;
	}	
	public Long getId() {
		return Id;
	}
	public void setId(Long id) {
		Id = id;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public String getHttpMethod() {
		return httpMethod;
	}
	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}
	public Long getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}

	public Integer getResponseTime() {
		return responseTime;
	}
	public void setResponseTime(Integer responseTime) {
		this.responseTime = responseTime;
	}
	public Short getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(Short statusCode) {
		this.statusCode = statusCode;
	}
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getRequestId() {
		return requestId;
	}
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
	public String getClientIP() {
		return clientIP;
	}
	public void setClientIP(String clientIP) {
		this.clientIP = clientIP;
	}
	public String getServerIP() {
		return serverIP;
	}
	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}	
	public String getComponentName() {
		return componentName;
	}
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	@Override
	public String toString() {
		return "ApiRequest [Id=" + Id + ", uri=" + uri + ", userId=" + userId + ", httpMethod=" + httpMethod
				+ ", createdDate=" + createdDate + ", responseTime=" + responseTime + ", statusCode=" + statusCode
				+ ", clientId=" + clientId + ", requestId=" + requestId + ", clientIP=" + clientIP + ", serverIP="
				+ serverIP + ", componentName=" + componentName + "]";
	}
}
