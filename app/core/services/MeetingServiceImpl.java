package core.services;

import core.daos.MeetingAttendeeDao;
import core.daos.MeetingInfoDao;
import core.entities.*;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums;
import core.utils.Enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingServiceImpl implements MeetingService {
	final static Logger logger = LoggerFactory.getLogger(MeetingServiceImpl.class);

	@Autowired
	private Environment env;

	@Autowired
	private MeetingInfoDao meetingInfoDao;

	@Autowired
	private CacheService cacheService;

	@Autowired
	private RecordingService recordingService;

	@Autowired
	private MeetingAttendeeDao meetingAttendeeDao;

    @Transactional
	@Override
		public Integer createMeeting(Integer groupId, Boolean alwaysCreateNewMeeting, Integer currentUserId, Byte meetingType, Integer to, Byte meetingStatus) {
    	 Integer meetingId= null;
    	if(!alwaysCreateNewMeeting) {
    	 	 meetingId = meetingInfoDao.getExistingMeetingId(groupId.longValue());
    	}   
    	if(meetingId != null){
    		return meetingId;
		} else {
			Meeting meeting = getMeeting(groupId, currentUserId, meetingType, to, meetingStatus);
			meetingInfoDao.create(meeting);
			return meeting.getId();
		}
		}

	@Transactional
	@Override
	public Integer createRecording(Integer groupId, Integer meetingId, Byte recordingType, Byte recordingMethod, Integer currentUserId, Integer to, Boolean alwaysCreateNewRec,Byte recordingStage){
		Integer	recordingId = null;
		if(!alwaysCreateNewRec) {
			Recording	recording= recordingService.getRecordingByMeetingId(meetingId);
			return recording.getId();
		}
		if(recordingId == null) {
			Recording recording = new Recording(currentUserId, to, System.currentTimeMillis(), recordingType.byteValue(),
					recordingStage, groupId, meetingId, (byte)1, recordingMethod);
			recording = recordingService.createVideoRecording(recording);
			return recording.getId();
		}
		return recordingId;
	}

	@Transactional
	@Override
	public void createMeetingAttendee(Integer meetingId, Integer userId){
		logger.info("Create Meeting Attendee for the MeetingId: "+meetingId+", UserId: "+userId);
		MeetingAttendee meetingAttendee = getMeetingAttendee(meetingId,userId);
		meetingAttendeeDao.create(meetingAttendee);
		logger.info("Create Meeting Attendee has been completed");
	}

	@Override
	public Meeting getMeetingDetails(Integer meetingId) {
		logger.info("get meeting details start: {}",meetingId);
		try {
           Meeting meeting = meetingInfoDao.findOne(meetingId);
			return meeting;
		}  catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to get meeting "+ e);
		}

	}

	private Meeting getMeeting(Integer groupId, Integer currentUserId, Byte meetingType, Integer to, Byte meetingStatus) {
		Meeting meeting = new Meeting();
		meeting.setActive(true);
		meeting.setMeetingRating((byte)0);
		meeting.setStartDate(0L);
		meeting.setEndDate(0L);
		meeting.setCreatedDate(System.currentTimeMillis());
		meeting.setFrom(currentUserId);
		//getGuestUserId from group Cache
		meeting.setTo(to);
		meeting.setLastPingDate(null);
		if(null != groupId) {
			meeting.setGroupId(groupId.longValue());
		}
		meeting.setMeetingStatus(meetingStatus);
		meeting.setMeetingType(meetingType);
		meeting.setUpdatedDate(System.currentTimeMillis());
		meeting.setUpdatedById(currentUserId);
		return meeting;
	}
private MeetingAttendee getMeetingAttendee(Integer meetingId, Integer userId) {
		MeetingAttendee meetingAttendee = new MeetingAttendee();
		meetingAttendee.setActive(true);
		meetingAttendee.setMeetingId(meetingId);
		meetingAttendee.setUserId(userId);
		meetingAttendee.setAttendeeStatus(Enums.MeetingAttendeeStatus.Joining.getId().byteValue());
		meetingAttendee.setCreatedDate(System.currentTimeMillis());
		meetingAttendee.setUpdatedDate(System.currentTimeMillis());
		return meetingAttendee;
	}


}


