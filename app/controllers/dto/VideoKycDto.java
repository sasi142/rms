package controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class VideoKycDto {
	private Integer groupId;
	private Integer priority;
	private Integer status;
	private Integer agentId;
	private Integer waitTimeInSec;
	public Integer getGroupId() {
		return groupId;
	}
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}
	public Integer getPriority() {
		return priority;
	}
	public void setPriority(Integer priority) {
		this.priority = priority;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public Integer getAgentId() {
		return agentId;
	}
	public void setAgentId(Integer agentId) {
		this.agentId = agentId;
	}
	public Integer getWaitTimeInSec() {
		return waitTimeInSec;
	}
	public void setWaitTimeInSec(Integer waitTimeInSec) {
		this.waitTimeInSec = waitTimeInSec;
	}
}
