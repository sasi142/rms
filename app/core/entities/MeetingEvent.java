package core.entities;

import javax.persistence.*;

@Entity
@Table(name = "meeting_event")
@NamedQueries({
		@NamedQuery(name = "MeetingEvent.getMeetingEvents", query = "Select me from MeetingEvent me where me.MeetingId=:MeetingId AND me.active=true")
})
public class MeetingEvent extends BaseEntity {
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Integer Id;
	
	@Column
	private Integer MeetingId;
	
	@Column
	private Integer userId;
	
	@Column
	private Byte eventType;
	
	@Column
	private Byte eventSource;
	
	@Column
	private String data;
	
	@Column
	private Long CreatedDate;

	public Integer getId() {
		return Id;
	}

	public void setId(Integer id) {
		Id = id;
	}

	public Integer getMeetingId() {
		return MeetingId;
	}

	public void setMeetingId(Integer meetingId) {
		MeetingId = meetingId;
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

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Long getCreatedDate() {
		return CreatedDate;
	}

	public void setCreatedDate(Long createdDate) {
		CreatedDate = createdDate;
	}

	public Byte getEventSource() {
		return eventSource;
	}

	public void setEventSource(Byte eventSource) {
		this.eventSource = eventSource;
	}
}
