package core.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import core.entities.RmsMessage;
import core.utils.Enums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.daos.MeetingInfoDao;
import org.springframework.transaction.annotation.Transactional;
import core.entities.Meeting;
import core.entities.VideoSignallingMessage;
import core.utils.CommonUtil;
import core.utils.Enums.ChatType;
import core.utils.Enums.EventTrackingSource;
import core.utils.Enums.MeetingEventType;
import core.utils.Enums.UserEventType;
import core.utils.Enums.VideoCallStatus;
import core.utils.Enums.VideoSignallingType;
import core.utils.MeetingUtil;
import messages.UserConnection;

@Service
@Transactional(rollbackFor = { Exception.class })
public class VideoSignallingServiceImpl implements VideoSignallingService, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(VideoSignallingServiceImpl.class);

	@Autowired
	private UserConnectionService userConnectionService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private MeetingUtil meetingUtil;

	@Autowired
	private MeetingInfoDao meetingInfoDao; 

	private List<String> videoCallClientIds;

	@Autowired
	private EventTrackingService eventTrackingService;

	@Override
	public void handleMessage(UserConnection connection, VideoSignallingMessage videoSignallingMessage) {
		logger.info("in signalling video: "+videoSignallingMessage.getVideoSignallingType().byteValue()+", from:"+videoSignallingMessage.getFrom()+", to : "+videoSignallingMessage.getTo());
		logger.debug("video message : "+videoSignallingMessage.toString());		
		if (VideoSignallingType.Create.getId().equals(videoSignallingMessage.getVideoSignallingType().byteValue())) {
			logger.debug("create room ");
			videoSignallingMessage.setVideoCallStatus(VideoCallStatus.Created.getId());

			logger.debug("create meeting in db");
			Meeting meeting = meetingUtil.getMeetingDetails(videoSignallingMessage);
			//meeting.setUpdatedById(connection.getUserContext().getUser().getId());
			meeting.setUpdatedById(videoSignallingMessage.getFrom());
			//meetingInfoDao.create(meeting);
			Integer meetingId = createMeeting(meeting);
			videoSignallingMessage.setMeetingId(meetingId);
			logger.debug("created meeting with id: "+meetingId);

			ObjectMapper mapper = new ObjectMapper(); 
			JsonNode message = mapper.convertValue(videoSignallingMessage, JsonNode.class);

			//userConnectionService.sendMessageToActor(videoSignallingMessage.getFrom(), message, connection.getActorRef(), videoCallClientIds);

             //This message send to same socket
			RmsMessage rmsMessage = new RmsMessage(message, Enums.RmsMessageType.Out);
			connection.getActorRef().tell(rmsMessage, null);
			
			Integer groupId = null;
			if (meeting.getGroupId() != null) {
				groupId = meeting.getGroupId().intValue();
			}

			eventTrackingService.sendMeetingEvent(meetingId, connection.getUserContext().getUser().getId(), videoSignallingMessage.getGroupId(), MeetingEventType.Start.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);
			logger.debug("created room and sent message");
		}
		else if (VideoSignallingType.Join.getId().equals(videoSignallingMessage.getVideoSignallingType().byteValue())) {	
			logger.debug("join the room");
			videoSignallingMessage.setVideoCallStatus(VideoCallStatus.Joined.getId());
			//logger.debug("update status to join for meetingId:"+videoSignallingMessage.getMeetingId());
			//meetingInfoDao.updateMeetingStatus(videoSignallingMessage.getMeetingId(), videoSignallingMessage.getVideoCallStatus().byteValue(), videoSignallingMessage.getFrom());
			//meetingInfoDao.updateMeetingStatus(videoSignallingMessage.getMeetingId(), videoSignallingMessage.getVideoCallStatus().byteValue(), connection.getUserContext().getUser().getId());
			//logger.debug("updated status to join");

			ObjectMapper mapper = new ObjectMapper(); 

			JsonNode message = mapper.convertValue(videoSignallingMessage, JsonNode.class);			

			userConnectionService.sendMessageToActor(videoSignallingMessage.getFrom(), message, null, videoCallClientIds);			

			userConnectionService.sendMessageToActor(videoSignallingMessage.getTo(), message, null, videoCallClientIds);

			eventTrackingService.sendMeetingEvent(videoSignallingMessage.getMeetingId(), connection.getUserContext().getUser().getId(), videoSignallingMessage.getGroupId(), MeetingEventType.Join.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);

			logger.debug("room is joined");
		}
		else if (VideoSignallingType.NewParticipants.getId().equals(videoSignallingMessage.getVideoSignallingType().byteValue())) {
			logger.debug("send new participants");
			videoSignallingMessage.setVideoCallStatus(VideoCallStatus.NewParticipants.getId());

			//logger.debug("update status to NewParticipants for meetingId:"+videoSignallingMessage.getMeetingId());
			//meetingInfoDao.updateMeetingStatus(videoSignallingMessage.getMeetingId(), videoSignallingMessage.getVideoCallStatus().byteValue(), videoSignallingMessage.getFrom());
			//logger.debug("updated status to NewParticipants");

			ObjectMapper mapper = new ObjectMapper(); 
			JsonNode message = mapper.convertValue(videoSignallingMessage, JsonNode.class);			
			userConnectionService.sendMessageToActor(videoSignallingMessage.getFrom(), message, null, videoCallClientIds);
			userConnectionService.sendMessageToActor(videoSignallingMessage.getTo(), message, null, videoCallClientIds);

			eventTrackingService.sendMeetingEvent(videoSignallingMessage.getMeetingId(), connection.getUserContext().getUser().getId(), videoSignallingMessage.getGroupId(), MeetingEventType.NewParticipants.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);

			logger.debug("sent new participants");
		}
		else {
			logger.debug("send default event");
			ObjectMapper mapper = new ObjectMapper(); 
			JsonNode message = mapper.convertValue(videoSignallingMessage, JsonNode.class);			
			userConnectionService.sendMessageToActor(videoSignallingMessage.getTo(), message, null, videoCallClientIds);

			if (VideoSignallingType.Offser.getId().equals(videoSignallingMessage.getVideoSignallingType().byteValue())) {
				eventTrackingService.sendMeetingEvent(videoSignallingMessage.getMeetingId(), connection.getUserContext().getUser().getId(), videoSignallingMessage.getGroupId(), MeetingEventType.Offer.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);

			}
			else if (VideoSignallingType.Answer.getId().equals(videoSignallingMessage.getVideoSignallingType().byteValue())) {
				eventTrackingService.sendMeetingEvent(videoSignallingMessage.getMeetingId(), connection.getUserContext().getUser().getId(), videoSignallingMessage.getGroupId(), MeetingEventType.Answer.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);
			}
			else if (VideoSignallingType.IceCandidate.getId().equals(videoSignallingMessage.getVideoSignallingType().byteValue())) {
				eventTrackingService.sendMeetingEvent(videoSignallingMessage.getMeetingId(), connection.getUserContext().getUser().getId(), videoSignallingMessage.getGroupId(), MeetingEventType.IceCandidate.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);

			}
			else if (VideoSignallingType.Bye.getId().equals(videoSignallingMessage.getVideoSignallingType().byteValue())) {
				eventTrackingService.sendMeetingEvent(videoSignallingMessage.getMeetingId(), connection.getUserContext().getUser().getId(), videoSignallingMessage.getGroupId(), MeetingEventType.Bye.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);
			}

			logger.debug("sent default event");
		}
	}
	

	@Transactional
	public Integer createMeeting(Meeting meeting) {
		meetingInfoDao.create(meeting);
		return meeting.getId();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		videoCallClientIds = new ArrayList<>(commonUtil.getSupportedClientIdList());		
		logger.info("videoCallClientIds: "+videoCallClientIds.toString());
	}



}
