package core.services.chimeevents;

import core.daos.MeetingAttendeeDao;
import core.daos.MeetingInfoDao;
import core.entities.ChimeMeetingEvent;
import core.exceptions.InternalServerErrorException;
import core.utils.ChimeEnums;
import core.utils.Enums;
import org.apache.commons.text.RandomStringGenerator;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

@Service("chimeUIServiceEventImpl")
public class ChimeUIServiceEventImpl implements ChimeEventService {
    final static Logger logger = LoggerFactory.getLogger(ChimeUIServiceEventImpl.class);

    @Autowired
    private ChimeEventTrackingService chimeEventTrackingService;

    @Autowired
    private MeetingInfoDao meetingInfoDao;

    @Autowired
    private MeetingAttendeeDao meetingAttendeeDao;

    @Override
    public void processEvents(JSONObject queueMessageJson, String eventSource, String messageId) {
        ChimeMeetingEvent chimeMeetingEvent = frameChimeMeeting(queueMessageJson, eventSource, messageId);
        if (!StringUtils.isEmpty(chimeMeetingEvent.getEventId()) && !StringUtils.isEmpty(chimeMeetingEvent.getEventType())) {
            chimeEventTrackingService.saveChimeMeetingEvent(chimeMeetingEvent);
        }
    }

    private ChimeMeetingEvent frameChimeMeeting(JSONObject queueMessageJson, String eventSource, String messageId) {
        ChimeMeetingEvent chimeMeetingEvent = new ChimeMeetingEvent();
        try {
            String meetingIdAsString = queueMessageJson.has("meetingId") ? queueMessageJson.getString("meetingId") : null;
            String userIdAsString = queueMessageJson.has("userId") ? queueMessageJson.getString("userId") : null;
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

                String eventId = queueMessageJson.has("id") ? queueMessageJson.getString("id") : messageId;

                String eventType = queueMessageJson.has("eventType") ? queueMessageJson.getString("eventType") : null;
                Long eventTime = queueMessageJson.has("timestamp") ? queueMessageJson.getLong("timestamp") : null;

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
        return chimeMeetingEvent;
    }
}
