package core.services.chimeevents;

import core.daos.MeetingAttendeeDao;
import core.daos.MeetingInfoDao;
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

@Service("chimeMeetingEventService")
public class ChimeMeetingEventServiceImpl implements ChimeEventService {
    final static Logger logger = LoggerFactory.getLogger(ChimeMeetingEventServiceImpl.class);

    @Autowired
    private ChimeEventTrackingService chimeEventTrackingService;

    @Autowired
    private MeetingInfoDao meetingInfoDao;

    @Autowired
    private MeetingAttendeeDao meetingAttendeeDao;

    @Override
    public void processEvents(JSONObject queueMessageJson, String eventSource, String messageId) {
        ChimeMeetingEvent chimeMeetingEvent = frameChimeMeeting(queueMessageJson, eventSource);
        if (!StringUtils.isEmpty(chimeMeetingEvent.getEventId()) && !StringUtils.isEmpty(chimeMeetingEvent.getEventType())) {
            chimeEventTrackingService.saveChimeMeetingEvent(chimeMeetingEvent);

            if (ChimeEnums.EventType.ChimeMeetingStarted.getId() == chimeMeetingEvent.getEventType().byteValue()) {
                meetingInfoDao.updateMeetingStatus(chimeMeetingEvent.getMeetingId(), ChimeEnums.EventType.ChimeMeetingStarted.getId().byteValue(), null);
            } else if (ChimeEnums.EventType.ChimeMeetingEnded.getId() == chimeMeetingEvent.getEventType().byteValue()) {
                meetingInfoDao.updateMeetingStatus(chimeMeetingEvent.getMeetingId(), ChimeEnums.EventType.ChimeMeetingEnded.getId().byteValue(), null);
            } else if (ChimeEnums.EventType.ChimeAttendeeJoined.getId() == chimeMeetingEvent.getEventType().byteValue()) {
                meetingInfoDao.updateMeetingStatus(chimeMeetingEvent.getMeetingId(), ChimeEnums.EventType.ChimeAttendeeJoined.getId().byteValue(), null);
                meetingAttendeeDao.updateMeetingAttendeeStatus(chimeMeetingEvent.getMeetingId(), chimeMeetingEvent.getUserId(), Enums.MeetingAttendeeStatus.Joined.getId().byteValue());
            } else if (ChimeEnums.EventType.ChimeAttendeeLeft.getId() == chimeMeetingEvent.getEventType().byteValue()) {
                meetingInfoDao.updateMeetingStatus(chimeMeetingEvent.getMeetingId(), ChimeEnums.EventType.ChimeAttendeeLeft.getId().byteValue(), null);
                meetingAttendeeDao.updateMeetingAttendeeStatus(chimeMeetingEvent.getMeetingId(), chimeMeetingEvent.getUserId(), Enums.MeetingAttendeeStatus.Left.getId().byteValue());
            }
        }
    }

    private ChimeMeetingEvent frameChimeMeeting(JSONObject queueMessageJson, String eventSource) {
        ChimeMeetingEvent chimeMeetingEvent = new ChimeMeetingEvent();
        try {
            if (Constants.CHIME_MEETING_STATE_CHANGE.equalsIgnoreCase(queueMessageJson.getString("detail-type"))) {
                JSONObject detailJson = queueMessageJson.getJSONObject("detail");
                String externalMeetingId = detailJson.has("externalMeetingId") ? detailJson.getString("externalMeetingId"):null;
                String externalUserId = detailJson.has("externalUserId") ? detailJson.getString("externalUserId"):null;
                Integer meetingId = null;
                Integer userId = null;
                if (!StringUtils.isEmpty(externalMeetingId) && isNumeric(externalMeetingId)) {
                    meetingId = Integer.parseInt(externalMeetingId);
                }
                if( !StringUtils.isEmpty(externalUserId) && isNumeric(externalUserId)) {
                    userId = Integer.parseInt(externalUserId);
                }
                if (meetingId != null) {
                    chimeMeetingEvent.setMeetingId(meetingId);
                    String eventId = queueMessageJson.getString("id");
                    String eventType = detailJson.getString("eventType");
                    Long eventTime = detailJson.getLong("timestamp");
                    String data = detailJson.toString();

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
                        chimeMeetingEvent.setData(data);
                    }

                    ChimeEnums.EventSource eventSourceEnum = ChimeEnums.EventSource.getEnum(eventSource);
                    if (eventSourceEnum != null) {
                        chimeMeetingEvent.setEventSource(eventSourceEnum.getId().byteValue());
                    }
                    chimeMeetingEvent.setCreatedDate(System.currentTimeMillis());
                    chimeMeetingEvent.setActive(true);
                }
            }
        } catch (JSONException e) {
            logger.error("Failed to parse the message {}", queueMessageJson, e);
            throw new InternalServerErrorException(Enums.ErrorCode.JSON_PARSING_ERROR, Enums.ErrorCode.JSON_PARSING_ERROR.getName(), e);
        }
        return chimeMeetingEvent;
    }

}
