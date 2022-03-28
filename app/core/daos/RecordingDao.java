package core.daos;

import java.util.List;
import java.util.Optional;

import core.entities.Recording;
import org.springframework.transaction.annotation.Transactional;

public interface RecordingDao extends JpaDao<Recording> {
	void updateRecordingStage(Integer recordingId, Byte recordingStage, Long endDate);
    void updateRecordingOnStopEvent(Integer recordingId, Long chatId, Integer attachmentId, Byte recordingStage,Long endDate);
    List<Recording> getRecordingsByGroupId(Integer groupId);
	void updateRecordingStageByMeetingId(Integer meetingId, Byte recordingStage);
    void updateRecordingStageByECSTaskId(String ecsTaskId, Byte recordingStage);
    void updateRecordingStageChimeRecordingTaskId(Integer recordingId, Byte recordingStage, String chimeRecordingTaskId);
	Optional<String> markForReprocessing(final Long groupId, final Integer recordingId, final Boolean transcoding, final Integer maxReprocessingCount);
	Recording getExistingRecording(Integer meetingId);
	String startChimeRecording(String meetingId);
	String stopChimeRecording(String meetingId, String chimeRecordingTaskId);
	List<Recording> getRecordingsListByMeetingId(Integer meetingId);
}
