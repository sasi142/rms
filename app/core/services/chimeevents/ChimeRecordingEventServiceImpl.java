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
        ChimeMeetingEvent chimeMeetingEvent = frameChimeMeeting(queueMessageJson, eventSource);
        if (!StringUtils.isEmpty(chimeMeetingEvent.getEventId()) && !StringUtils.isEmpty(chimeMeetingEvent.getEventType())) {
            chimeEventTrackingService.saveChimeMeetingEvent(chimeMeetingEvent);
            if(!StringUtils.isEmpty(chimeRecordingTaskId)) {
                if(ChimeEnums.EventType.RmsRecordingStarted.getId().byteValue() == chimeMeetingEvent.getEventType().byteValue()) {
                    recordingDao.updateRecordingStageByECSTaskId(chimeRecordingTaskId,ChimeEnums.EventType.RmsRecordingStarted.getId().byteValue());
                }
                if(ChimeEnums.EventType.RmsRecordingStopped.getId().byteValue() == chimeMeetingEvent.getEventType().byteValue()) {
                    recordingDao.updateRecordingStageByECSTaskId(chimeRecordingTaskId,ChimeEnums.EventType.RmsRecordingStopped.getId().byteValue());
                }
            }
        }
    }

    private ChimeMeetingEvent frameChimeMeeting(JSONObject queueMessageJson, String eventSource) {
        ChimeMeetingEvent chimeMeetingEvent = new ChimeMeetingEvent();
        try {
            if (Constants.ECS_TASK_STATE_CHANGE.equalsIgnoreCase(queueMessageJson.getString("detail-type"))) {
                JSONObject detailJson = queueMessageJson.getJSONObject("detail");
                String taskArn = detailJson.getString("taskArn");
                if(!StringUtils.isEmpty(taskArn)) {
                    chimeRecordingTaskId = taskArn.substring(taskArn.lastIndexOf("/")+1,taskArn.length());
                }
                String eventId = queueMessageJson.getString("id");
                String lastStatus = detailJson.getString("lastStatus");
                String eventType = null;
                Integer meetingId = null;
                if ("STOPPED".equalsIgnoreCase(lastStatus)) {
                    eventType = ChimeEnums.EventType.RmsRecordingStopped.getName();
                } else if ("STARTED".equalsIgnoreCase(lastStatus)) {
                    eventType = ChimeEnums.EventType.RmsRecordingStarted.getName();
                } else if ("RUNNING".equalsIgnoreCase(lastStatus)) {
                    eventType = ChimeEnums.EventType.RmsRecordingRunning.getName();
                } else if ("PENDING".equalsIgnoreCase(lastStatus)) {
                    eventType = ChimeEnums.EventType.RmsRecordingPending.getName();
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode detailJsonNode = mapper.readTree(String.valueOf(detailJson));
                JsonNode overrides = detailJsonNode.get("overrides");
                JsonNode containerOverridesList = overrides.get("containerOverrides");
                if (containerOverridesList.isArray()) {
                    for (final JsonNode containerOverrides : containerOverridesList) {
                        JsonNode environmentList = containerOverrides.get("environment");
                        if (environmentList.isArray()) {
                            for (final JsonNode environment : environmentList) {
                                String name = environment.get("name").asText();
                                if ("MEETING_URL".equalsIgnoreCase(name)) {
                                    String meetingIdValue = environment.get("value").asText();
                                    if (!StringUtils.isEmpty(meetingIdValue)) {
                                        if (meetingIdValue.contains("m=")) {
                                            String rmsMeetingId = meetingIdValue.substring(meetingIdValue.lastIndexOf("=")+1, meetingIdValue.length());
                                            if(isNumeric(rmsMeetingId)) {
                                                chimeMeetingEvent.setMeetingId(Integer.valueOf(rmsMeetingId));
                                            }
                                        } else {
                                            if(isNumeric(meetingIdValue)) {
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
                Instant instant = Instant.parse(detailJson.getString("updatedAt"));
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
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return chimeMeetingEvent;
    }
}
