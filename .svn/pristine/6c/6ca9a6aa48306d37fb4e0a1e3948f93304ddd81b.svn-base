package core.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(Include.NON_NULL)
public class VideoSignallingMessage {
	private Integer type;
	private Integer subtype;
	private Integer from;
	private Integer to;
	private Integer groupId;
	private Integer meetingId;
	private Integer videoCallType;
	private Integer videoSignallingType;
	private JsonNode signallingData;
	private Integer videoCallStatus; 
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
	public Integer getFrom() {
		return from;
	}
	public void setFrom(Integer from) {
		this.from = from;
	}
	public Integer getTo() {
		return to;
	}
	public void setTo(Integer to) {
		this.to = to;
	}
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
	public Integer getVideoCallType() {
		return videoCallType;
	}
	public void setVideoCallType(Integer videoCallType) {
		this.videoCallType = videoCallType;
	}
	public Integer getVideoSignallingType() {
		return videoSignallingType;
	}
	public void setVideoSignallingType(Integer videoSignallingType) {
		this.videoSignallingType = videoSignallingType;
	}
	public JsonNode getSignallingData() {
		return signallingData;
	}
	public void setSignallingData(JsonNode signallingData) {
		this.signallingData = signallingData;
	}
	public Integer getVideoCallStatus() {
		return videoCallStatus;
	}
	public void setVideoCallStatus(Integer videoCallStatus) {
		this.videoCallStatus = videoCallStatus;
	}
	@Override
	public String toString() {
		return "VideoSignallingMessage [type=" + type + ", subtype=" + subtype + ", from=" + from + ", to=" + to
				+ ", groupId=" + groupId + ", meetingId=" + meetingId + ", videoCallType=" + videoCallType
				+ ", videoSignallingType=" + videoSignallingType + ", signallingData=" + signallingData
				+ ", videoCallStatus=" + videoCallStatus + "]";
	}
}
