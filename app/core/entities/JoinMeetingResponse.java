package core.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class JoinMeetingResponse {

	private String chimeAttendeeDetails;

	private Recording recording;

	private User user;

	public String getChimeAttendeeDetails() {
		return chimeAttendeeDetails;
	}

	public void setChimeAttendeeDetails(String chimeAttendeeDetails) {
		this.chimeAttendeeDetails = chimeAttendeeDetails;
	}

	public Recording getRecording() {
		return recording;
	}

	public void setRecording(Recording recording) {
		this.recording = recording;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public String toString() {
		return "JoinMeetingResponse{" +
				"chimeAttendeeDetails='" + chimeAttendeeDetails + '\'' +
				", recordingId=" + recording +
				", user=" + user +
				'}';
	}
}
