package core.services.chimeevents;

import core.daos.RecordingDao;
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

    @Autowired
    private RecordingDao recordingDao;

    private String sourceS3Bucket;

    private String sourceS3SubFolders;


    @Override
    public void processEvents(JSONObject queueMessageJson, String eventSource, String messageId) {
        ChimeMeetingEvent chimeMeetingEvent = frameChimeMeeting(queueMessageJson, eventSource);
        if (!StringUtils.isEmpty(chimeMeetingEvent.getEventId()) && !StringUtils.isEmpty(chimeMeetingEvent.getEventType())) {
            chimeEventTrackingService.saveChimeMeetingEvent(chimeMeetingEvent);

            if (ChimeEnums.EventType.S3PutObject.getId().byteValue() == chimeMeetingEvent.getEventType().byteValue()) {
                String destinationS3SubFolders = sourceS3SubFolders.substring(0,sourceS3SubFolders.lastIndexOf('/'));
                mediaConvertService.initiateMediaJob("s3://" + sourceS3Bucket + "/" + sourceS3SubFolders, destinationS3Bucket + "/" + destinationS3SubFolders);
            }
        }
    }

    private ChimeMeetingEvent frameChimeMeeting(JSONObject queueMessageJson, String eventSource) {
        ChimeMeetingEvent chimeMeetingEvent = new ChimeMeetingEvent();
        try {
            if (Constants.OBJECT_CREATED.equalsIgnoreCase(queueMessageJson.getString("detail-type"))) {
                JSONObject detailJson = queueMessageJson.getJSONObject("detail");
                String eventId = queueMessageJson.getString("id");
                String eventType = detailJson.getString("reason");

                JSONObject bucket = detailJson.has("bucket") ? detailJson.getJSONObject("bucket") : null;
                if (bucket != null) {
                    sourceS3Bucket = bucket.has("name") ? bucket.getString("name") : null;
                }

                JSONObject objectJson = detailJson.has("object") ? detailJson.getJSONObject("object") : null;

                if (objectJson != null) {
                    sourceS3SubFolders = objectJson.has("key") ? objectJson.getString("key") : null;
                }

                Instant instant = Instant.parse(queueMessageJson.getString("time"));
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
        }
        return chimeMeetingEvent;
    }
}
