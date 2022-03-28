package core.services;

import controllers.dto.MeetingDto;
import core.entities.JoinMeetingResponse;
import core.entities.Meeting;
import core.entities.Recording;
import core.entities.User;

import java.util.Map;

public interface ChimeMeetingService {
    String createMeeting(MeetingDto meetingDto, Integer currentUserID);
    Meeting endMeeting(Integer meetingId, Integer currentUserID);
    JoinMeetingResponse joinMeeting(Integer meetingId, User currentUser, Boolean autoRecording);
    Recording startRecording(Integer meetingId, Integer currentUserID, Boolean alwaysCreateNewRecording);
    Recording stopRecording(Integer recordingId);
}