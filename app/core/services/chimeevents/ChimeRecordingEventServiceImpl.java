package core.services.chimeevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.daos.RecordingDao;
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

import java.time.Instant;

@Service("chimeRecordingEventServiceImpl")
public class ChimeRecordingEventServiceImpl implements ChimeEventService {
    final static Logger logger = LoggerFactory.getLogger(ChimeRecordingEventServiceImpl.class);

    @Autowired
    private ChimeEventTrackingService chimeEventTrackingService;

    @Autowired
    private RecordingDao recordingDao;

    private String chimeRecordingTaskId;

    @Override
    public void processEvents(JSONObject queueMessageJson, String eventSource, String messageId) {
        try {
            logger.debug("ChimeRecordingEventService::processEvents Method started for eventSource: {} messageId: {}", eventSource, messageId);
            ChimeMeetingEvent chimeMeetingEvent = frameChimeMeeting(queueMessageJson, eventSource);
            if (!StringUtils.isEmpty(chimeMeetingEvent.getEventId()) && !StringUtils.isEmpty(chimeMeetingEvent.getEventType())) {
                chimeEventTrackingService.saveChimeMeetingEvent(chimeMeetingEvent);
                if (!StringUtils.isEmpty(chimeRecordingTaskId)) {
                    if (ChimeEnums.EventType.RmsRecordingStarted.getId().byteValue() == chimeMeetingEvent.getEventType().byteValue()) {
                        recordingDao.updateRecordingStageByECSTaskId(chimeRecordingTaskId, ChimeEnums.EventType.RmsRecordingStarted.getId().byteValue());
                    }
                    if (ChimeEnums.EventType.RmsRecordingStopped.getId().byteValue() == chimeMeetingEvent.getEventType().byteValue()) {
                        recordingDao.updateRecordingStageByECSTaskId(chimeRecordingTaskId, ChimeEnums.EventType.RmsRecordingStopped.getId().byteValue());
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("failed to Process Chime Meeting Event. Event Source: {} messageId: {} ", eventSource, messageId, ex);
            throw new InternalServerErrorException(Enums.ErrorCode.FAILED_TO_PROCESS_CHIME_MEETING_EVENT, Enums.ErrorCode.FAILED_TO_SAVE_CHIME_MEETING_EVENT.getName(), ex);
        }
        logger.debug("ChimeRecordingEventService::processEvents Method Completed for eventSource: {} messageId: {}", eventSource, messageId);
    }

    private ChimeMeetingEvent frameChimeMeeting(JSONObject queueMessageJson, String eventSource) {
        logger.debug("ChimeRecordingEventService::frameChimeMeeting has been started");
        ChimeMeetingEvent chimeMeetingEvent = new ChimeMeetingEvent();
        try {
            if (Constants.ECS_TASK_STATE_CHANGE_STR.equalsIgnoreCase(queueMessageJson.getString(Constants.DETAIL_TYPE_STR))) {
                JSONObject detailJson = queueMessageJson.has(Constants.DETAIL_STR) ? queueMessageJson.getJSONObject(Constants.DETAIL_STR) : null;
                String taskArn = detailJson.has(Constants.TASK_ARN_STR) ? detailJson.getString(Constants.TASK_ARN_STR) : null;
                if (!StringUtils.isEmpty(taskArn)) {
                    chimeRecordingTaskId = taskArn.substring(taskArn.lastIndexOf("/") + 1, taskArn.length());
                }
                String eventId =  queueMessageJson.has(Constants.ID_STR) ? queueMessageJson.getString(Constants.ID_STR) : null;
                String lastStatus = detailJson.has(Constants.LAST_STATUS_STR) ? detailJson.getString(Constants.LAST_STATUS_STR) : null;
                String eventType = null;
                if (Constants.STOPPED_STR.equalsIgnoreCase(lastStatus)) {
                    eventType = ChimeEnums.EventType.RmsRecordingStopped.getName();
                } else if (Constants.STARTED_STR.equalsIgnoreCase(lastStatus)) {
                    eventType = ChimeEnums.EventType.RmsRecordingStarted.getName();
                } else if (Constants.RUNNING_STR.equalsIgnoreCase(lastStatus)) {
                    eventType = ChimeEnums.EventType.RmsRecordingRunning.getName();
                } else if (Constants.PENDING_STR.equalsIgnoreCase(lastStatus)) {
                    eventType = ChimeEnums.EventType.RmsRecordingPending.getName();
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode detailJsonNode = mapper.readTree(String.valueOf(detailJson));
                JsonNode overrides = (detailJsonNode != null && detailJsonNode.has(Constants.OVERRIDES_STR)) ? detailJsonNode.get(Constants.OVERRIDES_STR) : null;
                JsonNode containerOverridesList = (overrides != null && overrides.has(Constants.CONTAINER_OVERRIDES_STR)) ? overrides.get(Constants.CONTAINER_OVERRIDES_STR) : null;
                if (containerOverridesList.isArray()) {
                    for (final JsonNode containerOverrides : containerOverridesList) {
                        JsonNode environmentList = (containerOverrides != null && containerOverrides.has(Constants.ENVIRONMENT)) ? containerOverrides.get(Constants.ENVIRONMENT) : null;
                        if (environmentList != null && environmentList.isArray()) {
                            for (final JsonNode environment : environmentList) {
                                String name = (environment != null && environment.has(Constants.NAME_STR) ) ? environment.get(Constants.NAME_STR).asText() : null;
                                if (Constants.MEETING_URL_STR.equalsIgnoreCase(name)) {
                                    String meetingIdValue = (environment != null && environment.has(Constants.VALUE_STR)) ? environment.get(Constants.VALUE_STR).asText() : null;
                                    if (!StringUtils.isEmpty(meetingIdValue)) {
                                        if (meetingIdValue.contains("m=")) {
                                            String rmsMeetingId = meetingIdValue.substring(meetingIdValue.lastIndexOf("=") + 1, meetingIdValue.length());
                                            if (isNumeric(rmsMeetingId)) {
                                                chimeMeetingEvent.setMeetingId(Integer.valueOf(rmsMeetingId));
                                            }
                                        } else {
                                            if (isNumeric(meetingIdValue)) {
                                                chimeMeetingEvent.setMeetingId(Integer.valueOf(meetingIdValue));
                                            }
                                        }
                                        break;
                                    }

                                }
                            }
                        }

                    }
                }
                Instant instant = detailJson.has(Constants.UPDATED_AT) ? Instant.parse(detailJson.getString(Constants.UPDATED_AT)) : null;
                Long eventTime = instant.getEpochSecond();
                String data = detailJson.toString();

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
        } catch (JSONException e) {
            logger.error("Failed to parse the message {}", queueMessageJson, e);
            throw new InternalServerErrorException(Enums.ErrorCode.JSON_PARSING_ERROR, Enums.ErrorCode.JSON_PARSING_ERROR.getName(), e);
        } catch (JsonMappingException e) {
            logger.error("Failed to parse the message {}", queueMessageJson, e);
            throw new InternalServerErrorException(Enums.ErrorCode.JSON_MAPPING_ERROR, Enums.ErrorCode.JSON_MAPPING_ERROR.getName(), e);
        } catch (JsonProcessingException e) {
            logger.error("Failed to Process the message {}", queueMessageJson, e);
            throw new InternalServerErrorException(Enums.ErrorCode.JSON_PROCESSING_ERROR, Enums.ErrorCode.JSON_PROCESSING_ERROR.getName(), e);
        }
        logger.debug("ChimeRecordingEventService::frameChimeMeeting has been completed");
        return chimeMeetingEvent;
    }
}
