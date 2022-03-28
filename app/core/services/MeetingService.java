package core.services;

import core.entities.Meeting;

public interface MeetingService {

	Integer createMeeting(Integer groupId, Boolean autoRecording, Integer currentUserId, Byte meetingType, Integer to, Byte meetingStatus);
	Integer createRecording(Integer groupId, Integer meetingId, Byte recordingType, Byte recordingMethod, Integer currentUserId, Integer to, Boolean alwaysCreateNewRec, Byte recordingStage);
	Meeting getMeetingDetails(Integer meetingId);
	void createMeetingAttendee(Integer meetingId, Integer currentUserId);
}
