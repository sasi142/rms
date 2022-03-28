package core.entities;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(Include.NON_NULL)
public class IqMessage {
	private Integer type;
	private Integer subtype;
	private String cid;
	private String action;
	private Map<String, String> params;
	private JsonNode body;
	private Long mid;
	
	public IqMessage() {
		
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Integer getSubtype() {
		return subtype;
	}

	public void setSubtype(Integer subtype) {
		this.subtype = subtype;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public JsonNode getBody() {
		return body;
	}

	public void setBody(JsonNode body) {
		this.body = body;
	}
	public Long getMid() {
		return mid;
	}
	public void setMid(Long mid) {
		this.mid = mid;
	}
}
