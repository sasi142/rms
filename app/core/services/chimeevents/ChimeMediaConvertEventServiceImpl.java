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

@Service("chimeMediaConvertEventServiceImpl")
public class ChimeMediaConvertEventServiceImpl implements ChimeEventService {
    final static Logger logger = LoggerFactory.getLogger(ChimeMediaConvertEventServiceImpl.class);

    @Autowired
    private ChimeEventTrackingService chimeEventTrackingService;

    @Override
    public void processEvents(JSONObject queueMessageJson, String eventSource, String messageId) {
        ChimeMeetingEvent chimeMeetingEvent = frameChimeMeeting(queueMessageJson, eventSource);
        if (!StringUtils.isEmpty(chimeMeetingEvent.getEventId()) && !StringUtils.isEmpty(chimeMeetingEvent.getEventType())) {
            chimeEventTrackingService.saveChimeMeetingEvent(chimeMeetingEvent);
        }
    }

    private ChimeMeetingEvent frameChimeMeeting(JSONObject queueMessageJson, String eventSource) {
        ChimeMeetingEvent chimeMeetingEvent = new ChimeMeetingEvent();
        try {
            if (Constants.MEDIACONVERT_JOB_STATE_CHANGE.equalsIgnoreCase(queueMessageJson.getString("detail-type"))) {
                JSONObject detailJson = queueMessageJson.getJSONObject("detail");

                String eventId = queueMessageJson.has("id") ? queueMessageJson.getString("id") : null;
                String eventType = detailJson.has("status") ? detailJson.getString("status") : null;
                Long eventTime = detailJson.has("timestamp") ? detailJson.getLong("timestamp") : null;
                String data = detailJson.toString();

                if (!StringUtils.isEmpty(eventId)) {
                    chimeMeetingEvent.setEventId(eventId);
                }

                if (eventTime != null) {
                    chimeMeetingEvent.setEventTime(eventTime);
                }

                if (!StringUtils.isEmpty(eventId)) {
                    chimeMeetingEvent.setData(data);
                }

                ChimeEnums.EventType eventTypeEnum = ChimeEnums.EventType.getEnum(eventType);
                if (eventTypeEnum != null) {
                    chimeMeetingEvent.setEventType(eventTypeEnum.getId().byteValue());
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
