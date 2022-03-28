package core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.daos.ElasticSearchDao;
import core.daos.MeetingInfoDao;
import core.entities.RmsMessage;
import core.entities.User;
import core.exceptions.BadRequestException;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.Enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class InfoServiceImpl implements InitializingBean, InfoService {
	private static final Logger logger = LoggerFactory.getLogger(InfoServiceImpl.class);

	@Autowired
	private ElasticSearchDao elasticSearchDao;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private Environment env;

	@Autowired
	private UserConnectionService userConnectionService;

	@Autowired
	private MeetingInfoDao meetingInfoDao;
	
	private Boolean enableStoreNetworkInfo = false;
	
	@Autowired
	private EventTrackingService eventTrackingService;
	
	private String esNetworkInfoIndexName;	

	@Override
	@Transactional
	public void sendInfo(RmsMessage rmsMessage) {
		logger.debug("send network message to RU and to ES");
		JsonNode message = rmsMessage.getJsonNode();
		Integer subtype = message.findPath("subtype").asInt();
		Integer toUserId = message.findPath("recipientId").asInt();
		logger.info("send network message to RU and to ES for recipientId: {} ", toUserId);
		User toUser = null;

		if (InfoSubType.NetworkUsageOnetoOne.getId().equals(subtype)
				|| InfoSubType.DeviceOrientationOnetoOne.getId().equals(subtype)
				|| InfoSubType.NetworkUsageGroup.getId().equals(subtype)
				|| InfoSubType.DeviceOrientationGroup.getId().equals(subtype)) {
			toUser = cacheService.getUser(toUserId, false);
			if (toUser == null) {
				logger.error("Received user not found in cache " + toUserId);
				throw new BadRequestException(ErrorCode.InvalidTo, toUserId);
			}
		}

		List<String> clientIds = new ArrayList<>();
		clientIds.add(ClientType.Web.getClientId());
		clientIds.add(ClientType.OpenChat.getClientId());
		userConnectionService.sendMessageToActor(toUser.getId(), message, null, clientIds);
		
		Integer meetingId = message.findPath("meetingId").asInt();
		logger.info("meetingId: "+meetingId);

		// Invoke Elastic service for Network Usage type messages
		if (enableStoreNetworkInfo && (InfoSubType.NetworkUsageOnetoOne.getId().equals(subtype)
				|| InfoSubType.NetworkUsageGroup.getId().equals(subtype))) {

			logger.info("updated lastPingTime for meetingId: {}  and for userId: {}",meetingId ,toUserId);
			
			ObjectNode object = (ObjectNode) message;
			String bandwidth = message.findPath("bandwidth").asText();
			Long bandwidthInLong  = getBandWidth(bandwidth); 	
			object.put("bandwidth", bandwidthInLong);
			
			object.put("createdDate", System.currentTimeMillis());
			object.remove("type");
			object.remove("subtype");
			object.remove("to");
			object.remove("from");
			ObjectMapper mapper = new ObjectMapper();
			try {
				String json = mapper.writeValueAsString(object);				
				elasticSearchDao.add(esNetworkInfoIndexName, json);
			} catch (Exception e) {
				logger.error("failed to save to ES", e);
			}
		}
	}

	private Long getBandWidth(String bandwidth) {
		
		if(bandwidth.contains(" kbits/sec")) {
			return Long.valueOf(bandwidth.replace(" kbits/sec", ""));
		} else if(bandwidth.contains(" Mbits/sec")) {
			return Long.valueOf(bandwidth.replace(" Mbits/sec", "")) * 1024;
		} else if(bandwidth.contains(" Gbits/sec")) {
			return Long.valueOf(bandwidth.replace(" Gbits/sec", "")) * 1024 * 1024;
		} else {
			logger.error("bandwidth  not in proper format " + bandwidth);
			throw new BadRequestException(ErrorCode.Invalid_Data, "bandwidth value is invalid", bandwidth);
		}
	//	return null;
	}

	@Override
	public void createTrackingEvent(RmsMessage rmsMessage) {
		logger.debug("rmsMessage: "+rmsMessage.getJsonStr());
		JsonNode message = rmsMessage.getJsonNode();
		int type = message.findPath("type").asInt();
		int eventType = message.findPath("subtype").asInt();
		
		JsonNode meetingNode = message.findPath("meetingId");
		Integer meetingId = null;
		if (meetingNode != null) {
			meetingId = meetingNode.asInt();
		}
		
		JsonNode userNode = message.findPath("userId");
		Integer userId = null;
		if (userNode != null) {
			userId = userNode.asInt();
		}
		
		JsonNode groupNode = message.findPath("groupId");
		Integer groupId = null;
		if (groupNode != null) {
			groupId = groupNode.asInt();
		}
		
		JsonNode data = message.findPath("data");
		String dataStr = null;
		if (data != null) {
			logger.debug("read data as a string");
			dataStr = data.toString();
			logger.debug("read data as a string: "+dataStr);
		}
		
		logger.debug("dataStr: "+dataStr);
		
		if (type == MessageType.MeetingInfoTrackingEvent.getId()) {
			Enums.get(MeetingEventType.class, eventType);
			eventTrackingService.sendMeetingEvent(meetingId, userId, groupId, (byte) eventType, EventTrackingSource.UI.getId().byteValue(), dataStr);
		}
		else if (type == MessageType.UserInfoTrackingEvent.getId()) {
			Enums.get(UserEventType.class, eventType);
			eventTrackingService.sendUserEvent(userId, groupId, (byte) eventType, EventTrackingSource.UI.getId().byteValue(), dataStr);
		}
		else {
			logger.info("invalid event");
		}		
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		enableStoreNetworkInfo = Boolean.valueOf(env.getProperty(Constants.ENABLE_STORE_NETWORK_INFO_IN_ES));
		logger.info("enableStoreNetworkInfo: "+enableStoreNetworkInfo);
		
		esNetworkInfoIndexName = env.getProperty(Constants.ELASTIC_SEARCH_NETWORK_INFO_INDEX_NAME);
		logger.info("esNetworkInfoIndexName: "+esNetworkInfoIndexName);
	}
}
