package core.entities;

import javax.annotation.processing.Generated;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@NamedQueries({
	@NamedQuery(name="updateMeetingStatus", query= "update Meeting m set m.startDate = "
			+ "case when m.startDate > 0 then m.startDate else :startDate end"
			+ ",m.endDate = "
			+ "case when m.endDate = 0 then :endDate else m.endDate end"
			+ ",m.meetingStatus=:meetingStatus,m.updatedById=:updatedById,m.updatedDate=:updatedDate where m.id=:meetingId"),	
	@NamedQuery(name="updateMeetingRating",query= "update Meeting m set m.meetingRating=:meetingRating where m.id=:meetingId"),
		@NamedQuery(name="updateChimeMeetingId",query= "update Meeting m set m.chimeMeetingId=:chimeMeetingId, m.updatedById=:updatedById, m.updatedDate=:updatedDate where m.id=:meetingId"),
		@NamedQuery(name="getMeetingDetails",query= "select m.id from Meeting m where m.groupId=:groupId ORDER BY m.id DESC"),
	@NamedQuery(name="updateMeetingBandwidthInfo",query= "update Meeting m set m.callerMinBandwidth=:callerMinBandwidth,m.callerMaxBandwidth=:callerMaxBandwidth,m.receiverMinBandwidth=:receiverMinBandwidth,m.receiverMaxBandwidth=:receiverMaxBandwidth where m.id=:meetingId and Active=true")																					 
})
@Entity
@Table(name="meeting_info")
public class Meeting extends BaseEntity {
	final static Logger logger = LoggerFactory.getLogger(Meeting.class);
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Integer id;
	
	@Column(name="MeetingType")
	private Byte meetingType;
	
	@Column(name="CallerId")	
	private Integer from;
	
	@Column(name="ReceiverId")
	private Integer to;
	
	@Column(name="GroupId")
	private Long groupId;
	
	@Column(name="MeetingStatus")
	private Byte meetingStatus;

	@Column(name="CreatedDate")
	private Long createdDate;
	
	@Column(name="StartDate")
	private Long startDate;
	
	@Column(name="EndDate")
	private Long endDate;
	
	@Column(name="UpdatedById")
	private Integer updatedById;
	
	@Column(name="UpdatedDate")
	private Long updatedDate;
	
	@Column(name="MeetingRating")
	private Byte meetingRating;
	
	@Column(name="LastPingDate")
	private Long lastPingDate;
	
	@Column(name="CallerMinBandwidth")
	private Short callerMinBandwidth;
	
	@Column(name="CallerMaxBandwidth")
	private Short callerMaxBandwidth;
	
	@Column(name="ReceiverMinBandwidth")
	private Short receiverMinBandwidth;
	
	@Column(name="ReceiverMaxBandwidth")
	private Short receiverMaxBandwidth;
	
	@Column(name="ChimeMeetingId")
	private String chimeMeetingId;
	
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public Byte getMeetingType() {
		return meetingType;
	}
	public void setMeetingType(Byte meetingType) {
		this.meetingType = meetingType;
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
	public Long getGroupId() {
		return groupId;
	}
	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}
	public Byte getMeetingStatus() {
		return meetingStatus;
	}
	public void setMeetingStatus(Byte meetingStatus) {
		this.meetingStatus = meetingStatus;
	}
	public Long getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Long createdDate) {
		this.createdDate = createdDate;
	}
	public Long getStartDate() {
		return startDate;
	}
	public void setStartDate(Long startDate) {
		this.startDate = startDate;
	}
	public Long getEndDate() {
		return endDate;
	}
	public void setEndDate(Long endDate) {
		this.endDate = endDate;
	}
	public Integer getUpdatedById() {
		return updatedById;
	}
	public void setUpdatedById(Integer updatedById) {
		this.updatedById = updatedById;
	}
	public Long getUpdatedDate() {
		return updatedDate;
	}
	public void setUpdatedDate(Long updatedDate) {
		this.updatedDate = updatedDate;
	}
	public Byte getMeetingRating() {
		return meetingRating;
	}
	public void setMeetingRating(Byte meetingRating) {
		this.meetingRating = meetingRating;
	}
	public Long getLastPingDate() {
		return lastPingDate;
	}
	public void setLastPingDate(Long lastPingDate) {
		this.lastPingDate = lastPingDate;
	}
	
	public Short getCallerMinBandwidth() {
		return callerMinBandwidth;
	}
	public void setCallerMinBandwidth(Short callerMinBandwidth) {
		this.callerMinBandwidth = callerMinBandwidth;
	}
	public Short getCallerMaxBandwidth() {
		return callerMaxBandwidth;
	}
	public void setCallerMaxBandwidth(Short callerMaxBandwidth) {
		this.callerMaxBandwidth = callerMaxBandwidth;
	}
	public Short getReceiverMinBandwidth() {
		return receiverMinBandwidth;
	}
	public void setReceiverMinBandwidth(Short receiverMinBandwidth) {
		this.receiverMinBandwidth = receiverMinBandwidth;
	}
	public Short getReceiverMaxBandwidth() {
		return receiverMaxBandwidth;
	}
	public void setReceiverMaxBandwidth(Short receiverMaxBandwidth) {
		this.receiverMaxBandwidth = receiverMaxBandwidth;
	}
	public String getChimeMeetingId() {
	    return chimeMeetingId;
	}
	public void setChimeMeetingId(String chimeMeetingId) {
	    this.chimeMeetingId=chimeMeetingId;
	}
	@Override
	public String toString() {
		return "Meeting [id=" + id + ", meetingType=" + meetingType + ", from=" + from + ", to=" + to + ", groupId="
				+ groupId + ", meetingStatus=" + meetingStatus + ", createdDate=" + createdDate + ", startDate="
				+ startDate + ", endDate=" + endDate + ", updatedById=" + updatedById + ", updatedDate=" + updatedDate
				+ ", meetingRating=" + meetingRating + ", lastPingDate=" + lastPingDate + "]";
	}
		
}
