package controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(Include.NON_NULL)
public class ChimeEventDto {
	private Integer	groupId;
	private Integer meetingId;
	private String eventType;
	private String eventSource;
	private JsonNode data;

	public Integer getGroupId() {
		return groupId;
	}

	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	public Integer getMeetingId() {
		return meetingId;
	}

	public void setMeetingId(Integer meetingId) {
		this.meetingId = meetingId;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getEventSource() {
		return eventSource;
	}

	public void setEventSource(String eventSource) {
		this.eventSource = eventSource;
	}

	public JsonNode getData() {
		return data;
	}

	public void setData(JsonNode data) {
		this.data = data;
	}
}
