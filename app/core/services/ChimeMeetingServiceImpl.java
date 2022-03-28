package core.services;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import controllers.dto.MeetingDto;
import core.daos.MeetingInfoDao;
import core.daos.RecordingDao;
import core.entities.JoinMeetingResponse;
import core.entities.Meeting;
import core.entities.Recording;
import core.entities.User;
import core.exceptions.InternalServerErrorException;
import core.exceptions.ResourceNotFoundException;
import core.utils.Enums;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import core.utils.AwsChimeClientUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@Service
public class ChimeMeetingServiceImpl implements ChimeMeetingService {

    final static Logger logger = LoggerFactory.getLogger(ChimeMeetingServiceImpl.class);
    
    @Autowired
    private AwsChimeClientUtil awsChimeClientUtil;
    

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private EventTrackingService eventTrackingService;

    @Autowired
    private MeetingInfoDao meetingInfoDao;

    @Autowired
    private RecordingDao recordingDao;
    
    @Override
    public String createMeeting(MeetingDto meetingDto, Integer userId) {
        logger.info("createMeeting triggered for userId: {}",userId);
        // Create RMS Meeting
        Integer meetingId = meetingService.createMeeting(meetingDto.getGroupId(), meetingDto.getAlwaysCreateNewMeeting(), userId, meetingDto.getMeetingType(), meetingDto.getTo(), Enums.ChimeMeetingStatus.Created.getId().byteValue());
        String chimeMeetingResponse = "";
        try {
            if (meetingId != null) {
                //Create Meeting in AWS Chime Server
                chimeMeetingResponse = awsChimeClientUtil.createMeeting(meetingId.toString());
                if (chimeMeetingResponse != null) {
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String,Object> map = gson.fromJson(chimeMeetingResponse, type);
                    String chimeMeetingIdFromAWS = map.get("meetingId").toString();
                    logger.debug("meetingId Value:"+chimeMeetingIdFromAWS);
                    logger.info("Update Chime MeetingId into meetingInfo Entity");
                    //Update chimeMeetingIdFromAWS into meeting_info Table
                    meetingInfoDao.updateChimeMeetingId(meetingId, chimeMeetingIdFromAWS, userId);
                    logger.info("Chime Meeting id :" + chimeMeetingIdFromAWS);
                }
            }
        }catch(Exception e) {
                logger.error("failed to parse ES response",e);
                throw new InternalServerErrorException(Enums.ErrorCode.Failed_toParse_JSONObject,"failed to parse ES response",e);
            }
        return chimeMeetingResponse;
    }

    @Override
    public Meeting endMeeting(Integer meetingId, Integer currentUserID) {
        logger.info("endMeeting triggered for meetingId: {}",meetingId);
        Meeting meeting = meetingService.getMeetingDetails(meetingId);
        String endMeeting;
        if(Enums.ChimeMeetingStatus.Stopped.equals(meeting.getMeetingStatus())) {
            logger.error("MeetingId {} already ended. You can not end the meeting again.",meetingId);
            throw new InternalServerErrorException(Enums.ErrorCode.MEETING_ALREADY_ENDED, "Can not end the stopped Meeting. meetingId: "+meetingId);
        } else {
            //Updating the Meeting Status in meeting_info Table
            meetingInfoDao.updateMeetingStatus(meetingId, Enums.ChimeMeetingStatus.Stopped.getId().byteValue(), currentUserID);
            //Delete Meeting in AWS Chime Server
            endMeeting = awsChimeClientUtil.endMeeting(meeting.getChimeMeetingId());
            //Stop the Recording attached with the meeting
            List<Recording> recordingsList = recordingDao.getRecordingsListByMeetingId(meetingId);
            if (!CollectionUtils.isEmpty(recordingsList)) {
                logger.info("No. of Recordings to be stopped: {}",recordingsList.size());
                for(Recording recording : recordingsList) {
                    if (recording != null && recording.getId() != null && recording.getChimeRecordingTaskId() != null) {
                        logger.debug("recordingId: {} for the meetingId: {} ", recording.getId(), meetingId);
                        //Stop Recording in AWS Chime Server
                        recordingDao.stopChimeRecording(meeting.getChimeMeetingId(), recording.getChimeRecordingTaskId());
                        // Update RecordingStage Status as stopped in Recording Table
                        recordingDao.updateRecordingStageByMeetingId(meetingId, Enums.ChimeRecordingStage.Stopped.getId());
                    }
                }
            }
            //Updating the bean once meeting stopped
            meeting.setMeetingStatus(Enums.ChimeMeetingStatus.Stopped.getId().byteValue());
            logger.info("endMeeting completed for meetingId: {} userId: {}. ", meetingId, currentUserID);
        }
        return meeting;
    }

    @Override
    public JoinMeetingResponse joinMeeting(Integer meetingId, User currentUser, Boolean autoRecording) {
        logger.info("joinMeeting triggered for meetingId: {} userId: {}",meetingId, currentUser.getId());
        //Fetching the Meeting Details
        Meeting meeting = meetingService.getMeetingDetails(meetingId);
        //Creating the Meeting Attendee
        meetingService.createMeetingAttendee(meetingId, currentUser.getId());
        // Create Attendee in AWS Chime Server
        String attendee = awsChimeClientUtil.createAttendee(meeting.getChimeMeetingId(), currentUser.getId().toString());
        JoinMeetingResponse response = new JoinMeetingResponse();
        // Setting attendee in the result
        response.setChimeAttendeeDetails(attendee);
        if(currentUser.getId().equals(meeting.getFrom()) && autoRecording) {
            logger.info("If Attendee is Creator and autoRecording is true, Triggering the Recording Process");
            // Start Recording Process
            Recording recording = startRecording(meetingId,currentUser.getId(),true);
            //Setting up recordingId in the response
            response.setRecording(recording);
        }
        // Setting user details in the result
        response.setUser(currentUser);
        logger.info("joinMeeting completed for meetingId: {} userId: {}. The Attendee Id is: {}",meetingId, currentUser.getId(),attendee);
        return response;
    }

    @Override
    public Recording startRecording(Integer meetingId, Integer currentUserID, Boolean alwaysCreateNewRecording) {
        logger.info("startRecording triggered for meetingId: "+meetingId);
        Integer recordingId;
        try {
            //Fetching the Meeting Details
            Meeting meeting = meetingService.getMeetingDetails(meetingId);
            if(Enums.ChimeMeetingStatus.Stopped.equals(meeting.getMeetingStatus())) {
                logger.error("MeetingId {} already ended. You can not record the finished meeting.",meetingId);
                throw new InternalServerErrorException(Enums.ErrorCode.MEETING_ALREADY_ENDED, "Can not start recording for the ended meeting meetingId: "+meetingId);
            } else {
                // Creating Recording Entry
                recordingId = meetingService.createRecording(meeting.getGroupId().intValue(), meeting.getId(), Enums.RecordingType.TwoWayVideoRecording.getId().byteValue(), Enums.RecordingMethod.AwsEcsTaskRecording.getId(), currentUserID, meeting.getTo(), alwaysCreateNewRecording, Enums.ChimeRecordingStage.Created.getId());
                //Triggering the ECS Task for Recording in AWs Server
                String recordingResponse = recordingDao.startChimeRecording(meeting.getId().toString());

                if (StringUtils.isEmpty(recordingResponse)) {
                    logger.error("Start Recording Failed for the meetingId: " + meeting.getId() + " recordingId: " + recordingId);
                    throw new InternalServerErrorException(Enums.ErrorCode.AWS_CHIME_MEETING_START_RECORDING_ERROR, "Error while Start the Chime Recording");
                } else {
                    logger.debug("AWS Start Recording Response for the meetingId: " + meeting.getId() + " is below " + recordingResponse);
                    String ecsTaskARNId = recordingResponse.substring(1, recordingResponse.length() - 1);
                    String chimeRecordingTaskId = ecsTaskARNId.substring(recordingResponse.lastIndexOf("/"), ecsTaskARNId.length());
                    logger.debug("Chime Recording Task Id: " + chimeRecordingTaskId);
                    // Updating the ECS Recording TaskId and Recording Stage as "Starting" in Recording Table
                    recordingDao.updateRecordingStageChimeRecordingTaskId(recordingId, Enums.ChimeRecordingStage.Starting.getId(), chimeRecordingTaskId);
                }
            }
        } catch (Exception ex) {
            throw new InternalServerErrorException(Enums.ErrorCode.Internal_Server_Error, Enums.ErrorCode.Internal_Server_Error.getName(), ex);
        }
        Recording recording = recordingDao.findOne(recordingId);
        logger.info("startRecording completed for meetingId: {} recordingResponse: {}",meetingId,recording);
        return recording;
    }

    @Override
    public Recording stopRecording(Integer recordingId) {
        logger.info("stopRecording triggered for recordingId: "+ recordingId);
        //Fetching Recording Details
        Recording recording =  recordingDao.findOne(recordingId);
        if(recording == null) {
            logger.debug("Recording details not found for the recordingId: {}",recordingId);
            throw new ResourceNotFoundException(Enums.ErrorCode.Entity_Not_Found, "Recording Details are not found");
        } else {
            //Stopping the ECS Task running in AWS Server
            String result = recordingDao.stopChimeRecording(recordingId.toString(), recording.getChimeRecordingTaskId());
            logger.debug("Stop Recording Response from recordingDao: {}", result);
            //Updating the Recording Stage as "Stopping"
            recordingDao.updateRecordingStage(recordingId, Enums.ChimeRecordingStage.Stopping.getId(),System.currentTimeMillis());
            //Updating the Recording Bean for Response
            recording.setRecordingStage(Enums.ChimeRecordingStage.Stopping.getId());
            String stopRecordingResponse = "stopRecording process completed for meetingId: " + recordingId + " chimeRecordingTaskId: " + recording.getChimeRecordingTaskId();
            logger.debug("stopRecordingResponse :{}",stopRecordingResponse);
            return recording;
        }
    }

}
