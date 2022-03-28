/**
 * 
 */
package controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import core.entities.Attachment;
import core.entities.Memo;
import core.entities.MemoChatUser;
import core.entities.User;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.PropertyUtil;
import play.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Chandramohan.Murkute
 */
@JsonInclude(Include.NON_NULL)
public class MeetingDto {

	private Integer			groupId;
	private Integer			meetingId;
	private Integer			recordingId;
	private Boolean			autoRecording	= Boolean.FALSE;
	private Byte            meetingType;
	private Integer to;
	private Byte recordingMethod;
	private Byte recordingType;
	private Boolean alwaysCreateNewMeeting = Boolean.FALSE;
	private Boolean alwaysCreateNewRecording = Boolean.FALSE;


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

	public Integer getRecordingId() {
		return recordingId;
	}

	public void setRecordingId(Integer recordingId) {
		this.recordingId = recordingId;
	}

	public Boolean getAutoRecording() {
		return autoRecording;
	}

	public void setAutoRecording(Boolean autoRecording) {
		this.autoRecording = autoRecording;
	}

	public Byte getMeetingType() {
		return meetingType;
	}

	public void setMeetingType(Byte meetingType) {
		this.meetingType = meetingType;
	}

	public Integer getTo() {
		return to;
	}

	public void setTo(Integer to) {
		this.to = to;
	}

	public Byte getRecordingMethod() {
		return recordingMethod;
	}

	public void setRecordingMethod(Byte recordingMethod) {
		this.recordingMethod = recordingMethod;
	}

	public Byte getRecordingType() {
		return recordingType;
	}

	public void setRecordingType(Byte recordingType) {
		this.recordingType = recordingType;
	}

	public Boolean getAlwaysCreateNewMeeting() {
		return alwaysCreateNewMeeting;
	}

	public void setAlwaysCreateNewMeeting(Boolean alwaysCreateNewMeeting) {
		this.alwaysCreateNewMeeting = alwaysCreateNewMeeting;
	}

	public Boolean getAlwaysCreateNewRecording() {
		return alwaysCreateNewRecording;
	}

	public void setAlwaysCreateNewRecording(Boolean alwaysCreateNewRecording) {
		this.alwaysCreateNewRecording = alwaysCreateNewRecording;
	}		
	
}





