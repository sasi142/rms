package core.entities;

import javax.persistence.*;

@Entity
@Table(name = "chime_meeting_event")
@NamedQueries({
		@NamedQuery(name = "ChimeMeetingEvent.getChimeMeetingEvents", query = "Select cme from ChimeMeetingEvent cme where cme.meetingId=:MeetingId AND cme.active=true")
})
public class ChimeMeetingEvent extends BaseEntity {
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Integer Id;

	@Column(name="EventId")
	private String eventId;
	
	@Column(name="MeetingId")
	private Integer meetingId;
	
	@Column(name="UserId")
	private Integer userId;
	
	@Column(name="EventType")
	private Byte eventType;
	
	@Column(name="EventSource")
	private Byte eventSource;
	
	@Column(name="Data")
	private String data;

	@Column(name="EventTime")
	private Long eventTime;
	
	@Column(name="CreatedDate")
	private Long CreatedDate;

	public Integer getId() {
		return Id;
	}

	public void setId(Integer id) {
		Id = id;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
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

	public Long getEventTime() {
		return eventTime;
	}

	public void setEventTime(Long eventTime) {
		this.eventTime = eventTime;
	}

	public Long getCreatedDate() {
		return CreatedDate;
	}

	public void setCreatedDate(Long createdDate) {
		CreatedDate = createdDate;
	}
}
