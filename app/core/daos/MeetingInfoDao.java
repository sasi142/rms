package core.daos;

import core.entities.Meeting;

public interface MeetingInfoDao extends JpaDao<Meeting>{

	public void updateMeetingStatus(Integer meetingId, Byte meetingStatus, Integer updatedById);
	public void updateMeetingRating(Integer meetingId, Byte meetingRating);
	public void updateChimeMeetingId(Integer meetingId, String chimeMeetingId, Integer updatedById);
	public void updateMeetingBandwidthInfo(Integer meetingId, Short callerMaxBandwidth, Short callerMinBandwidth, Short receiverMaxBandwidth, Short receiverMinBandwidth);
	public Integer getExistingMeetingId(Long groupId);
}
