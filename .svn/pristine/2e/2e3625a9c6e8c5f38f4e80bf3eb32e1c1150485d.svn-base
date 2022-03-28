package controllers;

import com.google.inject.Singleton;

import controllers.actions.ApiAuthAction;
import controllers.actions.PublicApiAction;
import controllers.aspects.ValidatorAspect;
import controllers.dto.TrackingEventDto;
import core.akka.actors.RmsActorSystem;
import core.entities.Event;
import core.exceptions.BadRequestException;
import core.services.EventService;
import core.services.EventTrackingService;
import core.services.RecordingService;
import core.utils.AuthUtil;
import core.utils.Constants;
import core.utils.Enums.*;
import core.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.With;
import utils.RmsApplicationContext;

import java.io.IOException;
import java.util.UUID;


@Singleton
public class EventController extends Controller {
    final static Logger logger = LoggerFactory.getLogger(EventController.class);

    private final Validator validator;

    private final EventTrackingService eventTrackingService;

    private final EventService eventService;

    private final RecordingService recordingService;

    private final ValidatorAspect validatorAspect;

    private final AuthUtil authUtil;


    public EventController() {
        ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
        validator = (Validator) ctx.getBean(Constants.VALIDATOR_SPRING_BEAN);
        validatorAspect = (ValidatorAspect) ctx.getBean(Constants.VALIDATOR_ASPECT_SPRING_BEAN);
        eventTrackingService = (EventTrackingService) ctx.getBean(Constants.EVENT_TRACKING_SERVICE_BEAN);
        eventService = (EventService) ctx.getBean(Constants.EVENT_SERVICE_SPRING_BEAN);
        recordingService = (RecordingService) ctx.getBean(Constants.RECORDING_SERVICE_BEAN);
        authUtil = ctx.getBean(AuthUtil.class);
    }

    @With(ApiAuthAction.class)
    public Result processEvents(Request request) throws IOException {
        Event event = Json.fromJson(request.body().asJson(), Event.class);
        validator.validate(event);
        logger.debug("handle event " + event.toString());
        EventType eventType = EventType.getEventTypeById(event.getType());
        if (eventType == null) {
            return badRequest();
        }
        switch (eventType) {
            //OWB Events
            case ProcessedVideoRecording:
                recordingService.handleProcessedVideoRecordingEvent(event);
                break;
            case FailedVideoRecording:
                recordingService.handleFailedVideoRecordingEvent(event);
                break;
            //Events sent by IMS
            case ContactAdd:
            case ChatGuestAdded:
            case ContactRemove:
            case DeleteUser:
            case LockUser:
            case ResetPassword:
            case UserUpdate:
            case ContactAddOrg:
            case CloseConnection:
            case AddGroup:
            case UpdateGroup:
            case UpdateGroupBySystem:
            case CloseGroup:
            case UpdateUseCaseStatus:
            case LeaveGroup:
            case ExitGroup:
            case Logout:
            case DeviceUpdated:
            case GuestChatDeviceInfoUpdate:
            case SessionExpired:
            case CreateVideoKyc:
            case UploadAttachment:
            case UpdateVideoKycStatus:
            case UpdateUserRole:
            case ChatCustomerForwardGuestAdded:
                eventService.handleEvent(event);
                break;
            default:
                RmsActorSystem.getEventRouterActorRef().tell(event, null);
                break;
        }
        return created();
    }

    @With(PublicApiAction.class)
    public Result sendTrackingEvents(Request request) {
        String uuid = UUID.randomUUID().toString();
        logger.info("uuid: " + uuid);
        logger.info("json: " + request.body().asJson());

        TrackingEventDto event = Json.fromJson(request.body().asJson(), TrackingEventDto.class);
        validatorAspect.validateTrackingEventInput(event);
        
        String data = null;
        if (event.getData() != null) {
        	data = event.getData().asText();
        }
        
        if (TrackingEvent.Meeting.getId().equals(event.getType())) {
            MeetingEventType.getEnum(event.getSubType().byteValue());
            eventTrackingService.sendMeetingEvent(event.getMeetingId(), event.getUserId(), event.getGroupId(), event.getSubType().byteValue(), EventTrackingSource.UI.getId().byteValue(), data);
        } else if (TrackingEvent.User.getId().equals(event.getType())) {
            UserEventType.getEnum(event.getSubType().byteValue());
            eventTrackingService.sendUserEvent(event.getUserId(), event.getGroupId(), event.getSubType().byteValue(), EventTrackingSource.UI.getId().byteValue(), data);
        } else {
            logger.error("bad request");
            throw new BadRequestException(ErrorCode.InvalidTrackingEventType, ErrorCode.InvalidTrackingEventType.getName());
        }

        return created();
    }
}