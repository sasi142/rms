package core.utils;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.daos.ElasticSearchDao;
import core.daos.MeetingInfoDao;
import core.entities.Meeting;
import core.entities.VideoSignallingMessage;
import core.exceptions.InternalServerErrorException;
import core.exceptions.ResourceNotFoundException;
import core.utils.Enums.ErrorCode;


@Component
public class MeetingUtil implements InitializingBean {
	static final Logger logger = LoggerFactory.getLogger(MeetingUtil.class);
	@Autowired
	private MeetingInfoDao meetingInfoDao;

	@Autowired
	private ElasticSearchDao elasticSearchDao;

	@Autowired
	private Environment env;

	private String esNetworkInfoIndexName;	
	private String esNetworkInfoTypeName;

	public Meeting getMeetingDetails(VideoSignallingMessage videoSignallingMessage) {

		//	validateMeeting(videoSignallingMessage);
		Meeting meeting = new Meeting();
		//	meeting.setId(videoSignallingMessage.getMeetingId());
		meeting.setActive(true);
		meeting.setMeetingRating((byte)0);
		meeting.setStartDate(0L);
		meeting.setEndDate(0L);
		meeting.setCreatedDate(System.currentTimeMillis());
		meeting.setFrom(videoSignallingMessage.getFrom());
		meeting.setTo(videoSignallingMessage.getTo());
		meeting.setLastPingDate(null);
		if(Objects.nonNull(videoSignallingMessage.getGroupId())) {
			meeting.setGroupId(videoSignallingMessage.getGroupId().longValue());
		}
		meeting.setMeetingStatus(videoSignallingMessage.getVideoCallStatus().byteValue());
		meeting.setMeetingType(videoSignallingMessage.getVideoCallType().byteValue());

		meeting.setUpdatedDate(System.currentTimeMillis());

		return meeting;
	}

	//@Async
	public void getAndUpdateMeetingBandWidthInfo(Integer guestGroupId) {
		logger.info("getAndUpdateMeetingBandWidthInfo start");	
		List<Meeting> meetings = null;
		try {
			meetings = meetingInfoDao.findAllByGroupId(guestGroupId.longValue());
			logger.debug("got "+meetings.size()+" meetings from db for GroupId:"+guestGroupId);
		}
		catch(Exception e) {
			logger.error("error in fetching meetings data for GroupId:"+guestGroupId,e);
			return;
		}
		for(Meeting meeting :meetings) {
			try {
				String minMaxbandwidthInfo = elasticSearchDao.get(esNetworkInfoIndexName, meeting.getId());
				if(minMaxbandwidthInfo == null || minMaxbandwidthInfo.isEmpty()) {
					logger.error("ES returned null");
					//					throw new InternalServerErrorException(ErrorCode.Entity_Not_Found,"meetindId:"+meeting.getId()+" data not found in ES");
				}else {
					updateMeetingBandwidthInfo(meeting.getId(), meeting.getFrom(), meeting.getTo(), minMaxbandwidthInfo);
				}
			}
			catch(Exception e) {
				logger.error("error in getAndUpdateMeetingBandWidthInfo",e);
			}
		}
		logger.info("getAndUpdateMeetingBandWidthInfo end");
	}

	public void updateMeetingBandwidthInfo(Integer meetingId, Integer callerId, Integer receiverId, String bandWidthInfo) {
		logger.info("parse bandWidthInfo");
		Short receiverMinBandwidth=null, receiverMaxBandwidth=null, callerMinBandwidth=null, callerMaxBandwidth=null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(bandWidthInfo);
			JsonNode users = jsonNode.findValue("aggregations").findValue("dimension").withArray("buckets");
			if(users.size() > 0) {
				for(JsonNode user : users) {
					Integer userId = user.get("key").asInt();
					logger.debug("user json:"+user.toString());
					if(callerId.intValue() == userId) {
						callerMaxBandwidth = getMaxBandWidth(user).shortValue();
						logger.debug("callerMaxBandwidth:"+callerMaxBandwidth);
						callerMinBandwidth = getMinBandWidth(user).shortValue();
						logger.debug("callerMinBandwidth:"+callerMinBandwidth);
					}
					else if(receiverId.intValue() == userId) {			
						receiverMaxBandwidth = getMaxBandWidth(user).shortValue();
						logger.debug("receiverMaxBandwidth:"+receiverMaxBandwidth);
						receiverMinBandwidth = getMinBandWidth(user).shortValue();
						logger.debug("receiverMinBandwidth:"+receiverMinBandwidth);
					}
					else{
						logger.error("userId:"+userId+" is neither a caller or receiver of meetingId:"+meetingId);
						throw new InternalServerErrorException(ErrorCode.Invalid_Data,"userId:"+userId+" is not a caller or receiver");
					}
				}
			}
			else {
				logger.error("didnot get any BandWidthInfo for receiver/caller for meetingId:"+meetingId);
				throw new InternalServerErrorException(ErrorCode.Resource_Not_Found,"didnot get any BandWidthInfo for receiver/caller for meetingId");
			}
		}
		catch(Exception e) {
			logger.error("failed to parse ES response",e);
			throw new InternalServerErrorException(ErrorCode.Failed_toParse_JSONObject,"failed to parse ES response",e);
		}
		logger.info("update meeting Info");
		meetingInfoDao.updateMeetingBandwidthInfo(meetingId, callerMaxBandwidth, callerMinBandwidth, receiverMaxBandwidth, receiverMinBandwidth);
		logger.info("details updated in db");
	}


	private void validateMeeting(VideoSignallingMessage videoSignallingMessage) {
		if(Objects.isNull(videoSignallingMessage.getMeetingId())) {
			throw new ResourceNotFoundException(ErrorCode.Entity_Not_Found, "MeetingId is Null");
		}
		Meeting meeting = meetingInfoDao.findOne(videoSignallingMessage.getMeetingId());
		Byte meetingStatus = meeting.getMeetingStatus();		
	}

	private static Integer getMaxBandWidth(JsonNode user) {
		
		Integer maxBandWidth = (getValueFromJsonNode(user, "max_bandwidth"));
		return maxBandWidth;
	}

	private static Integer getMinBandWidth(JsonNode user) {
		
		Integer minBandWidth = (getValueFromJsonNode(user, "min_bandwidth"));
		return minBandWidth;
	}

	private static Integer getValueFromJsonNode(JsonNode user, String key) {
		logger.debug("get "+key+" form userJson");
		Integer value = user.findValue(key).get("value").asInt();
		logger.debug("got "+key+" form userJson as:"+value);
		return value;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		esNetworkInfoIndexName = env.getProperty(Constants.ELASTIC_SEARCH_NETWORK_INFO_INDEX_NAME);
		logger.info("esNetworkInfoIndexName: "+esNetworkInfoIndexName);

		esNetworkInfoTypeName = env.getProperty(Constants.ELASTIC_SEARCH_NETWORK_INFO_TYPE_NAME);
		logger.info("esNetworkInfoTypeName: "+esNetworkInfoTypeName);		
	}
}
