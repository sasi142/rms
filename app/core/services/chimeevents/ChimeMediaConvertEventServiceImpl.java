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
        try {
            logger.debug("ChimeMediaConvertEventService::processEvents Method started for eventSource: {} messageId: {}", eventSource, messageId);
            ChimeMeetingEvent chimeMeetingEvent = frameChimeMeeting(queueMessageJson, eventSource);
            if (!StringUtils.isEmpty(chimeMeetingEvent.getEventId()) && !StringUtils.isEmpty(chimeMeetingEvent.getEventType())) {
                chimeEventTrackingService.saveChimeMeetingEvent(chimeMeetingEvent);
            }
        } catch (Exception ex) {
            logger.error("failed to Process Chime Meeting Event. Event Source: {} messageId: {} ", eventSource, messageId, ex);
            throw new InternalServerErrorException(Enums.ErrorCode.FAILED_TO_PROCESS_CHIME_MEETING_EVENT, Enums.ErrorCode.FAILED_TO_SAVE_CHIME_MEETING_EVENT.getName(), ex);
        }
        logger.debug("ChimeMediaConvertEventService::processEvents Method Completed for eventSource: {} messageId: {}", eventSource, messageId);
    }

    private ChimeMeetingEvent frameChimeMeeting(JSONObject queueMessageJson, String eventSource) {
        logger.debug("ChimeMediaConvertEventService::frameChimeMeeting has been started");
        ChimeMeetingEvent chimeMeetingEvent = new ChimeMeetingEvent();
        try {
            if (Constants.MEDIACONVERT_JOB_STATE_CHANGE_STR.equalsIgnoreCase(queueMessageJson.getString(Constants.DETAIL_TYPE_STR))) {
                JSONObject detailJson = queueMessageJson.getJSONObject("detail");

                String eventId = queueMessageJson.has(Constants.ID_STR) ? queueMessageJson.getString(Constants.ID_STR) : null;
                String eventType = detailJson.has(Constants.STATUS_STR) ? detailJson.getString(Constants.STATUS_STR) : null;
                Long eventTime = detailJson.has(Constants.TIMESTAMP_STR) ? detailJson.getLong(Constants.TIMESTAMP_STR) : null;
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
        logger.debug("ChimeMediaConvertEventService::frameChimeMeeting has been completed");
        return chimeMeetingEvent;
    }

}
