package core.daos;

import core.entities.Meeting;
import core.entities.MeetingAttendee;
import core.entities.Recording;

import java.util.List;

public interface MeetingAttendeeDao extends JpaDao<MeetingAttendee>{
	void updateMeetingAttendeeStatus(Integer meetingId, Integer userId, Byte attendeeStatus);
	List<Recording> getMeetingAttendeeByMeetingIdUserId(Integer meetingId, Integer userId);
}
