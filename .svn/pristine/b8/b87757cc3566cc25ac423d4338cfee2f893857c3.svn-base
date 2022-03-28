package core.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.daos.CacheGroupDao;
import core.daos.CacheUserDao;
import core.entities.Group;
import core.entities.GroupMember;
import core.entities.UserContext;
import core.utils.Enums;
import core.utils.Enums.ConnectionType;
import core.utils.Enums.EventType;
import core.utils.Enums.GroupUseCaseStatus;
import core.utils.Enums.NotificationParamName;
import core.utils.CommonUtil;
import core.utils.Constants;

@Service
public class EventNotificationBuilder {
	final static Logger logger = LoggerFactory.getLogger(EventNotificationBuilder.class);
	@Autowired
	private CacheUserDao cacheUserDao;

	@Autowired
	private Environment env;

	@Autowired
	private CacheGroupDao cacheGroupDao;

	@Autowired
	private CommonUtil commonUtil;

	public String buildMessage(EventType event, String data, Integer recipientUserId) {
		String message = null;
		JsonNode jsonNode = Json.parse(data);
		try {
			if (event == null) {
				event = EventType.getEventTypeById(jsonNode.findPath("eventType").asInt());
			}

			 String template = null; 
			 if (event.getId().equals(EventType.GuestChatDeviceInfo.getId()) || event.getId().equals(EventType.GuestChatDeviceInfoUpdate.getId())) { 
				 String templateName = jsonNode.findPath("templateName").asText();
				 template = env.getProperty(templateName);
			 }
				 //if(templateName.contentEquals("compatible.browser")) { return
			// "Guest User connected from a browser which allows Video Call"; } template =
			  else {
				  template = env.getProperty(event.name()); 
				  }
			 
			 logger.info("eventName:"+event+",template:"+template);

			List<NotificationParamName> templateParamNames = loadParmNames(event.name());
			message = replacePlaceHolders(template, templateParamNames, jsonNode, recipientUserId);
		} catch (Exception e) {
			logger.error("Failed to get group object from json String : " + data, e);
		}

		return message;
	}

	public String buildMessage(Long messageDate, EventType event, String data, Integer recipientUserId,
			UserContext userContext) {
		String messageText = "";
		JsonNode jsonNode = Json.parse(data);
		try {
			if (event == null) {
				event = EventType.getEventTypeById(jsonNode.findPath("eventType").asInt());
			}
			
			
			 String template = null; 
			 if (event.getId().equals(EventType.GuestChatDeviceInfo.getId()) || event.getId().equals(EventType.GuestChatDeviceInfoUpdate.getId())) { 
				 String templateName = jsonNode.findPath("templateName").asText();
				 template = env.getProperty(templateName);
			 }
				 //if(templateName.contentEquals("compatible.browser")) { return
			// "Guest User connected from a browser which allows Video Call"; } template =
			  else {
				  template = env.getProperty(event.name()); 
				  }
						//String template = env.getProperty(event.name());
			
			List<NotificationParamName> templateParamNames = loadParmNames(event.name());
			messageText = replacePlaceHolders(template, templateParamNames, jsonNode, recipientUserId);
			if (userContext != null
					&& commonUtil.getConnectionType(userContext.getClientId()).equals(ConnectionType.Mobile.getId())
					&& event != null && EventType.ChatGuestAdded.getId().equals(event.getId())) {
				messageText = CommonUtil.addDateToChatMessage(messageDate, messageText,
						userContext.getUser().getTimezone());
			}

		} catch (Exception e) {
			logger.error("Failed to get group object from json String : " + data, e);
		}

		return messageText;
	}

	private String replacePlaceHolders(String template, List<NotificationParamName> templateParamNames,
			JsonNode dataJson, Integer recipientUserId) {
		String paramValue = null;
		Integer eventType = dataJson.findPath("eventType").asInt();
		for (NotificationParamName param : templateParamNames) {
			switch (param) {
			case GrpC:
				paramValue = getUserName(dataJson.findPath("createdById").asInt(), recipientUserId);
				break;
			case Grp:
				paramValue = dataJson.findPath("groupName").asText();
				break;
			case GrpM:
				paramValue = getUserName(dataJson.findPath("affectedMemberId").asInt(), recipientUserId);
				if (EventType.UpdateGroup_AddMember.getId().equals(eventType)
						|| EventType.UpdateGroup_RemoveMember.getId().equals(eventType)
						|| EventType.UpdateGroup_RemoveAdmin.getId().equals(eventType)
						|| EventType.UpdateGroup_AddAdmin.getId().equals(eventType)) {
					if (dataJson.findPath("createdById").asInt() == dataJson.findPath("affectedMemberId").asInt()) {
						paramValue = "Self";
					}
				}
					break;
				case GrpOldName:
					paramValue = dataJson.findPath("oldName").asText();
					break;
				case GrpNewName:
					paramValue = dataJson.findPath("newName").asText();
					break;
				case AffectedMember:
					paramValue = getUserName(dataJson.findPath("affectedMemberId").asInt(), recipientUserId);
					break;
				case ActionTaker:
					paramValue = getUserName(dataJson.findPath("actionTakerId").asInt(), recipientUserId);
					break;
				case Duration:
					paramValue = CommonUtil.getDurationInHoursMinutesAndSeconds(dataJson.findPath("callDuration").asLong()).toString();
					break;
				case Receiver:
					paramValue = getUserName(dataJson.findPath("receiver").asInt(),recipientUserId);
					break;				
				case AppName:
					paramValue = commonUtil.getChatAppName(dataJson.findPath("orgId").asInt());
					break;
				case OpenChatURL:
					paramValue = env.getProperty(Constants.OPEN_GROUP_CHAT_URL) + dataJson.findPath("groupId").asText();
					break;
				
				case GuestUserLabel:
					paramValue = getGuestLabel(dataJson.findPath("guestUserId").asInt(), recipientUserId);
					logger.info("GuestUserLabel paramValue value;"+paramValue);
					break;
			
				case DeviceBrowserInfo: 
					paramValue = dataJson.findPath("deviceBrowserInfo").asText(); 
					logger.info("deviceBrowserInfo paramValue value;"+paramValue);
					break;
			 
				/*case GuestMessage:
					if(dataJson.findPath("guestUserId").asInt() == recipientUserId) {
						paramValue = "";
					}
					else {
							paramValue = "Guest User connected from a browser which allows Video Call";
					}*/
				default:
			}
			template = replacePlaceHolder(param, paramValue, template);
			logger.info("final template after replacement:"+template);
		}
		if (EventType.UpdateGroup_AddMember.getId().equals(eventType)) {
			GroupMember member = cacheGroupDao.getGroupMember(dataJson.findPath("groupId").asInt(),
					dataJson.findPath("affectedMemberId").asInt());
			if (member.getMemberRole().byteValue() == 1) {
				template = template + " as an Admin";
			}
		}
		return template;
	}
	
	private String getGuestLabel(Integer guestUserId, Integer userId) {
		String userName = "";
		if (userId.intValue() == guestUserId.intValue()) {
			userName = "You";
		} else {
			userName = "Guest User";
		}
		return userName;
	}
	
	private String replacePlaceHolder(NotificationParamName param, String paramValue, String template) {
		String parameterKey = "{" + param.name() + "}";
		logger.info("template:"+template+" param:"+param+" paramValue:"+paramValue);
		template = template.replace(parameterKey, paramValue);
		return template;
	}

	// TODO if currentUserId==userId then return You
	private String getUserName(Integer userId, Integer recipientUserId) {
		String userName = "";
		if (recipientUserId.intValue() == userId.intValue()) {
			userName = "You";
		} else {
			JsonNode user = cacheUserDao.find(userId);
			userName = user.findPath("firstName").asText();
		}
		return userName;
	}	

	private List<NotificationParamName> loadParmNames(String eventName) {
		List<NotificationParamName> paramNames = new ArrayList<NotificationParamName>();
		String key = eventName + ".params";
		logger.info("eventNameParams:"+key);
		String strParams = env.getProperty(key);
		logger.info("eventNameParams from rms.properties:"+strParams);
		if(strParams != null) {
		String[] parameters = strParams.split(",");
		for (String param : parameters) {
			logger.info("Param:"+param);
			NotificationParamName notParam = Enums.valueOf(NotificationParamName.class, param);
			paramNames.add(notParam);
		}
		}
		return paramNames;
	}

	public String createNotificationBuildingData(EventType event, Group group) {
		ObjectMapper mapper = new ObjectMapper();
		String data = null;
		try {
			group.setEventType(event.getId());
			data = mapper.writeValueAsString(group);
		} catch (Exception e) {
			logger.error("Failed to jsonify group object : ", e);
		}
		return data;
	}
}
