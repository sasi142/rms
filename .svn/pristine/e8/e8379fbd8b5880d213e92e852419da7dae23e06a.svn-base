package core.services;

import core.entities.Event;
import core.entities.Recording;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public interface RecordingService {
    Recording createVideoRecording(Recording recording);

    void updateRecordingStage(Integer recordingId, Byte recordingStage);

//    void updateRecordingOnStopEvent(Integer recordingId, Long chatId, Integer attachmentId, Integer senderAttachmentId,
//                                    Byte recordingStage, Long endDate, BigDecimal duration);
//
//    void updateRecordingOnFailedEvent(Integer recordingId, Byte recordingStage, String failureReason);

    List<Recording> getRecordingsByGroupId(Integer groupId);

    void handleProcessedVideoRecordingEvent(Event event) throws IOException;

    void handleFailedVideoRecordingEvent(Event event) throws IOException;

    void markForReprocessing(final Long groupId, final Integer recordingId, final boolean transcoding);


    JSONObject checkReprocessApplicability(final Long groupId, final Integer recordingId);

	Recording getRecordingByMeetingId(Integer meetingId);
}
