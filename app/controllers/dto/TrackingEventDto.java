package controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(Include.NON_NULL)
public class TrackingEventDto {
	public Integer type;
	public Integer subType;
	public JsonNode data;
	public Integer meetingId;
	public Integer trackingId;
	public Integer groupId;
	public Integer userId;
	public Integer userCategory;
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	public Integer getSubType() {
		return subType;
	}
	public void setSubType(Integer subType) {
		this.subType = subType;
	}	
	
	public JsonNode getData() {
		return data;
	}
	public void setData(JsonNode data) {
		this.data = data;
	}
	public Integer getMeetingId() {
		return meetingId;
	}
	public void setMeetingId(Integer meetingId) {
		this.meetingId = meetingId;
	}
	public Integer getGroupId() {
		return groupId;
	}
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	
	
	
}
