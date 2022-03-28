package core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.akka.actors.RmsActorSystem;
import core.daos.*;
import core.entities.*;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationServiceImpl implements InitializingBean, NotificationService {
	final static Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

	@Autowired
	@Qualifier("GcmPushNotification")
	private PushNotificationService androidPushNotificationService;

	@Autowired
	@Qualifier("ApnsPushNotification")
	private PushNotificationService iOSPushNotificationService;

	@Autowired
	private UserConnectionService userConnectionService;

	@Autowired
	private Environment env;

	@Autowired
	private DeviceDao deviceDao;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private ChatSummaryDao chatSummaryDao;

	@Autowired
	private MemoRecipientDao memoRecipientDao;

	@Autowired
	@Qualifier("CacheImpl")
	private CacheConnectionInfoDao cacheConnectionInfoDao;

	@Autowired
	private CacheUserDao cacheUserDao;

	@Autowired
	private CacheGroupDao cacheGroupDao;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	private Boolean enableWebNotification;
	private Boolean enableApnsNotification;
	private Boolean enableGcmNotification;
	private Boolean enableWebBrowserNotification;
	private Boolean enableClosedConnectionNotificationJob;
	private Boolean enableChatNotification = false;

	@Override
	public void afterPropertiesSet() throws Exception {
		enableWebNotification = Boolean.valueOf(env.getProperty(Constants.ENABLE_WEB_NOTIFICATION));
		logger.info("enable web notification : " + enableWebNotification);

		enableApnsNotification = Boolean.valueOf(env.getProperty(Constants.ENABLE_APNS_NOTIFICATION));
		logger.info("enable apns notification : " + enableApnsNotification);

		enableGcmNotification = Boolean.valueOf(env.getProperty(Constants.ENABLE_GCM_NOTIFICATION));
		logger.info("enable gcm notification : " + enableGcmNotification);

		enableClosedConnectionNotificationJob = Boolean
				.valueOf(env.getProperty(Constants.ENABLE_CLSOED_CONNECTION_NOTIFICATION_JOB));
		logger.info("enableClosedConnectionNotificationJob : " + enableClosedConnectionNotificationJob);

		enableWebBrowserNotification = Boolean.valueOf(env.getProperty(Constants.ENABLE_WEB_BROWSER_NOTIFICATION));
		logger.info("enable web browser notification : " + enableWebBrowserNotification);

		enableChatNotification = Boolean.valueOf(env.getProperty(Constants.ENABLE_CHAT_NOTIFICATION));
		logger.info("ENABLE_CHAT_NOTIFICATION : " + enableChatNotification);
	}

	@Override
	public void sendNotifications(Notification notification) {
		notification.setType(NotificationType.WorkApps);
		JsonNode pushJson = commonUtil.getPushNotification(notification);

		logger.info("message length: " + pushJson.toString().length() + "," + pushJson.toString());
		if (enableWebNotification) {
			logger.debug("web notification is enabled");
			List<Integer> userids = notification.getRecipients();
			logger.debug("sending web notification to " + (userids == null ? null : userids.toString()));
			if (userids != null && !userids.isEmpty()) {
				for (Integer Id : userids) {
					userConnectionService.sendMessageToActor(Id, pushJson, null);
				}
			}
		}

		if (enableGcmNotification || enableApnsNotification || enableWebBrowserNotification) {
			sendMobilePushNotification(notification.getRecipients(), pushJson, NotificationType.WorkApps);
		}
	}

	@Override
	public void sendMobileNotification(int subtype, ChatMessage message, List<Integer> recipients,
			PushNotificationVisibility pushNotificationVisibility) {
		Byte chatMessageType = null;
		if(message.getChatMessageType()!= null) {
			Integer messageType = message.getChatMessageType();
			chatMessageType = messageType.byteValue();
		} 
		sendPushNotification(subtype, message.getTo(), message.getFrom(), message.getName(), message.getText(),
				message.getDate(), message.getUtcDate(), message.getMid(), recipients,
				message.getParentMsg() == null ? null : Json.toJson(message.getParentMsg()).toString(), chatMessageType, message.getChatType(),
						pushNotificationVisibility);
	}

	@SuppressWarnings("deprecation")
	@Override	
	public void sendMobileNotification(int subtype, Integer to, Integer from, String name, String text, String date,
			Long utcDate, Long mid, List<Integer> recipients, String parentMsg, Byte chatMessageType, PushNotificationVisibility pushNotificationVisibility){
		sendPushNotification(subtype, to, from, name, text, date, utcDate, mid, recipients, parentMsg, chatMessageType, null,
				pushNotificationVisibility);
	}

	private void sendPushNotification(int subtype, Integer to, Integer from, String name, String text, String date,
			Long utcDate, Long mid, List<Integer> recipients, String parentMsg, Byte chatMessageType, Integer chatType,
			PushNotificationVisibility pushNotificationVisibility) {
		logger.info(
				"subtype " + subtype + " to " + to+" from: "+from +" name: "+name+" chatMessageType: "+chatMessageType+" chatType: "+ " pushNotificationVisibility: "+pushNotificationVisibility);
		logger.info(
				"send mobile alert to user " + to + " enable ChatNotification property is " + enableChatNotification);
		if (enableChatNotification) {
			ObjectNode pushNotification = Json.newObject();
			ObjectNode data = Json.newObject();
			data.put("type", MessageType.Notification.getId());
			data.put("subtype", subtype);
			data.put("to", to);
			data.put("from", from);
			data.put("name", name);
			if(mid != null) {
		    	data.put("mid", mid);
			}
			if(pushNotificationVisibility.getId()!=null) {
				data.put("visibility", pushNotificationVisibility.getId());	
			}	
			if(chatMessageType!= null) {
				data.put("chatMessageType", chatMessageType);
			}
			if(chatType != null) {
				data.put("chatType", chatType);
			}			
			if (parentMsg != null && !parentMsg.isEmpty()) {
				JsonNode jsonParentMsg = Json.parse(parentMsg);
				data.put("parentMsg", jsonParentMsg);
			}

			ObjectNode aps = Json.newObject();
			String msg = null;
			// photo info
			//	JsonNode user = cacheUserDao.find(from);
			User user = cacheService.getUser(from, false);
			logger.info("Got User from cache for user " + from);
			logger.debug("Got User from cache for user " + from + " as " + user);
			if (name == null || "".equalsIgnoreCase(name)) {
				//	name = user.findPath("firstName").asText();
				name = user.getName();
				data.put("name", name);
			}

			NotificationType notificationType = NotificationType.Chat;
			if (subtype == NotificationType.GroupChat.getId().intValue()
					|| subtype == NotificationType.AddGroup.getId().intValue()
					|| subtype == NotificationType.AddGroupMember.getId().intValue()) {
				Group group = cacheGroupDao.getGroup(to);
				logger.info("Got Group from cache for group " + to);
				logger.debug("Got Group from cache for group " + to + " as " + group);
				name = user.getName();
				data.put("name", name);
				String groupName = group.getName();
				msg = name + "@" + groupName+ ": " + text;
				data.put("groupName", groupName);
				if (group.getGuestUserId() != null) {
					data.put("guId", group.getGuestUserId());
					data.put("creatorOrgId", group.getCreatorOrganizationId());
					if (group.getParentGroupId() != null) {
						data.put("parentGrpId", group.getParentGroupId());
					}
					if(group.getCreatedById() != null) {
						data.put("groupCreaterId", group.getCreatedById());
					}
				}
				notificationType = NotificationType.GroupChat;
			} else {
				msg = name + ": " + text;
			}

			aps.put("alert", msg);
			aps.put("sound", "default");
			aps.put("category", "reply");
			pushNotification.put("aps", aps);

			try {
				UserPhoto photoNode = user.getPhotoURL();
				ObjectNode photoJsonNode = Json.newObject();
				if(photoNode != null) {
					if(photoNode.getProfile() != null) {
						photoJsonNode.put("profile",photoNode.getProfile());
					} 

					if(photoNode.getThumbnail()!= null) {
						photoJsonNode.put("thumbnail", photoNode.getThumbnail());
					}

					data.put("photoURL", photoJsonNode);
				}			
			} catch (Exception ex) {
				logger.error("failed to read photo information from cache", ex);
			}

			if (date == null || "".equalsIgnoreCase(date)) {
				//String timezone = user.findPath("timezone").asText();
				String timezone =user.getTimezone();
				Long time = System.currentTimeMillis();
				if (utcDate != null) {
					time = utcDate;
				}
				date = commonUtil.getDateTimeWithTimeZone(time, timezone);
			}
			data.put("date", date);
			data.put("utcDate", utcDate);		
			pushNotification.put("data", data);
			logger.debug("Calling send Mobile Push Notification : " + pushNotification.toString());
			sendMobilePushNotification(recipients, pushNotification, notificationType);
			logger.info("Called send Mobile Push Notification  ");
		}
	}
	
	@Override	
	public void sendVideoCallingPushNotification(int subtype, ChatMessage message, List<Integer> recipients,
			PushNotificationVisibility pushNotificationVisibility, JsonNode videoCallData) {
		logger.info(
				"subtype " + subtype + " to " + message.getTo()+" from: "+ message.getFrom() +" name: "+message.getName()+" chatMessageType: "+ message.getChatMessageType()+" chatType: "+ " pushNotificationVisibility: "+pushNotificationVisibility);

		if (enableChatNotification) {
			ObjectNode pushNotification = Json.newObject();
			ObjectNode data = Json.newObject();
			data.put("type", MessageType.Notification.getId());
		//	data.put("subtype", subtype);
			data.put("subtype", NotificationType.VideoCall.getId());
			data.put("to", message.getTo());
			data.put("from", message.getFrom());
			data.put("name", message.getName());
			if( message.getMid() != null) 
			data.put("mid", message.getMid());
			if(pushNotificationVisibility.getId()!=null) {
				data.put("visibility", pushNotificationVisibility.getId());	
			}	
			if(message.getChatMessageType()!= null) {
				data.put("chatMessageType", message.getChatMessageType());
			}
			if(message.getChatType() != null) {
				data.put("chatType", message.getChatType());
			}
			if(message.getVideoCallMessageType() != null) {
				data.put("videoCallMessageType", message.getVideoCallMessageType());
			}
			if(message.getUuid() != null) {
				data.put("uuid", message.getUuid());
			}		

			ObjectNode aps = Json.newObject();
			String msg = null;
			// photo info
			//	JsonNode user = cacheUserDao.find(from);
			User user = cacheService.getUser(message.getFrom(), false);
			logger.info("Got User from cache for user " + message.getFrom());
			logger.debug("Got User from cache for user " + message.getFrom() + " as " + user);
			if ( message.getName() == null || "".equalsIgnoreCase( message.getName())) {
				//	name = user.findPath("firstName").asText();
			   String name = user.getName();
				data.put("name", name);
			}

			NotificationType notificationType = NotificationType.VideoCall;		
			msg =  message.getName() + ": " + message.getText();
			aps.put("alert", msg);
			aps.put("sound", "default");
			aps.put("category", "reply");
			pushNotification.put("aps", aps);
			try {
				UserPhoto photoNode = user.getPhotoURL();
				ObjectNode photoJsonNode = Json.newObject();
				if(photoNode != null) {
					if(photoNode.getProfile() != null) {
						photoJsonNode.put("profile",photoNode.getProfile());
					} 
					if(photoNode.getThumbnail()!= null) {
						photoJsonNode.put("thumbnail", photoNode.getThumbnail());
					}
					data.put("photoURL", photoJsonNode);
				}			
			} catch (Exception ex) {
				logger.debug("failed to read photo information from cache", ex);
			}

			String date =null;
			if (message.getDate() == null || "".equalsIgnoreCase(message.getDate())) {
				//String timezone = user.findPath("timezone").asText();
				String timezone =user.getTimezone();
				Long time = System.currentTimeMillis();
				if (message.getUtcDate() != null) {
					time = message.getUtcDate();
				}
				date = commonUtil.getDateTimeWithTimeZone(time, timezone);
				data.put("date", date);
				data.put("utcDate", time);
			}		
			if(videoCallData != null && !videoCallData.isNull()) {
				data.put("videoCallData", videoCallData);
			}			
			
			pushNotification.put("data", data);
			//pushNotification.put("videoCallData", videoCallData);
			logger.debug("Calling send Mobile Push Notification : " + pushNotification.toString());
			sendMobilePushNotification(recipients, pushNotification, notificationType);
			logger.info("Called send Mobile Push Notification  ");
		}
	}


	@Override
	public void sendMobilePushNotification(List<Integer> recipients, JsonNode pushJson,
			NotificationType pushNotificationType) {
		logger.debug("send mobile notification to " + recipients.toString() + ",pushNotificationType:"
				+ pushNotificationType.getId());
		for (Integer recipient : recipients) {// separated each recipient as every user should get his specific unread
			// chat count as bash
			String msgToPush = null;

			// sending orgId for white labeling
			if ((pushJson instanceof ObjectNode) && !pushJson.findPath("data").isMissingNode()
					&& (pushJson.findValue("data") instanceof ObjectNode)) {
				ObjectNode dataNode = (ObjectNode) pushJson.findValue("data");
				User reciever = cacheService.getUser(recipient, false);
				Integer orgId = reciever.getOrganizationId();
				if (NotificationType.Chat.equals(pushNotificationType)
						&& UserCategory.Guest.getId().byteValue() == reciever.getUserCategory().byteValue()) {
					Integer senderId = pushJson.findPath("from").asInt();
					User sender = cacheService.getUser(senderId, false);
					orgId = sender.getOrganizationId();
				}
				dataNode.put("orgId", orgId);
				((ObjectNode) pushJson).replace("data", dataNode);
			}

			// Get devices for recipient
			List<Integer> tempRecipients = new ArrayList<Integer>();
			tempRecipients.add(recipient);
			List<Device> devices = deviceDao.getDevices(tempRecipients, pushNotificationType);
			logger.info("number of devices: " + devices.size());

			// android
			if (enableGcmNotification && !devices.isEmpty()) {
				List<Device> andriodDevices = CommonUtil.getDevicesByType(devices, DeviceType.Android);
				logger.info("Sending android push notification. num devices: " + andriodDevices.size());
				if (!andriodDevices.isEmpty()) {
					msgToPush = addBadgeCount(pushJson, recipient, true);
					logger.debug("gcm - pushNotification object : " + msgToPush);
					logger.debug("sending android notification to " + andriodDevices.toString());
					PushNotification pushNotification = new PushNotification(msgToPush, andriodDevices,
							pushNotificationType);
					RmsActorSystem.getGcmRouterActorRef().tell(pushNotification, null);
					logger.info("Handend over android notification to GcmRouterActorRef");
				}
			}

			// ios
			if (enableApnsNotification && !devices.isEmpty()) {
				List<Device> iOSDevices = CommonUtil.getDevicesByType(devices, DeviceType.iOS);
				logger.info("Sending ios push notification. " + iOSDevices.size());
				if (!iOSDevices.isEmpty()) {
					msgToPush = addBadgeCount(pushJson, recipient, true);
					logger.debug("apns - pushNotification object : " + msgToPush + " , message length: "
							+ msgToPush.length());
					logger.debug("sending iOS notification to " + iOSDevices.toString());
					PushNotification pushNotification = new PushNotification(msgToPush, iOSDevices,
							pushNotificationType);
					RmsActorSystem.getApnsRouterActorRef().tell(pushNotification, null);
					logger.info("Handend over iOS notification to ApnsRouterActorRef");
				}
			}

			// web browser
			if (enableWebBrowserNotification && !devices.isEmpty()) {
				List<Device> webBrowserDevices = CommonUtil.getDevicesByType(devices, DeviceType.WebBrowser);
				logger.info("Sending web browser push notification. " + webBrowserDevices.size());
				if (!webBrowserDevices.isEmpty()) {
					msgToPush = addBadgeCount(pushJson, recipient, true);
					logger.debug("web browser - pushNotification object : " + msgToPush + " , message length: "
							+ msgToPush.length());
					logger.info("sending web browser notification to " + webBrowserDevices.toString());
					PushNotification pushNotification = new PushNotification(msgToPush, webBrowserDevices,
							pushNotificationType);
					RmsActorSystem.getWebBrowserActorRef().tell(pushNotification, null);
					logger.info("Handend over web browser notification to WebBrowserActorRef");
				} else {
					logger.info("web browser devices are empty");
				}
			}
		}
	}

	


	private String addBadgeCount(JsonNode pushJson, Integer recipient, Boolean setCount) {
		logger.info("Add badge count for recioient " + recipient + " when setCount flag is " + setCount);
		String msgToPush;
		Long badgeCount = 0L;
		try {
			if (setCount) {
				badgeCount = chatSummaryDao.getUnReadSenderCount(recipient);
				try {
					Long unreadMemoCount = memoRecipientDao.getMemoCountByStatus(recipient, false);
					badgeCount = badgeCount + unreadMemoCount;
				} catch (Exception e) {
					logger.debug("failed to getUnreadMemoCount so treating it as 0, nothing gets added to badge count ",
							e);
				}
			}
			JSONObject node = new JSONObject(pushJson.toString());
			JSONObject aps = (JSONObject) node.get("aps");
			aps.put("badge", badgeCount);
			msgToPush = node.toString();
		} catch (JSONException e) {
			logger.debug("failed to parse  pushNotification object while setting badge count to recipient " + recipient,
					e);
			msgToPush = pushJson.toString();
		}
		return msgToPush;
	}
}
