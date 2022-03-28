package core.daos;

import java.util.Set;

import core.utils.Enums.VideoCallStatus;

public interface CacheVideoMeetingRoomDao {
	public VideoCallStatus createOrJoin(String meetingId, Integer userId);
	public void remove(String meetingId, Integer userId);
	public Set<String> getUsers(String meetingId);
}
