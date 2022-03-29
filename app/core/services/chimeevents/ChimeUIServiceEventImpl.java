package core.services.chimeevents;

import core.entities.ChimeMeetingEvent;
import core.exceptions.InternalServerErrorException;
import core.utils.ChimeEnums;
import core.utils.Constants;
import core.utils.Enums;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("chimeUIServiceEventImpl")
public class ChimeUIServiceEventImpl implements ChimeEventService {
    final static Logger logger = LoggerFactory.getLogger(ChimeUIServiceEventImpl.class);

    @Autowired
    private ChimeEventTrackingService chimeEventTrackingService;

    @Override
    public void processEvents(JSONObject queueMessageJson, String eventSource, String messageId) {
        try {
            logger.debug("ChimeUIServiceEvent::processEvents Method started for eventSource: {} messageId: {}", eventSource, messageId);
            ChimeMeetingEvent chimeMeetingEvent = frameChimeMeeting(queueMessageJson, eventSource, messageId);
            if (!StringUtils.isEmpty(chimeMeetingEvent.getEventId()) && !StringUtils.isEmpty(chimeMeetingEvent.getEventType())) {
                chimeEventTrackingService.saveChimeMeetingEvent(chimeMeetingEvent);
            }
        } catch (Exception ex) {
            logger.error("failed to Process Chime Meeting Event. Event Source: {} messageId: {} ", eventSource, messageId, ex);
            throw new InternalServerErrorException(Enums.ErrorCode.FAILED_TO_PROCESS_CHIME_MEETING_EVENT, Enums.ErrorCode.FAILED_TO_SAVE_CHIME_MEETING_EVENT.getName(), ex);
        }
        logger.debug("ChimeUIServiceEvent::processEvents Method Completed for eventSource: {} messageId: {}", eventSource, messageId);
    }

    private ChimeMeetingEvent frameChimeMeeting(JSONObject queueMessageJson, String eventSource, String messageId) {
        logger.debug("ChimeUIServiceEvent::frameChimeMeeting has been started");
        ChimeMeetingEvent chimeMeetingEvent = new ChimeMeetingEvent();
        try {
            String meetingIdAsString = queueMessageJson.has(Constants.MEETING_ID_STR) ? queueMessageJson.getString(Constants.MEETING_ID_STR) : null;
            String userIdAsString = queueMessageJson.has(Constants.USER_ID_STR) ? queueMessageJson.getString(Constants.USER_ID_STR) : null;
            Integer meetingId = null;
            Integer userId = null;
            if (!StringUtils.isEmpty(meetingIdAsString) && isNumeric(meetingIdAsString)) {
                meetingId = Integer.parseInt(meetingIdAsString);
            }
            if (!StringUtils.isEmpty(userIdAsString) && isNumeric(userIdAsString)) {
                userId = Integer.parseInt(userIdAsString);
            }
            if (meetingId != null) {
                chimeMeetingEvent.setMeetingId(meetingId);

                String eventId = queueMessageJson.has(Constants.ID_STR) ? queueMessageJson.getString(Constants.ID_STR) : messageId;

                String eventType = queueMessageJson.has(Constants.EVENT_TYPE_STR) ? queueMessageJson.getString(Constants.EVENT_TYPE_STR) : null;
                Long eventTime = queueMessageJson.has(Constants.TIMESTAMP_STR) ? queueMessageJson.getLong(Constants.TIMESTAMP_STR) : null;

                if (userId != null) {
                    chimeMeetingEvent.setUserId(userId);
                }

                if (!StringUtils.isEmpty(eventId)) {
                    chimeMeetingEvent.setEventId(eventId);
                }

                ChimeEnums.EventType eventTypeEnum = ChimeEnums.EventType.getEnum(eventType);
                if (eventTypeEnum != null) {
                    chimeMeetingEvent.setEventType(eventTypeEnum.getId().byteValue());
                }

                if (eventTime != null) {
                    chimeMeetingEvent.setEventTime(eventTime);
                }

                if (!StringUtils.isEmpty(eventId)) {
                    chimeMeetingEvent.setData(queueMessageJson.toString());
                }

                ChimeEnums.EventSource eventSourceEnum = ChimeEnums.EventSource.getEnum(eventSource);
                if (eventSourceEnum != null) {
                    chimeMeetingEvent.setEventSource(eventSourceEnum.getId().byteValue());
                }
                chimeMeetingEvent.setCreatedDate(System.currentTimeMillis());
                chimeMeetingEvent.setActive(true);
            }
        } catch (JSONException e) {
            logger.error("Failed to parse the message {}", queueMessageJson, e);
            throw new InternalServerErrorException(Enums.ErrorCode.JSON_PARSING_ERROR, Enums.ErrorCode.JSON_PARSING_ERROR.getName(), e);
        }
        logger.debug("ChimeUIServiceEvent::frameChimeMeeting has been Completed");
        return chimeMeetingEvent;
    }
}
