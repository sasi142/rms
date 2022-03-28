package core.entities;

public class UserInteractionEvent {
	private static final long serialVersionUID = 1L;	
	protected Integer Id;
	private String trackingId;
	private Integer meetingId;
	private Integer userId;	
	private Integer groupId;	
	private Byte type;	
	private Byte eventType;	
	private Byte eventSource;
	private String data;	
	private Long createdDate;
	public Integer getId() {
		return Id;
	}
	public void setId(Integer id) {
		Id = id;
	}
	public String getTrackingId() {
		return trackingId;
	}
	public void setTrackingId(String trackingId) {
		this.trackingId = trackingId;
	}
	public Integer getMeetingId() {
		return meetingId;
	}
	public void setMeetingId(Integer meetingId) {
		this.meetingId = meetingId;
	}
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public Integer getGroupId() {
		return groupId;
	}
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}
	public Byte getType() {
		return type;
	}
	public void setType(Byte type) {
		this.type = type;
	}
	public Byte getEventType() {
		return eventType;
	}
	public void setEventType(Byte eventType) {
		this.eventType = eventType;
	}
	public Byte getEventSource() {
		return eventSource;
	}
	public void setEventSource(Byte eventSource) {
		this.eventSource = eventSource;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public Long getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}
	@Override
	public String toString() {
		return "UserInteractionEvent [Id=" + Id + ", trackingId=" + trackingId + ", meetingId=" + meetingId
				+ ", userId=" + userId + ", groupId=" + groupId + ", type=" + type + ", eventType=" + eventType
				+ ", eventSource=" + eventSource + ", data=" + data + ", createdDate=" + createdDate + "]";
	}
	
}
