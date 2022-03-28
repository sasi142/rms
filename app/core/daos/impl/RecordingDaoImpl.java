package core.daos.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;

import core.exceptions.InternalServerErrorException;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.HttpConnectionManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.core.env.Environment;

import core.daos.RecordingDao;
import core.entities.Recording;
import core.utils.Enums.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;

@Repository
public class RecordingDaoImpl extends AbstractJpaDAO<Recording> implements RecordingDao {

	@Autowired
	private HttpConnectionManager httpConnectionManager;

	@Autowired
	private Environment env;

	public RecordingDaoImpl() {
		super();
		setClazz(Recording.class);
	}

	@Override
	public void updateRecordingStage(Integer recordingId, Byte recordingStage, Long endDate) {
		Query query = entityManager.createNamedQuery("Recording.UpdateRecordingStage");
		query.setParameter("recordingId", recordingId);
		query.setParameter("recordingStage", recordingStage);
		query.setParameter("endDate", endDate);
		query.executeUpdate();
	}

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public void updateRecordingStageByMeetingId(Integer meetingId, Byte recordingStage) {
		logger.debug("updateRecordingStageByMeetingId Triggered for meetingId: {} recordingStage: {}",meetingId,recordingStage);
		Query query = entityManager.createNamedQuery("Recording.updateRecordingStageByMeetingId");
		query.setParameter("meetingId", meetingId);
		query.setParameter("recordingStage", recordingStage);
		query.executeUpdate();
		logger.debug("updateRecordingStageByMeetingId Completed for meetingId: {}",meetingId);
	}

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public void updateRecordingStageByECSTaskId(String chimeRecordingTaskId, Byte recordingStage) {
		logger.debug("updateRecordingStageByECSTaskId Triggered for ecsTaskId: {} recordingStage: {}",chimeRecordingTaskId,recordingStage);
		Query query = entityManager.createNamedQuery("Recording.updateRecordingStageByECSTaskId");
		query.setParameter("chimeRecordingTaskId", chimeRecordingTaskId);
		query.setParameter("recordingStage", recordingStage);
		query.executeUpdate();
		logger.debug("updateRecordingStageByECSTaskId Completed for ecsTaskId: {}",chimeRecordingTaskId);
	}

	@Transactional(rollbackFor = { Exception.class })
	@Override
	public void updateRecordingStageChimeRecordingTaskId(Integer recordingId,Byte recordingStage,String chimeRecordingTaskId) {
		logger.debug("updateRecordingStageChimeRecordingTaskId Triggered for recordingId: {}, chimeRecordingTaskId {}",recordingId,chimeRecordingTaskId);
		Query query = entityManager.createNamedQuery("Recording.updateRecordingStageChimeRecordingTaskId");
		query.setParameter("recordingId", recordingId);
		query.setParameter("recordingStage", recordingStage);
		query.setParameter("chimeRecordingTaskId", chimeRecordingTaskId);
		query.executeUpdate();
		logger.debug("updateRecordingStageChimeRecordingTaskId Completed for recordingId: {}, chimeRecordingTaskId {}",recordingId,chimeRecordingTaskId);
	}
	
	@Override
	public List<Recording> getRecordingsByGroupId(Integer groupId) {
		logger.debug("getRecordingsByGroupId Triggered for groupId: {}",groupId);
		Query query = entityManager.createNamedQuery("Recording.GetRecordingsByGroupId");
		query.setParameter("groupId", groupId);		
		List<Recording> recordingList = query.getResultList();
		logger.debug("getRecordingsByGroupId Completed for groupId: {}. RecordingList Size",recordingList.size());
		return recordingList;
	}

    @Override
    public Optional<String> markForReprocessing(final Long groupId, final Integer recordingId, final Boolean transcoding, final Integer maxReprocessingCount) {
		final StoredProcedureQuery query = entityManager.createNamedStoredProcedureQuery("Recording.MarkForReprocessing");
		query.setParameter("P_GroupId", groupId);
		query.setParameter("P_RecordingId", recordingId == null ? 0 : recordingId);
		query.setParameter("P_EnableTranscoding", transcoding);
		query.setParameter("P_ReprocessRetry", maxReprocessingCount);
		query.execute();
		final boolean isSuccess = (boolean)query.getOutputParameterValue("O_IsSuccess");
		final String message = (String)query.getOutputParameterValue("O_Message");
		logger.info("isSuccess: {}, message: {}", isSuccess, message);
		return isSuccess ? Optional.empty():Optional.of(message);
	}

    @Override
	public void updateRecordingOnStopEvent(Integer recordingId, Long chatId, Integer attachmentId, Byte recordingStage, Long endDate) {
		Query query = entityManager.createNamedQuery("Recording.UpdateRecordingOnStopEvent");
		query.setParameter("recordingId", recordingId);
		query.setParameter("recordingStage", recordingStage);
		query.setParameter("chatId", chatId);
		query.setParameter("attId", attachmentId);
		query.setParameter("endDate", endDate);
		query.executeUpdate();
	}

	@Override
	public Recording getExistingRecording(Integer meetingId) {
		logger.info("getExistingMeetingId start: {}",meetingId);
		Recording recording;
		try {
			Query query = entityManager.createNamedQuery("Recording.GetRecordingsByMeetingId");
			query.setParameter("meetingId", meetingId);
			query.setMaxResults(1);
			return (Recording)query.getSingleResult();	
		}  catch (NonUniqueResultException | NoResultException ex) {
			return null;
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to get meeting "+ e);
		}

	}

	@Override
	public String startChimeRecording(String meetingId) {
		logger.debug("startChimeRecording in RecordingDaoImpl with meetingId:"+meetingId);
		CloseableHttpClient client = httpConnectionManager.getHttpClient();
		String url = env.getProperty(Constants.AWS_CHIME_RECORDING_URL);
		logger.info("chime recording url: "+url);

		url = url+"?recordingAction=start&meetingURL="+meetingId;

		logger.info("final chime recording url: "+url);
		HttpPost httpPost = new HttpPost(url);
		String accessKey = env.getProperty(Constants.AWS_ACCESS_KEY);
		String secretKey = env.getProperty(Constants.AWS_SECRET_KEY);

		logger.info("accessKey: "+accessKey);
		logger.info("secretKey: "+secretKey);

		httpPost.addHeader("AccessKey", accessKey);
		httpPost.addHeader("SecretKey", secretKey);

		CloseableHttpResponse response = null;
		StringBuffer result = new StringBuffer();
		try {
			response = client.execute(httpPost);

			logger.debug("Start Recording Server Response for the MeetingId: "+meetingId+" is here:"+response);

			processResult(response, result);
         	if (response != null) {
				logger.info("response.getStatusLine().getStatusCode() : " + response.getStatusLine().getStatusCode());
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new RuntimeException("failed to start recording : "
							+ response.getStatusLine().getStatusCode());
				} else {
					logger.info("Recording started successfully for the meetingId: "+meetingId);
				}

			}
		} catch (Exception e) {
			logger.info("Failed to Start the Chime Recording, exception is : ", e);
			throw new InternalServerErrorException(Enums.ErrorCode.AWS_CHIME_MEETING_START_RECORDING_ERROR, "Error while Start the Chime Recording");
		} finally {
			httpPost.releaseConnection();
		}
		return result.toString();
	}

	@Override
	public String stopChimeRecording(String meetingId, String chimeRecordingTaskId) {
		CloseableHttpClient client = httpConnectionManager.getHttpClient();
		String url = env.getProperty(Constants.AWS_CHIME_RECORDING_URL);

		logger.info("chime recording url: "+url);
		logger.info("aws chime ecs taskId: "+chimeRecordingTaskId);

		url = url+"?recordingAction=stop&meetingUrl="+meetingId+"&taskId="+chimeRecordingTaskId;

		logger.info("final chime recording url: "+url);
		HttpPost httpPost = new HttpPost(url);
		String accessKey = env.getProperty(Constants.AWS_ACCESS_KEY);
		String secretKey = env.getProperty(Constants.AWS_SECRET_KEY);

		logger.info("accessKey: "+accessKey);
		logger.info("secretKey: "+secretKey);

		httpPost.addHeader("AccessKey", accessKey);
		httpPost.addHeader("SecretKey", secretKey);

		CloseableHttpResponse response = null;
		StringBuffer result = new StringBuffer();
		try {
			response = client.execute(httpPost);
			logger.debug("Stop Recording Server Response for the MeetingId: "+meetingId+" and TaskId: "+chimeRecordingTaskId+ " is here: "+response);
			processResult(response, result);
			if (response != null) {
				logger.info("response.getStatusLine().getStatusCode() : " + response.getStatusLine().getStatusCode());
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new RuntimeException("failed to stop the recording as response code is : "
							+ response.getStatusLine().getStatusCode());
				} else {
					logger.info("Recording Stopped successfully for the MeetingId: "+meetingId+" and TaskId: "+chimeRecordingTaskId);
				}

			}
		} catch (Exception e) {
			logger.info("Failed to Stop the Chime Recording, exception is : ", e);
			throw new InternalServerErrorException(Enums.ErrorCode.AWS_CHIME_MEETING_STOP_RECORDING_ERROR, "Error while Stopping the Recording of the Meeting. MeetingId:"+meetingId+"ECS TaskId: "+chimeRecordingTaskId,e);
		} finally {
			httpPost.releaseConnection();
		}
		return result.toString();
	}

	@Override
	public List<Recording> getRecordingsListByMeetingId(Integer meetingId) {
		logger.debug("getRecordingsListByMeetingId Triggered for meetingId: {}",meetingId);
		Query query = entityManager.createNamedQuery("Recording.GetRecordingsListByMeetingId");
		query.setParameter("meetingId", meetingId);
		List<Recording> recordingList = query.getResultList();
		logger.debug("getRecordingsListByMeetingId Completed for meetingId: {}. RecordingList Size",recordingList.size());
		return recordingList;
	}

	private void processResult(CloseableHttpResponse response, StringBuffer result) throws IOException {
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		if (rd != null) {
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close(); // close the stream
		}
	}
}