package core.queue;

import core.exceptions.InternalServerErrorException;
import core.services.chimeevents.ChimeEventService;
import core.services.chimeevents.ChimeEventServiceFactory;
import core.utils.ChimeEnums;
import core.utils.Enums;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("defaultQueueMessageHandler")
public class DefaultQueueMessageHandler implements QueueMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(DefaultQueueMessageHandler.class);

    @Autowired
    private ChimeEventServiceFactory chimeEventServiceFactory;


    @Override
    public void handleQueueMessage(String queueMessage, String messageId) {
        logger.debug("handleQueueMessage started for messageId: {}", messageId);
        try {
            JSONObject messageJSON = new JSONObject(queueMessage);
            String eventSource = fetchEventSource(messageJSON);
            if(!StringUtils.isEmpty(eventSource) && messageJSON != null) {
                ChimeEventService chimeEventService = chimeEventServiceFactory.getChimeEventService(eventSource);
                chimeEventService.processEvents(messageJSON, eventSource,messageId);
            } else {
                logger.error("Unable to fetch Event Source for the messageID: {}",messageId);
            }
        } catch (JSONException e) {
            logger.error("Failed to parse the message {}", queueMessage, e);
            throw new InternalServerErrorException(Enums.ErrorCode.JSON_PARSING_ERROR, Enums.ErrorCode.JSON_PARSING_ERROR.getName(),e);
        }
        logger.debug("handleQueueMessage Completed for messageId: {}", messageId);
    }

    private String fetchEventSource(JSONObject messageJSON) {
        String eventSource = null;
        try {
            eventSource = messageJSON.has("source") ? messageJSON.getString("source") : null;
            if(!StringUtils.isEmpty(eventSource)) {
                switch (eventSource) {
                    case "aws.chime":
                        eventSource = ChimeEnums.EventSource.ChimeServer.getName();
                        break;
                    case "aws.ecs":
                        eventSource = ChimeEnums.EventSource.RecordingECSTask.getName();
                        break;
                    case "aws.s3":
                        eventSource = ChimeEnums.EventSource.S3.getName();
                        break;
                    case "aws.mediaconvert":
                        eventSource = ChimeEnums.EventSource.MediaConvertJob.getName();
                        break;
                    case "WorkAppsWebApp":
                        eventSource = ChimeEnums.EventSource.WorkAppsWebApp.getName();
                        break;
                }
            }
        } catch (JSONException e) {
            logger.error("Failed to parse the message {}", messageJSON, e);
            throw new InternalServerErrorException(Enums.ErrorCode.JSON_PARSING_ERROR, Enums.ErrorCode.JSON_PARSING_ERROR.getName(),e);
        }
        logger.debug("eventSource from fetchEventSource method is: {}", eventSource);
        return eventSource;
    }

}
