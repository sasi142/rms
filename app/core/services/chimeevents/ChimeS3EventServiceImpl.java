package core.services.chimeevents;

import core.entities.ChimeMeetingEvent;
import core.exceptions.InternalServerErrorException;
import core.mediaconvert.MediaConvertService;
import core.utils.ChimeEnums;
import core.utils.Constants;
import core.utils.Enums;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service("chimeS3EventServiceImpl")
public class ChimeS3EventServiceImpl implements ChimeEventService {
    final static Logger logger = LoggerFactory.getLogger(ChimeS3EventServiceImpl.class);


    @Value("${aws.mediaconvert.output.s3.bucket}")
    private String destinationS3Bucket;

    @Autowired
    private ChimeEventTrackingService chimeEventTrackingService;

    @Autowired
    private MediaConvertService mediaConvertService;

    private String sourceS3Bucket;

    private String sourceS3SubFolders;


    @Override
    public void processEvents(JSONObject queueMessageJson, String eventSource, String messageId) {
        try {
            logger.debug("ChimeS3EventService::processEvents Method started for eventSource: {} messageId: {}", eventSource, messageId);
            ChimeMeetingEvent chimeMeetingEvent = frameChimeMeeting(queueMessageJson, eventSource);
            if (!StringUtils.isEmpty(chimeMeetingEvent.getEventId()) && !StringUtils.isEmpty(chimeMeetingEvent.getEventType())) {
                chimeEventTrackingService.saveChimeMeetingEvent(chimeMeetingEvent);

                if (ChimeEnums.EventType.S3PutObject.getId().byteValue() == chimeMeetingEvent.getEventType()) {
                    String destinationS3SubFolders = sourceS3SubFolders.substring(0, sourceS3SubFolders.lastIndexOf('/'));
                    mediaConvertService.initiateMediaJob("s3://" + sourceS3Bucket + "/" + sourceS3SubFolders, destinationS3Bucket + "/" + destinationS3SubFolders);
                }
            }
        } catch (Exception ex) {
            logger.error("failed to Process Chime Meeting Event. Event Source: {} messageId: {} ", eventSource, messageId, ex);
            throw new InternalServerErrorException(Enums.ErrorCode.FAILED_TO_PROCESS_CHIME_MEETING_EVENT, Enums.ErrorCode.FAILED_TO_SAVE_CHIME_MEETING_EVENT.getName(), ex);
        }
        logger.debug("ChimeS3EventService::processEvents Method Completed for eventSource: {} messageId: {}", eventSource, messageId);
    }

    private ChimeMeetingEvent frameChimeMeeting(JSONObject queueMessageJson, String eventSource) {
        logger.debug("ChimeS3EventService::frameChimeMeeting has been started");
        ChimeMeetingEvent chimeMeetingEvent = new ChimeMeetingEvent();
        try {
            if (Constants.OBJECT_CREATED_STR.equalsIgnoreCase(queueMessageJson.getString(Constants.DETAIL_TYPE_STR))) {
                JSONObject detailJson = queueMessageJson.has(Constants.DETAIL_STR) ? queueMessageJson.getJSONObject(Constants.DETAIL_STR) : null;
                String eventId = queueMessageJson.has(Constants.ID_STR) ? queueMessageJson.getString(Constants.ID_STR) : null;
                String eventType = detailJson.has(Constants.REASON_STR) ? detailJson.getString(Constants.REASON_STR) : null;

                JSONObject bucket = detailJson.has(Constants.BUCKET_STR) ? detailJson.getJSONObject(Constants.BUCKET_STR) : null;
                if (bucket != null) {
                    sourceS3Bucket = bucket.has(Constants.NAME_STR) ? bucket.getString(Constants.NAME_STR) : null;
                }

                JSONObject objectJson = detailJson.has(Constants.OBJECT_STR) ? detailJson.getJSONObject(Constants.OBJECT_STR) : null;

                if (objectJson != null) {
                    sourceS3SubFolders = objectJson.has(Constants.KEY_STR) ? objectJson.getString(Constants.KEY_STR) : null;
                }

                Instant instant = Instant.parse(queueMessageJson.getString(Constants.TIME_STR));
                long eventTime = instant.getEpochSecond();
                String data = detailJson.toString();
                if (!StringUtils.isEmpty(eventId)) {
                    chimeMeetingEvent.setEventId(eventId);
                }

                ChimeEnums.EventType eventTypeEnum = ChimeEnums.EventType.getEnum(eventType);
                if (eventTypeEnum != null) {
                    chimeMeetingEvent.setEventType(eventTypeEnum.getId().byteValue());
                }

                if (eventTime > 0L) {
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
        }
        logger.debug("ChimeS3EventService::frameChimeMeeting has been completed");
        return chimeMeetingEvent;
    }
}
