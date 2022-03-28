package core.services;

import core.entities.MeetingEvent;

import java.util.List;

public interface EventTrackingService {
	void sendMeetingEvent(Integer meetingId, Integer userId, Integer groupId, Byte eventType, Byte eventSource, String data);
	void sendUserEvent(Integer userId, Integer groupId, Byte eventType, Byte eventSource, String data);
	void sendUserConnectionEvent(Integer userId, Integer groupId, Byte eventType, Byte eventSource, String data);
}
