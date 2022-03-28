package controllers;

import com.google.inject.Singleton;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.aspects.ValidatorAspect;
import controllers.dto.MeetingDto;
import core.entities.Meeting;
import core.exceptions.BadRequestException;
import core.services.MeetingService;
import core.services.RecordingService;
import core.utils.*;
import core.validator.Validator;
import play.mvc.Result;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.With;
import core.utils.Enums.videoCallType;
import utils.RmsApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import controllers.actions.UserAuthAction;
import org.springframework.context.ApplicationContext;

@Singleton
public class MeetingController extends Controller{
	final static Logger logger = LoggerFactory.getLogger(MeetingController.class);

	private final Validator validator;

	private final RecordingService recordingService;

	private final ValidatorAspect validatorAspect;

	private final AuthUtil authUtil;

	private final MeetingService meetingService;

	public MeetingController() {
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		validator = (Validator) ctx.getBean(Constants.VALIDATOR_SPRING_BEAN);
		validatorAspect = (ValidatorAspect) ctx.getBean(Constants.VALIDATOR_ASPECT_SPRING_BEAN);
		recordingService = (RecordingService) ctx.getBean(Constants.RECORDING_SERVICE_BEAN);
		meetingService = (MeetingService) ctx.getBean(Constants.MEETING_SERVICE_BEAN);
		authUtil = ctx.getBean(AuthUtil.class);
	}
	
	@With(UserAuthAction.class)
	public Result createMeeting(Request request) {
		MeetingDto meetingDto = Json.fromJson(request.body().asJson(), MeetingDto.class);
		//Integer currentUserId= 440;
		Integer currentUserId= ThreadContext.getUserContext().getUser().getId();
		if(meetingDto.getMeetingType() == null){
            meetingDto.setMeetingType(Enums.videoCallType.TwoWay.getId());
        }		
		videoCallType.getVideoCallType(meetingDto.getMeetingType());
        validatorAspect.validateCreateMeetingRecording(meetingDto.getGroupId(), currentUserId, meetingDto.getTo(), meetingDto.getAlwaysCreateNewMeeting());
        Integer meetingId = meetingService.createMeeting(meetingDto.getGroupId(), meetingDto.getAlwaysCreateNewMeeting(), currentUserId, meetingDto.getMeetingType(), meetingDto.getTo(), Enums.VideoCallStatus.Created.getId().byteValue());
		logger.info("meetingId: {}",meetingId);
        meetingDto.setMeetingId(meetingId);
		if(meetingDto.getAutoRecording()){
			validatorAspect.validateCreateRecording(meetingDto);
			Integer recordingId = meetingService.createRecording(meetingDto.getGroupId(), meetingId, meetingDto.getRecordingType(), meetingDto.getRecordingMethod(), currentUserId, meetingDto.getTo(),meetingDto.getAlwaysCreateNewRecording(), Enums.RecordingStage.Started.getId());
			meetingDto.setRecordingId(recordingId);
		}
		JsonNode result = Json.toJson(meetingDto);
		logger.info("video kyc call wait : "+result.toString());
		return ok(result);
	}



	@With(UserAuthAction.class)
	public Result createRecording(Request request) {
		MeetingDto meetingDto = Json.fromJson(request.body().asJson(), MeetingDto.class);
		Integer currentUserId= ThreadContext.getUserContext().getUser().getId();
	       Meeting meeting = meetingService.getMeetingDetails(meetingDto.getMeetingId());
	       if(meeting == null){
			throw new BadRequestException(Enums.ErrorCode.BadRequest,"Input meeting Id is not valid.");
           }
        validatorAspect.validateCreateMeetingRecording(meeting.getGroupId().intValue(), currentUserId, meetingDto.getTo(), meetingDto.getAlwaysCreateNewMeeting());
		validatorAspect.validateCreateRecording(meetingDto);
			Integer recordingId = meetingService.createRecording(meeting.getGroupId().intValue(), meetingDto.getMeetingId(), meetingDto.getRecordingType(), meetingDto.getRecordingMethod(), currentUserId, meetingDto.getTo(),meetingDto.getAlwaysCreateNewRecording(), Enums.RecordingStage.Started.getId() );
			meetingDto.setRecordingId(recordingId);
		JsonNode result = Json.toJson(meetingDto);
		logger.info("video kyc call wait : "+result.toString());
		return ok(result);
	}

}
