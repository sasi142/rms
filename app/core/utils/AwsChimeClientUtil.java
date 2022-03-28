package core.utils;

import java.util.UUID;

import com.amazonaws.services.chime.model.*;
import core.exceptions.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.chime.AmazonChime;
import com.amazonaws.services.chime.AmazonChimeClient;
import com.amazonaws.services.chime.AmazonChimeClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import controllers.MeetingController;
import utils.RmsApplicationContext;

@Component
public class AwsChimeClientUtil implements InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(AwsChimeClientUtil.class);
	private AmazonChime client;
	private String awsRegion;

	@Autowired
	private Environment env;

	@Override
	public void afterPropertiesSet() throws Exception {
	    String accessKey = env.getProperty(Constants.AWS_ACCESS_KEY);
		String secretKey = env.getProperty(Constants.AWS_SECRET_KEY);
		awsRegion = env.getProperty(Constants.AWS_REGION);
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		AWSCredentialsProvider credentialsProvider = null;
		credentialsProvider = new AWSStaticCredentialsProvider(credentials);			
		AmazonChimeClientBuilder builder = AmazonChimeClient.builder();		
		builder.setCredentials(credentialsProvider);
		ClientConfiguration config = builder.getClientConfiguration();
		builder.setRegion(awsRegion);
		client = builder.build();	
	}

	public String createMeeting(String meetingId) {
		logger.info("AwsChimeClientUtil::createMeeting triggered with meetingId:"+meetingId);
		String meetingJson = null;
		try {
		CreateMeetingRequest createMeetingRequest = new CreateMeetingRequest();
		createMeetingRequest.setMediaRegion(awsRegion);
		createMeetingRequest.setExternalMeetingId(meetingId);
		createMeetingRequest.setClientRequestToken(UUID.randomUUID().toString());		
		CreateMeetingResult meeting = client.createMeeting(createMeetingRequest);		
		ObjectMapper mapper = new ObjectMapper();
		meetingJson = mapper.writeValueAsString(meeting.getMeeting());
		} catch (JsonProcessingException e) {
			logger.error("Json Processing Exception during Create Meeting with AWS Chime. rms meetingId {}", meetingId,e);
			throw new InternalServerErrorException(Enums.ErrorCode.JSON_PARSING_ERROR,
					"Failed to Create Meeting with AWS Chime due to Json Parsing Error "+ e);
		} catch(Exception e) {
			logger.error("Error during Create Meeting with AWS. rms meetingId {} ",meetingId, e);
			throw new InternalServerErrorException(Enums.ErrorCode.AWS_CHIME_CREATE_MEETING_ERROR,
					"Failed to Create Meeting with AWS Chime "+ e);
		}
		logger.debug("AwsChimeClientUtil::createMeeting completed with meetingId:{} ChimeResponse: {}",meetingId,meetingJson);
		return meetingJson;
	}

	public String createAttendee(String chimeMeetingId, String userId) {
		logger.info("AwsChimeClientUtil::createAttendee triggered with chimeMeetingId: {}, userId: {}"+chimeMeetingId,userId);
		String attendeeJson = null;
		try {
		CreateAttendeeRequest createAttendeeRequest = new CreateAttendeeRequest();
		//TODO: Mocking the UserId as 11 to meet the two digit criteria. This has to replace with userId
		createAttendeeRequest.setExternalUserId("11");
		createAttendeeRequest.setMeetingId(chimeMeetingId);
		CreateAttendeeResult attendee = client.createAttendee(createAttendeeRequest);
		ObjectMapper mapper = new ObjectMapper();
		attendeeJson = mapper.writeValueAsString(attendee.getAttendee());
		} catch (JsonProcessingException e) {
			logger.error("Json Processing Exception during Create Attendee with AWS Chime.chimeMeetingId {} userId {}",chimeMeetingId,userId, e);
			throw new InternalServerErrorException(Enums.ErrorCode.JSON_PARSING_ERROR,
					"Failed to Add Attendee due to Json Parsing Error. "+ e);
		}  catch(Exception e) {
			logger.error("Error during Create Attendee with AWS. chimeMeetingId {} userId {}",chimeMeetingId,userId, e);
			throw new InternalServerErrorException(Enums.ErrorCode.AWS_CHIME_CREATE_ATTENDEE_ERROR,
					"Failed to Create Attendee with AWS Chime "+ e);
		}
		return attendeeJson;
	}

	public String endMeeting(String chimeMeetingId) {
		logger.info("AwsChimeClientUtil::endMeeting triggered with chimeMeetingId: {}"+chimeMeetingId);
		String deleteMeetingResponse = null;
		try {
		DeleteMeetingRequest deleteMeetingRequest = new DeleteMeetingRequest();
		deleteMeetingRequest.setMeetingId(chimeMeetingId);
		DeleteMeetingResult deleteMeetingResult = client.deleteMeeting(deleteMeetingRequest);
		deleteMeetingResponse="Chime Meeting Ended.";
		}  catch(Exception e) {
			logger.error("Error during End Meeting with AWS. chimeMeetingId {}",chimeMeetingId, e);
			throw new InternalServerErrorException(Enums.ErrorCode.AWS_CHIME_END_MEETING_ERROR,
					"Failed to End Meeting with AWS Chime "+ e);
		}
        logger.debug("AwsChimeClientUtil::endMeeting Response: "+deleteMeetingResponse);
		return deleteMeetingResponse;
	}
}
