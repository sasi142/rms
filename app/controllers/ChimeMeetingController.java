package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import controllers.actions.UserAuthAction;
import controllers.dto.ChimeEventDto;
import controllers.dto.MeetingDto;
import core.entities.*;
import core.queue.QueueMessage;
import core.queue.SQSMessageService;
import core.services.EventTrackingService;
import core.services.MeetingService;
import core.services.chimeevents.ChimeEventTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.inject.Singleton;

import core.utils.Constants;
import org.springframework.util.StringUtils;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;
import utils.RmsApplicationContext;
import controllers.aspects.ValidatorAspect;
import core.utils.*;
import core.services.ChimeMeetingService;

import java.util.List;
import java.util.Map;

@Singleton
public class ChimeMeetingController  extends Controller { 
	final static Logger logger = LoggerFactory.getLogger(ChimeMeetingController.class);

    private final ValidatorAspect validatorAspect;
	
    private final ChimeMeetingService chimeMeetingService;

	private final ChimeEventTrackingService chimeEventTrackingService;

	private final MeetingService meetingService;

    private final SQSMessageService sqsMessageService;

	public ChimeMeetingController() {
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		validatorAspect = (ValidatorAspect) ctx.getBean(Constants.VALIDATOR_ASPECT_SPRING_BEAN);
		meetingService = (MeetingService) ctx.getBean(Constants.MEETING_SERVICE_BEAN);
		chimeMeetingService = (ChimeMeetingService) ctx.getBean(Constants.CHIME_MEETING_SERVICE_BEAN);
		chimeEventTrackingService = (ChimeEventTrackingService) ctx.getBean(Constants.CHIME_EVENT_TRACKING_SERVICE_BEAN);
		sqsMessageService = (SQSMessageService) ctx.getBean(Constants.SQS_SERVICE_BEAN);
	}

	@With(UserAuthAction.class)
	public Result createMeeting(Request request) {
		logger.info("createMeeting has been triggered.");
		MeetingDto meetingDto = Json.fromJson(request.body().asJson(), MeetingDto.class);
		Integer currentUserId= ThreadContext.getUserContext().getUser().getId();
		if(meetingDto.getMeetingType() == null){
			meetingDto.setMeetingType(Enums.videoCallType.TwoWay.getId());
		}
		//Validating MeetingType Input Values
		Enums.videoCallType.getVideoCallType(meetingDto.getMeetingType());
		//TODO: This validation has to be enabled. But getting error for "cacheService" in local and ned to check. Time being commented it
		//validatorAspect.validateCreateMeetingRecording(meetingDto.getGroupId(), currentUserId, meetingDto.getTo(), meetingDto.getAlwaysCreateNewMeeting());
    	String meeting = chimeMeetingService.createMeeting(meetingDto, currentUserId);
		logger.info("createMeeting api has been completed.Meeting id:"+meeting);
		return Results.ok(meeting);
	}

	@With(UserAuthAction.class)
	public Result endMeeting(Integer meetingId) {
		logger.info("endMeeting has been triggered with Meeting Id: "+meetingId);
        Integer currentUserId= ThreadContext.getUserContext().getUser().getId();
		Meeting endMeeting = chimeMeetingService.endMeeting(meetingId, currentUserId);
		logger.info("endMeeting Response :"+endMeeting);
		return Results.ok(Json.toJson(endMeeting));
	}

	@With(UserAuthAction.class)
	public Result joinMeeting(Request request,Integer meetingId) {
        User currentUser= ThreadContext.getUserContext().getUser();
		Boolean autoRecording = Boolean.valueOf(request.getQueryString("autoRecording"));
		logger.info("JoinMeeting has been triggered with Meeting Id: {}, userId: {}, autoRecordingEnabled: {} ",meetingId,currentUser.getId(),autoRecording);
		JoinMeetingResponse joinMeetingResponse = chimeMeetingService.joinMeeting(meetingId, currentUser,autoRecording );
		logger.info("joinMeetingResponse :"+joinMeetingResponse);
		return Results.ok(Json.toJson(joinMeetingResponse));
	}

	@With(UserAuthAction.class)
	public Result startRecording(Request request,Integer meetingId) {
		logger.info("startRecording with meetingId: "+meetingId);
		Integer currentUserId= ThreadContext.getUserContext().getUser().getId();

		Boolean alwaysCreateNewRecording;
		if(StringUtils.isEmpty(request.getQueryString("alwaysCreateNewRecording"))) {
			alwaysCreateNewRecording=true;
		} else {
			alwaysCreateNewRecording = Boolean.valueOf(request.getQueryString("alwaysCreateNewRecording"));
		}
		Recording recording = chimeMeetingService.startRecording(meetingId,currentUserId,alwaysCreateNewRecording);
		logger.info("startRecording response as recordingId : "+recording);
		return ok(Json.toJson(recording));
	}

	@With(UserAuthAction.class)
	public Result stopRecording(Integer recordingId) {
		logger.info("stopRecording has been triggered with Recording Id: {}",recordingId);
		Recording recording = chimeMeetingService.stopRecording(recordingId);
		logger.info("Response :"+recording);
		return Results.ok(Json.toJson(recording));
	}

	@With(UserAuthAction.class)
	public Result sendEvents(Request request) {
		logger.info("sendEvents json: " + request.body().asJson());
		ChimeEventDto event = Json.fromJson(request.body().asJson(), ChimeEventDto.class);
		ObjectNode node = null;
		if (event.getData() != null) {
			node = (ObjectNode) event.getData();
			node.put("meetingId",event.getMeetingId());
			node.put("source",event.getEventSource());
			node.put("eventType",event.getEventType());
			node.put("userId",ThreadContext.getUserContext().getUser().getId());
			node.put("timestamp", System.currentTimeMillis());
		}
		String messageId = sqsMessageService.sendMessage(event.getMeetingId().toString(),node.toString());
		logger.info("messageId :"+messageId);
		return Results.ok(messageId);
	}

	@With(UserAuthAction.class)
	public Result getEvents(Integer meetingId) {
		logger.info("getEvents Method triggered for meetingId: "+meetingId);
		List<ChimeMeetingEvent> chimeMeetingEvents = chimeEventTrackingService.getChimeMeetingEvents(meetingId);
		logger.info("Number of Events: "+chimeMeetingEvents.size());
		JsonNode result = Json.toJson(chimeMeetingEvents);
		return ok(result);
	}
}
