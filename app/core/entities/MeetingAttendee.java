package core.entities;

import javax.persistence.*;

@Entity
@Table(name = "meeting_attendee")
@NamedQueries({
		@NamedQuery(name = "MeetingAttendee.updateMeetingAttendeeStatus", query = "update MeetingAttendee ma set ma.attendeeStatus=:attendeeStatus where ma.MeetingId=:MeetingId and ma.userId=:userId and ma.active=true"),
    	@NamedQuery(name = "MeetingAttendee.GetMeetingAttendeeByMeetingIdUserId", query = "select ma from MeetingAttendee ma where ma.MeetingId=:MeetingId and ma.userId=:userId and ma.active=TRUE")})

public class MeetingAttendee extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	protected Integer Id;
	
	@Column(name="MeetingId")
	private Integer MeetingId;
	
	@Column(name="UserId")
	private Integer userId;
	
	@Column(name="AttendeeStatus")
	private Byte attendeeStatus;
	
	@Column(name="CreatedDate")
	private Long createdDate;

	@Column(name="UpdatedDate")
	private Long updatedDate;

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

	public Byte getAttendeeStatus() {
		return attendeeStatus;
	}

	public void setAttendeeStatus(Byte attendeeStatus) {
		this.attendeeStatus = attendeeStatus;
	}

	public Long getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}

	public Long getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(Long updatedDate) {
		this.updatedDate = updatedDate;
	}

}
