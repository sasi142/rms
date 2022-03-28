package core.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.daos.CacheConnectionInfoDao;
import core.daos.CacheGroupDao;
import core.daos.CacheOpenMapInfoDao;
import core.daos.ContactDao;
import core.entities.*;
import core.exceptions.BadRequestException;
import core.exceptions.ForbiddenException;
import core.exceptions.ResourceNotFoundException;
import core.utils.AccessCheckUtil;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums.*;
import core.utils.PropertyUtil;
import core.validator.Validator;
import messages.UserConnection;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import play.libs.Json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPOutputStream;

@Service
public class IqMessageServiceImpl implements IqMessageService {
	final static Logger logger = LoggerFactory.getLogger(IqMessageServiceImpl.class);

	@Autowired
	private ChatHistoryService chatHistoryService;

	@Autowired
	private UserConnectionService userConnectionService;

	@Autowired
	public Environment env;

	@Autowired
	private Validator validator;

	@Autowired
	private CacheGroupDao cacheGroupDao;

	@Autowired
	private UserService userService;

	@Autowired
	private PresenceService presenceService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private ContactDao contactDao;

	@Autowired
	private CacheOpenMapInfoDao cacheOpenMapInfoDao;

	@Autowired
	@Qualifier("CacheImpl")
	private CacheConnectionInfoDao cacheConnectionInfoDao;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private RecordingService recordingService;

	@Autowired
	private AccessCheckUtil accessCheckUtil;

	@SuppressWarnings("deprecation")
	public void handleIqRequest(UserConnection userConnection, IqMessage iqMessage) {
		validator.validate(userConnection, iqMessage);
		logger.debug("IqMessageServiceImpl :UserContext: " + userConnection.getUserContext());
		logger.info("handleIqRequest : " + iqMessage.getAction());
		if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetUnReadCount.getName())) {
			List<ChatSummary> contactUnreadDetails = chatHistoryService
					.getUnReadMsgsPerContact(userConnection.getUserContext().getUser().getId());
			logger.info("iq - got unread message per contact, count is " + contactUnreadDetails.size());
			JsonNode unReadUserCountJson = getContactCountJson(userConnection, contactUnreadDetails);
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = mapper.createObjectNode();
			node.put("unReadMsgCount", unReadUserCountJson);
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.UpdateChatStatusRead.getName())) {
			Integer to = Integer.valueOf(iqMessage.getParams().get("to").toString());
			ChatType chatType = ChatType.One2One;
			Object objChatType = iqMessage.getParams().get("chatType");
			if (objChatType != null) {
				Integer intChatType = Integer.valueOf(objChatType.toString());
				chatType = ChatType.getChatTypeById(intChatType);
			}
			logger.debug("iq - update ChatReadStatus");
			chatHistoryService.updateChatReadStatus(to, chatType, MessageType.IQ, userConnection.getUserContext());
			logger.info("iq - updated ChatReadStatus");
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = mapper.createObjectNode();
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetPresence.getName())) {
			Integer Id = Integer.valueOf(iqMessage.getParams().get("id").toString());
			logger.debug("iq - get presence of user " + Id);
			Integer chatType = null;
			if(iqMessage.getParams().get("chatType") != null) 
				chatType = Integer.valueOf(iqMessage.getParams().get("chatType").toString());			
			Presence presence = null;
			if( chatType != null && chatType == ChatType.GroupChat.getId().intValue()) {
				Group group = cacheService.getGroupDetails(Id);
				if(group.getGroupType().equals(GroupType.GuestDirectGroupChat.getId().byteValue())) {	
					Integer creatorId = group.getCreatedById();
					presence = presenceService.getPresence(creatorId, true);
					logger.info("get presence info of creator of group" + presence.toString());
				} else {
					presence = presenceService.getGroupPresence(Id);
					logger.info("get presence info of group" + presence.toString());
				}				
			} else {
				presence = presenceService.getPresence(Id, true);	
				logger.info("get presence info of another registerd user" + presence.toString());
			}
			logger.info("iq - got presence received " + presence.toString());
			ObjectNode node = Json.newObject();
			node.put("presence", Json.toJson(presence));
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetUser.getName())) {
			Integer Id = Integer.valueOf(iqMessage.getParams().get("id").toString());
			logger.debug("iq - get user info for currentUser " + userConnection.getUserContext().getUser().getId()
					+ " and contact " + Id);
			Contact contact = userService.getUserInfo(userConnection.getUserContext().getUser().getId(), Id);
			logger.debug("iq - got user as " + contact.toString());
			ObjectNode node = Json.newObject();
			node.put("user", Json.toJson(contact));
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetContacts.getName())) {
			logger.debug("iq - get Contacts: with params: " + iqMessage.getParams() + " start-time: "
					+ System.currentTimeMillis());
			UserContext userContext = userConnection.getUserContext();
			if (!commonUtil.isMobileChatClient(userContext.getClientId())) {
				throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
						"Only Mobile ChatApps can access this API");
			}
			Integer offset = ((iqMessage.getParams().get("offset") != null)
					? Integer.valueOf(iqMessage.getParams().get("offset"))
							: 0);
			Integer limit = ((iqMessage.getParams().get("limit") != null)
					? Integer.valueOf(iqMessage.getParams().get("limit"))
							: 500);
			List<ChatContact> contacts = new ArrayList<>();
			if (iqMessage.getParams().get("timeRanges") != null) {
				JsonNode syncDates = Json.parse(iqMessage.getParams().get("timeRanges"));
				List<String> syncDateStr = new ArrayList<String>();
				for (int count = 0; count < syncDates.size(); count++) {
					syncDateStr
					.add(((syncDates.get(count).get("end") != null) ? syncDates.get(count).get("end").asLong()
							: "0000000000000") + ":" + syncDates.get(count).get("start").asLong());
				}
				// Check if it is first time sync
				Boolean isFirstTimeSync = iqMessage.getParams().get("isFirstTimeSync") != null
						? Boolean.valueOf(iqMessage.getParams().get("isFirstTimeSync"))
								: false;
						logger.debug("iq - getChatContacts: with timeRanges : " + syncDateStr + ", isFirstTimeSync flag = "
								+ isFirstTimeSync + " start-time: " + System.currentTimeMillis());
						contacts = contactDao.getChatContacts(userContext, offset, limit, StringUtils.join(syncDateStr, ','),
								isFirstTimeSync);
						logger.debug("iq - getChatContacts: with timeRanges : " + syncDateStr + ", isFirstTimeSync flag = "
								+ isFirstTimeSync + " responded with contact list size " + contacts.size() + " end-time: "
								+ System.currentTimeMillis());
			}
			logger.debug("iq - getChatContacts, createChatContacts start-time: " + System.currentTimeMillis());
			createChatContacts(userContext, contacts);
			logger.debug("iq - getChatContacts, createChatContacts end-time: " + System.currentTimeMillis());
			ObjectNode node = Json.newObject();
			node.put("chatcontacts", Json.toJson(contacts));
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
			logger.debug("iq - get Contacts: end-time: " + System.currentTimeMillis());
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetPresenceLocAndUnreadCount.getName())) {
			List<Contact> contacts = new ArrayList<Contact>();
			if (iqMessage.getParams().get("contacts") != null) {
				JsonNode contactNodes = Json.parse(iqMessage.getParams().get("contacts"));
				for (int count = 0; count < contactNodes.size(); count++) {
					if (contactNodes.get(count).get("Id") != null && contactNodes.get(count).get("chatType") != null) {
						contacts.add(new Contact(contactNodes.get(count).get("Id").asInt(),
								Byte.valueOf(contactNodes.get(count).get("chatType").asText())));
					}
				}
			}
			logger.debug("iq - get presence location using currentUserId "
					+ userConnection.getUserContext().getUser().getId() + " of contacts " + contacts.size());
			contacts = userService.getPresenceLocationUnreadCount(userConnection.getUserContext().getUser().getId(),
					contacts);
			logger.info("iq - get presence location using currentUserId "
					+ userConnection.getUserContext().getUser().getId() + " returned contacts " + contacts.size());
			ObjectNode node = Json.newObject();
			node.put("presence-location-unreadcount", Json.toJson(contacts));
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetContactCountToSync.getName())) {
			logger.debug("iq - GetContactCountToSync : with params: " + iqMessage.getParams());
			Integer contactCountToSync = 0;
			UserContext userContext = userConnection.getUserContext();
			if (iqMessage.getParams().get("timeRanges") != null) {
				JsonNode syncDates = Json.parse(iqMessage.getParams().get("timeRanges"));
				List<String> syncDateStr = new ArrayList<String>();
				for (int count = 0; count < syncDates.size(); count++) {
					syncDateStr
					.add(((syncDates.get(count).get("end") != null) ? syncDates.get(count).get("end").asLong()
							: "0000000000000") + ":" + syncDates.get(count).get("start").asLong());
				}
				logger.debug("iq - GetContactCountToSync: with timeRanges : " + syncDateStr + " and Usercontext "
						+ userContext.toString());
				contactCountToSync = contactDao.getContactCountToSync(userContext, StringUtils.join(syncDateStr, ','));
				logger.debug("iq - GetContactCountToSync: with timeRanges : " + syncDateStr + " returned count as "
						+ contactCountToSync);
			}
			ObjectNode node = Json.newObject();
			node.put("contactCountToSync", contactCountToSync);
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetChatHistory.getName())) {
			logger.debug(
					"iq - GetChatHistory: with userConnection : " + userConnection + " and iqMessage " + iqMessage);
			List<ChatMessage> chatMessages = chatHistoryService.getChatHistory(userConnection, iqMessage);
			logger.info(
					"iq - GetChatHistory: received records : " + (chatMessages == null ? null : chatMessages.size()));
			ObjectNode node = Json.newObject();
			if (chatMessages != null) {
				node.put("chathistory", Json.toJson(chatMessages));
			} else {
				node.put("chathistory", Json.toJson(""));
			}
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage outMessage = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(outMessage, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetContactsForFirstTime.getName())) {
			logger.debug("iq - GetContactsForFirstTime: with params: " + iqMessage.getParams());
			UserContext userContext = userConnection.getUserContext();
			Integer offset = ((iqMessage.getParams().get("offset") != null)
					? Integer.valueOf(iqMessage.getParams().get("offset"))
							: 0);
			Integer limit = ((iqMessage.getParams().get("limit") != null)
					? Integer.valueOf(iqMessage.getParams().get("limit"))
							: 500);
			String contacts = null;
			boolean compressed = false;
			Long count = null;
			Map<String, Object> resultMap = new HashMap<String, Object>();
			if (iqMessage.getParams().get("timeRanges") != null) {
				JsonNode syncDates = Json.parse(iqMessage.getParams().get("timeRanges"));
				List<String> syncDateStr = new ArrayList<String>();
				for (int size = 0; size < syncDates.size(); size++) {
					syncDateStr.add(
							((syncDates.get(size).get("end") != null) ? syncDates.get(size).get("end").asLong() : "0")
							+ ":" + syncDates.get(size).get("start").asLong());
				}
				resultMap = contactDao.getSyncChatContacts(userContext, offset, limit,
						StringUtils.join(syncDateStr, ','), Boolean.FALSE);
				contacts = (String) resultMap.get("Contacts");
				count = (Long) resultMap.get("Count");
				try {
					if (contacts != null) {
						contacts = new String(contacts.getBytes(), "UTF-8");
					}
					String compressionLimit = PropertyUtil.getProperty(Constants.SYNC_CONTACTS_LIMIT_FOR_COMPRESSION);
					if (count.longValue() >= Long.valueOf(compressionLimit).longValue()) {
						contacts = zipJsonString(contacts);
						compressed = true;
					}
				} catch (IOException ex) {
					logger.info("Could not compress the contacts string." + ex);
				}
			}
			ObjectNode node = Json.newObject();
			node.put("fields", (String) resultMap.get("Heading"));
			node.put("count", count);
			node.put("compressed", compressed);
			node.put("chatcontacts", Json.toJson(contacts));
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
			logger.info("iq - GetContactsForFirstTime: completed");
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetProfileInfo.getName())) {
			logger.debug("iq - GetProfileInfo: with params: " + iqMessage.getParams());
			UserContext userContext = userConnection.getUserContext();
			Integer offset = ((iqMessage.getParams().get("offset") != null)
					? Integer.valueOf(iqMessage.getParams().get("offset"))
							: 0);
			Integer limit = ((iqMessage.getParams().get("limit") != null)
					? Integer.valueOf(iqMessage.getParams().get("limit"))
							: 500);
			String contacts = null;
			boolean compressed = false;
			Long count = null;
			Map<String, Object> resultMap = new HashMap<String, Object>();
			if (iqMessage.getParams().get("timeRanges") != null) {
				JsonNode syncDates = Json.parse(iqMessage.getParams().get("timeRanges"));
				List<String> syncDateStr = new ArrayList<String>();
				for (int size = 0; size < syncDates.size(); size++) {
					syncDateStr.add(
							((syncDates.get(size).get("end") != null) ? syncDates.get(size).get("end").asLong() : "0")
							+ ":" + syncDates.get(size).get("start").asLong());
				}
				resultMap = contactDao.getSyncChatContacts(userContext, offset, limit,
						StringUtils.join(syncDateStr, ','), Boolean.TRUE);
				try {
					contacts = (String) resultMap.get("Contacts");
					if (contacts != null) {
						contacts = new String(contacts.getBytes(), "UTF-8");
					}
					count = (Long) resultMap.get("Count");
					String compressionLimit = PropertyUtil.getProperty(Constants.SYNC_CONTACTS_LIMIT_FOR_COMPRESSION);
					if (count.longValue() >= Long.valueOf(compressionLimit).longValue()) {
						contacts = zipJsonString(contacts);
						compressed = true;
					}
				} catch (IOException ex) {
					logger.info("Could not compress the contacts string." + ex);
				}
			}
			ObjectNode node = Json.newObject();
			node.put("fields", (String) resultMap.get("Heading"));
			node.put("count", count);
			node.put("compressed", compressed);
			node.put("chatcontacts", Json.toJson(contacts));
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
			logger.info("iq - GetProfileInfo: completed");
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetCustomStatus.getName())) {
			Integer Id = Integer.valueOf(iqMessage.getParams().get("id").toString());
			logger.debug("iq - GetCustomStatus of contact with ID: " + Id);
			Contact contact = userService.getUserInfo(userConnection.getUserContext().getUser().getId(), Id);
			String status = null;
			List<Map<String, String>> prefs = contact.getUserPreferences();
			if (prefs != null && !prefs.isEmpty()) {
				for (Map<String, String> pref : prefs) {
					if ("CustomStatus".equalsIgnoreCase(pref.get("name"))) {
						status = pref.get("value");
						break;
					}
				}
			}
			logger.info("iq - status received " + status);
			ObjectNode node = Json.newObject();
			if (status != null) {
				node.put("status", status);
			}
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetContactsV2.getName())) {
			logger.debug("iq - GetContactsV2: with params: " + iqMessage.getParams());
			UserContext userContext = userConnection.getUserContext();
			if (!commonUtil.isMobileChatClient(userContext.getClientId())) {
				throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
						"Only Mobile ChatApps can access this API");
			}
			Integer offset = ((iqMessage.getParams().get("offset") != null)
					? Integer.valueOf(iqMessage.getParams().get("offset"))
							: 0);
			Integer limit = ((iqMessage.getParams().get("limit") != null)
					? Integer.valueOf(iqMessage.getParams().get("limit"))
							: 500);
			List<ChatContact> contacts = new ArrayList<>();
			if (iqMessage.getParams().get("timeRanges") != null) {
				JsonNode syncDates = Json.parse(iqMessage.getParams().get("timeRanges"));
				List<String> syncDateStr = new ArrayList<String>();
				for (int count = 0; count < syncDates.size(); count++) {
					syncDateStr.add(
							(
									(syncDates.get(count).get("end") != null) ? syncDates.get(count).get("end").asLong() : "0") + ":" + syncDates.get(count).get("start").asLong());
				}
				logger.debug("iq - GetContactsV2: with timeRanges : " + syncDateStr);
				contacts = contactDao.getContactsV2(userContext, offset, limit, StringUtils.join(syncDateStr, ','));
				logger.debug("iq - GetContactsV2: with timeRanges : " + syncDateStr
						+ " responded with contact list size " + contacts.size());
			}
			ObjectNode node = Json.newObject();
			node.put("chatcontacts", Json.toJson(contacts));
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
			logger.info("iq - GetContactsV2 completed.");
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetUnreadCountV2.getName())) {
			logger.debug("iq - GetUnreadCountV2: with params: " + iqMessage.getParams());
			List<Contact> contacts = new ArrayList<Contact>();
			if (iqMessage.getParams().get("contacts") != null) {
				JsonNode contactNodes = Json.parse(iqMessage.getParams().get("contacts"));
				for (int count = 0; count < contactNodes.size(); count++) {
					if (contactNodes.get(count).get("id") != null && contactNodes.get(count).get("chatType") != null) {
						contacts.add(new Contact(contactNodes.get(count).get("id").asInt(),
								Byte.valueOf(contactNodes.get(count).get("chatType").asText())));
					}
				}
			}
			logger.debug("iq - GetUnreadCountV2 using currentUserId "
					+ userConnection.getUserContext().getUser().getId() + " of contacts " + contacts.size());
			contacts = chatHistoryService.getUnReadMsgsForContacts(userConnection.getUserContext().getUser().getId(),
					contacts);
			logger.info("iq - GetUnreadCountV2 using currentUserId " + userConnection.getUserContext().getUser().getId()
					+ " returned chatSummary count " + contacts.size());
			ObjectNode node = Json.newObject();
			node.put("unreadcount", Json.toJson(contacts));
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetUnreadCountV3.getName())) {

			Integer userId = userConnection.getUserContext().getUser().getId();

			String unreadData = cacheService.getUserUnreadCount(userId);
			if (unreadData == null) {
				logger.info("Unread count v3 data not found in cache for user {}", userId);
				List<Contact> contacts = chatHistoryService.getRecipientUnreadCount(userId);
				JsonNode json = Json.toJson(contacts);
				unreadData = json.toString();
				cacheService.storeUserUnreadCount(userId, unreadData);
				logger.info("Unread count v3 data stored in cache for user {}", userId);
			}
			else{
				logger.info("Unread count v3 data found in cache for user {}", userId);
			}
			ObjectNode node = Json.newObject();
			node.put("unreadcount", Json.parse(unreadData));
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);
			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.UpdateChatStatusReadV2.getName())) {
			List<String> one2oneMsgIds = new ArrayList<String>();
			List<String> groupMsgIds = new ArrayList<String>();

			if (iqMessage.getParams().get("chat-messages") != null) {
				JsonNode msgNodes = Json.parse(iqMessage.getParams().get("chat-messages"));
				for (int count = 0; count < msgNodes.size(); count++) {
					JsonNode msg = msgNodes.get(count);
					if (msg.get("mid") != null && msg.get("chatType") != null) {
						if (msg.get("chatType").asInt() == 0) {
							one2oneMsgIds.add(msg.get("mid").asText());
						} else {
							groupMsgIds.add(msg.get("mid").asText());
						}
					}
				}
			}
			logger.debug("iq - update ChatReadStatus");
			List<ChatMessage> msgs = chatHistoryService.updateChatReadStatusV2(userConnection.getUserContext(),
					StringUtils.join(one2oneMsgIds, ","), StringUtils.join(groupMsgIds, ","));
			chatHistoryService.sendReadACKMsgs(userConnection.getUserContext(), msgs);
			logger.info("iq - updated ChatReadStatus");

			for (ChatMessage message : msgs) {
				if(message.getChatType() == 1){
					logger.info("message chat Type:"+message.getChatType());

					Integer groupId = message.getTo();
					logger.info("groupId:"+groupId);
					Group group = cacheGroupDao.getGroup(groupId);
					if(GroupType.VideoKycGuestGroupChat.getId() == group.getGroupType().intValue()){
						message.setGroupType(group.getGroupType());
						logger.info("message:"+message);
					}
				}
			}
			ObjectNode node = Json.newObject();
			logger.info("msgs:"+msgs);
			node.put("chat-messages", Json.toJson(msgs));
			logger.info("msgs:"+Json.toJson(msgs));
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetUnreadMsgCount.getName())) {
			Integer currentUserId = userConnection.getUserContext().getUser().getId();
			List<ChatSummary> contactUnreadDetails = chatHistoryService.getUnReadMsgsPerContact(currentUserId);
			logger.info("iq - got unread message per contact, count is " + contactUnreadDetails.size());
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = mapper.createObjectNode();
			node.put("unReadMsgCount", contactUnreadDetails.size());
			iqMessage.setBody(node);
			iqMessage.setSubtype(IqType.Response.getId());
			JsonNode iqJson = Json.toJson(iqMessage);

			// send message to only requester
			logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
			RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
			userConnection.getActorRef().tell(message, null);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.MsgReadInfo.getName())) {
			handleMsgReadInfo(userConnection, iqMessage);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetUserInfo.getName())) {
			handleGetUserInfo(userConnection, iqMessage);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.UpdateUserMapWindowStatus.getName())) {
			handleUpdateMapOpenStatus(userConnection, iqMessage);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.StartVideoRecording.getName())
				|| iqMessage.getAction().equalsIgnoreCase(IqActionType.StopVideoRecording.getName())
				|| iqMessage.getAction().equalsIgnoreCase(IqActionType.SaveVideoRecording.getName())
				|| iqMessage.getAction().equalsIgnoreCase(IqActionType.DiscardVideoRecording.getName())) {
			handleVideoRecordingEvent(userConnection, iqMessage);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.DeleteChatMessage.getName())) {
			handleDeleteChatMessage(userConnection, iqMessage);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.TakeAndUploadScreenshot.getName())) {
			handleGenericIQMessage(userConnection, iqMessage);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetOrgContacts.getName())) {
			handleGetOrgContacts(userConnection, iqMessage);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetNonOrgContacts.getName())) {
			handleGetNonOrgContacts(userConnection, iqMessage);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetOrgGroupContacts.getName())) {
			handleGetGroupContacts(userConnection, iqMessage);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetGroupAndNonOrgContacts.getName())) {
			handleGetGroupAndNonOrgContacts(userConnection, iqMessage);
		} else {
			throw new BadRequestException(ErrorCode.Action_Not_Supported, ErrorCode.Action_Not_Supported.getName());
		}
	}

	private void handleDeleteChatMessage(UserConnection userConnection, IqMessage iqMessage) {
		// TODO: Add logging
		if (iqMessage.getParams().get("mids") != null) {
			JsonNode msgNodes = Json.parse(iqMessage.getParams().get("mids"));
			List<Integer> mids = null;
			ObjectMapper mapper = new ObjectMapper();
			try {
				mids = mapper.readValue(msgNodes.toString(), new TypeReference<List<Integer>>() {
				});
			} catch (Exception ex) {
				throw new BadRequestException(ErrorCode.Invalid_iqMessage_Data,
						ErrorCode.Invalid_iqMessage_Data.getName() + " for field : mids");
			}
			Integer chatType = Integer.valueOf(iqMessage.getParams().get("chatType"));
			Integer deleteOption = Integer.valueOf(iqMessage.getParams().get("deleteOption"));
			Integer contactId = Integer.valueOf(iqMessage.getParams().get("contactId"));
			chatHistoryService.deleteChatMessages(userConnection.getUserContext(), mids, contactId, deleteOption,
					chatType);
		}
		iqMessage.setBody(Json.toJson(iqMessage.getParams()));
		iqMessage.setSubtype(IqType.Response.getId());
		JsonNode iqJson = Json.toJson(iqMessage);

		// send message to only requester
		logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
		RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
		userConnection.getActorRef().tell(message, null);
	}

	private void handleUpdateMapOpenStatus(UserConnection userConnection, IqMessage iqMessage) {
		logger.debug("iq - UpdateUserMapWindowStatus: with userConnection : " + userConnection + " and iqMessage "
				+ iqMessage);
		Integer mapOpen = Integer.valueOf(iqMessage.getParams().get("mapOpen").toString());
		Integer userId = userConnection.getUserContext().getUser().getId();
		if (mapOpen.intValue() == 1) {
			cacheConnectionInfoDao.updateMapOpenStatus(userId, userConnection.getUuid(), 1);
			cacheOpenMapInfoDao.create(userId);
		} else if (mapOpen.intValue() == 0) {
			ArrayNode connectionNodes = cacheConnectionInfoDao.getAll(userId);
			logger.debug("Got all user connections for user " + userId + " as " + connectionNodes);
			int count = 0;
			if (connectionNodes != null && connectionNodes.size() > 0) {
				for (JsonNode connectionNode : connectionNodes) {
					if (!connectionNode.findPath("mapOpen").isMissingNode()
							&& connectionNode.findPath("mapOpen").asInt() == 1) {
						if (userConnection.getUuid().equalsIgnoreCase(connectionNode.findPath("uuid").asText())) {
							cacheConnectionInfoDao.updateMapOpenStatus(userId, userConnection.getUuid(), 0);
						} else {
							count++;
						}
					}
				}
			}
			if (count == 0) {
				cacheOpenMapInfoDao.remove(userConnection.getUserContext().getUser().getId());
			}
		}
		ObjectNode node = Json.newObject();
		iqMessage.setBody(node);
		iqMessage.setSubtype(IqType.Response.getId());
		JsonNode iqJson = Json.toJson(iqMessage);

		// send message to only requester
		logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
		RmsMessage outMessage = new RmsMessage(iqJson, RmsMessageType.Out);
		userConnection.getActorRef().tell(outMessage, null);
	}

	private void handleGetUserInfo(UserConnection userConnection, IqMessage iqMessage) {
		Integer Id = Integer.valueOf(iqMessage.getParams().get("id").toString());
		logger.debug("iq - get user info for currentUser " + userConnection.getUserContext().getUser().getId()
				+ " and contact " + Id);
		Boolean isInContact = cacheService.isInContact(userConnection.getUserContext().getUser().getId(), Id);
		if (!isInContact) {
			throw new ForbiddenException(ErrorCode.NotInContact, Id, userConnection.getUserContext().getUser().getId());
		}
		JsonNode user = cacheService.getUserJson(Id);
		if (user == null) {
			throw new ResourceNotFoundException(ErrorCode.User_not_found, ErrorCode.User_not_found.getName());
		}
		Contact contact = new Contact();
		contact.setContactType(null);
		contact.setUnreadMsg(null);
		commonUtil.setUserInfo(contact, user);
		Presence presence = presenceService.getPresence(Id, true);
		contact.setPresence(presence);
		ObjectNode node = Json.newObject();
		node.put("user-info", Json.toJson(contact));
		iqMessage.setBody(node);
		iqMessage.setSubtype(IqType.Response.getId());
		JsonNode iqJson = Json.toJson(iqMessage);

		// send message to only requester
		logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
		RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
		userConnection.getActorRef().tell(message, null);
	}

	private void handleMsgReadInfo(UserConnection userConnection, IqMessage iqMessage) {
		Long startTime = System.currentTimeMillis();
		logger.debug("iq - handleMsgReadInfo: with params: " + iqMessage.getParams() + " start-time: " + startTime);
		Long msgId = ((iqMessage.getParams().get("msgId") != null) ? Long.valueOf(iqMessage.getParams().get("msgId"))
				: 0);
		Byte chatType = Byte.valueOf(iqMessage.getParams().get("chatType"));
		Integer currentUserId = userConnection.getUserContext().getUser().getId();
		List<MessageReadInfo> msgReadInfoList = chatHistoryService.getChatMsgReadInfo(msgId, currentUserId, chatType);
		logger.debug("got GroupChat Msg ReadInfo for message : " + msgId + " as "
				+ (msgReadInfoList == null ? null : msgReadInfoList.size()));

		ObjectMapper mapper = new ObjectMapper();
		ObjectNode node = mapper.createObjectNode();
		node.put("RmsServerTime", System.currentTimeMillis());
		node.put("readInfo", Json.toJson(msgReadInfoList));

		iqMessage.setBody(node);
		iqMessage.setSubtype(IqType.Response.getId());
		JsonNode iqJson = Json.toJson(iqMessage);

		// send message to only requester
		logger.info("handledIqRequest : " + iqMessage.getAction() + " creating and sending RMS Message");
		RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
		userConnection.getActorRef().tell(message, null);
		logger.info("iq - handleMsgReadInfo : completed in " + (System.currentTimeMillis() - startTime));
	}

	private String zipJsonString(String jsonStr) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(jsonStr.length());
		GZIPOutputStream gzip = new GZIPOutputStream(bos);
		gzip.write(jsonStr.getBytes());
		gzip.close();
		byte[] compressed = bos.toByteArray();
		byte[] encodedBytes = Base64.encodeBase64(compressed);
		String encodeStr = new String(encodedBytes);
		bos.close();
		logger.debug("encodedBytes length" + encodeStr.length());
		return encodeStr;
	}

	private void createChatContacts(UserContext userContext, List<ChatContact> contacts) {
		for (ChatContact contact : contacts) {
			if (ChatType.GroupChat.getId().intValue() == contact.getChatType().intValue()) {
				logger.info("Creating Group Chat contact");
				if (contact.getGroupMembersStr() != null && !contact.getGroupMembersStr().isEmpty()) {
					JsonNode memberNodes = Json.parse(contact.getGroupMembersStr());
					Set<Contact> members = new HashSet<Contact>();
					for (int count = 0; count < memberNodes.size(); count++) {
						memberNodes.get(count);
						// Fix for iOS
						Integer contactType = memberNodes.get(count).get("contactType").asInt();
						contactType = commonUtil.overRideContactType(userContext, contactType);

						Contact groupMember = new Contact(memberNodes.get(count).get("name").asText(),
								memberNodes.get(count).get("Id").asInt(),							
								memberNodes.get(count).has("email") ? memberNodes.get(count).get("email").asText()
										: null,
								Byte.parseByte(memberNodes.get(count).get("memberStatus").asText()),
								memberNodes.get(count).has("photoUrl") ? memberNodes.get(count).get("photoUrl").asText()
										: null,
										memberNodes.get(count).has("designation")
										? memberNodes.get(count).get("designation").asText()
												: null,
												memberNodes.get(count).has("departmentName")
												? memberNodes.get(count).get("departmentName").asText()
														: null,
														contactType, null);
						groupMember.setUnreadMsg(null);
						members.add(groupMember);
					}
					contact.setGroupMembers(members);
					logger.info("Created Group Chat contact with member size " + members.size() + " and "
							+ contact.getId());
				}
			} else if (ChatType.One2One.getId().intValue() == contact.getChatType().intValue()) {
				logger.info("Creating One2One Chat contact");
				if (contact.getPhotoURLStr() != null && !contact.getPhotoURLStr().isEmpty()) {
					contact.setPhotoURL(Json.fromJson(Json.parse(contact.getPhotoURLStr()), UserPhoto.class));
					logger.debug("PhotoURL is set on One2One Chat contact");
				}
				logger.debug("Setting Preferences on One2One Chat contact");
				JsonNode prefs = null;
				if (contact.getUserPreferencesStr() != null
						&& !"".equalsIgnoreCase(contact.getUserPreferencesStr().trim())) {
					prefs = Json.parse(contact.getUserPreferencesStr());
				}
				if (prefs != null) {
					List<Map<String, String>> preferences = new ArrayList<Map<String, String>>();
					for (int count = 0; count < prefs.size(); count++) {
						JsonNode pref = prefs.get(count);
						String name = pref.findPath("name").asText();
						if (name != null && name.equalsIgnoreCase("CustomStatus")) {
							Map<String, String> mapCustStatus = new HashMap<String, String>();
							mapCustStatus.put("name", "CustomStatus");
							mapCustStatus.put("value", pref.findPath("value").asText());
							preferences.add(mapCustStatus);
						}
						if (name != null && name.equalsIgnoreCase("ShareDeviceLocation")) {
							Map<String, String> mapTrackDev = new HashMap<String, String>();
							mapTrackDev.put("name", "ShareDeviceLocation");
							mapTrackDev.put("value", pref.findPath("value").asText());
							preferences.add(mapTrackDev);
						}
						if (name != null && name.equalsIgnoreCase("ShareJourneyMap")) {
							Map<String, String> mapShareJourney = new HashMap<String, String>();
							mapShareJourney.put("name", "ShareJourneyMap");
							mapShareJourney.put("value", pref.findPath("value").asText());
							preferences.add(mapShareJourney);
						}
						if (name != null && name.equalsIgnoreCase("ShareAttendanceRegister")) {
							Map<String, String> mapShareAttendance = new HashMap<String, String>();
							mapShareAttendance.put("name", "ShareAttendanceRegister");
							mapShareAttendance.put("value", pref.findPath("value").asText());
							preferences.add(mapShareAttendance);
						}
					}
					if (!preferences.isEmpty()) {
						contact.setUserPreferences(preferences);
					}
					logger.debug("Set Preferences on One2One Chat contact as " + preferences.toString());
				}
			}
			// Fix for iOS
			Integer contactType = contact.getContactType();
			contactType = commonUtil.overRideContactType(userContext, contactType);
			contact.setContactType(contactType);
		}
	}

	private JsonNode getContactCountJson(UserConnection userConnection, List<ChatSummary> contactUnreadDetails) {
		Boolean isInContact = false;
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode arrayNode = mapper.createArrayNode();
		if (contactUnreadDetails != null && !contactUnreadDetails.isEmpty()) {

			for (ChatSummary chatSummary : contactUnreadDetails) {
				ObjectNode node = Json.newObject();
				if (ChatType.One2One.getId() == chatSummary.getChatType()) {
					isInContact = cacheService.isInContact(userConnection.getUserContext().getUser().getId(),
							chatSummary.getContactId());
					logger.debug("Check currentUserId " + userConnection.getUserContext().getUser().getId()
							+ " and contactId : " + chatSummary.getContactId() + ". isInContact returned "
							+ isInContact);

					// TODO : add name and thumbnail profile
					JsonNode userJson = cacheService.getUserJson(chatSummary.getContactId());

					if (userJson != null) {
						String name = userJson.findPath("firstName").asText();
						node.put("name", name);
						JsonNode photoURL = userJson.findPath("photoURL");
						if (photoURL != null && !photoURL.isMissingNode()) {
							String photoStr = photoURL.asText();
							if (photoStr != null && !"".equalsIgnoreCase(photoStr)) {
								photoStr = photoStr.replaceAll("//", "");
								ObjectMapper mapper1 = new ObjectMapper();
								try {
									JsonNode photoNode = (JsonNode) mapper1.readTree(photoStr);
									if (photoNode != null && !photoNode.isMissingNode()) {		
										JsonNode thumbnail = photoNode.findPath("thumbnail");
										String thumbnailStr = thumbnail.asText();
										node.put("thumbnail", thumbnailStr);
									}
								} catch (Exception ex) {
									logger.error("failed to parse photo url", ex);
								}
							}
						}
					}

				} else if (ChatType.GroupChat.getId() == chatSummary.getChatType()) {
					Group group = cacheGroupDao.getGroup(chatSummary.getContactId());				
					isInContact = cacheGroupDao.isMemberInGroup(group, userConnection.getUserContext().getUser().getId());
					logger.debug("Check currentUserId " + userConnection.getUserContext().getUser().getId()
							+ " and groupId : " + chatSummary.getContactId() + ". isInGroup returned " + isInContact);
					if(isInContact) {
						node.put("groupType", group.getGroupType());
						node.put("name", group.getName());
					}

				}
				if (isInContact) {				
					node.put("contactId", chatSummary.getContactId());
					node.put("chatType", chatSummary.getChatType());
					node.put("count", chatSummary.getUnReadMsgCount());
					arrayNode.add(node);
				}
			}
		}
		return arrayNode;
	}

	private void handleVideoRecordingEvent(UserConnection userConnection, IqMessage iqMessage) {
		logger.info("handle video recording iq message");
		Integer toUserId = Integer.valueOf(iqMessage.getParams().get("to").toString());
		Integer fromUserId = userConnection.getUserContext().getUser().getId();
		Integer recordingType = null;
		Integer meetingId = null;
		Short orientation = null;

		Integer guestId = null;
		Recording recording = null;
		Integer groupId = null;

		accessCheckUtil.isVideoRecordingEventAllowed(fromUserId, iqMessage);

		if (Integer.valueOf(iqMessage.getParams().get("chatType")).equals(ChatType.One2One.getId().intValue())) {
			logger.info("iq - startVideoRecording from: " + fromUserId + " and contact: " + toUserId);
		} else {
			// Null check is skipped as it's already verified in the validate method above
			Group group = cacheGroupDao.getGroup(toUserId);
			guestId = group.getGuestUserId();
			groupId = toUserId;

			// Overwrite receiver id with the Guest Id in case of group chats.
			toUserId = guestId;
			logger.info("iq - startVideoRecording from: " + fromUserId + " and group: " + groupId + " and guest: "
					+ toUserId);
		}

		Integer recordingId = null;
		if (iqMessage.getAction().equalsIgnoreCase(IqActionType.StartVideoRecording.getName())) {
			recordingType = Integer.valueOf(iqMessage.getParams().get("recordingType").toString());
			if (iqMessage.getParams().get("recipientFileOrientation") != null) {
				orientation = Short.valueOf(iqMessage.getParams().get("recipientFileOrientation").toString());
			} else {
				orientation = 0;
			}
			meetingId = Integer.valueOf(iqMessage.getParams().get("meetingId").toString());
			Byte machineId = Byte.valueOf(env.getProperty(Constants.MACHINE_ID));
			
			Byte recordingMethod = null;
			
			if (iqMessage.getParams().get("recordingMethod") != null) {
				recordingMethod = Byte.valueOf(iqMessage.getParams().get("recordingMethod").toString());
			} else {
				recordingMethod = RecordingMethod.Regular.getId();
			}
			
			recording = new Recording(fromUserId, toUserId, System.currentTimeMillis(), recordingType.byteValue(),
					RecordingStage.Started.getId(), groupId, meetingId, machineId, recordingMethod);
			recording.setOrientation(orientation);
			recording = recordingService.createVideoRecording(recording);
			recordingId = recording.getId();

			// Add the new recording id to the returning message
			ObjectNode node = Json.newObject();
			node.put("recordingId", recording.getId());
			node.put("machineId", "m" + machineId);
			iqMessage.setBody(node);
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.StopVideoRecording.getName()) ||
				iqMessage.getAction().equalsIgnoreCase(IqActionType.SaveVideoRecording.getName())) {
			recordingId = Integer.valueOf((iqMessage.getParams().get("recordingId").toString()));
			recordingService.updateRecordingStage(recordingId, RecordingStage.Stopped.getId());
		} else if (iqMessage.getAction().equalsIgnoreCase(IqActionType.DiscardVideoRecording.getName())) {
			recordingId = Integer.valueOf((iqMessage.getParams().get("recordingId").toString()));
			recordingService.updateRecordingStage(recordingId, RecordingStage.Discarded.getId());
		}

		iqMessage.setSubtype(IqType.Response.getId());
		JsonNode iqJson = Json.toJson(iqMessage);

		// send message to requester
		logger.info("handledIqRequest : " + iqMessage.getAction());
		RmsMessage message = new RmsMessage(iqJson, RmsMessageType.Out);
		userConnection.getActorRef().tell(message, null);

		// send message to receiver
		userConnectionService.sendMessageToActor(toUserId, iqJson, null);
	}

	private void handleGenericIQMessage(UserConnection userConnection, IqMessage iqMessage) {
		logger.info("handle Generic IQ of type " + iqMessage.getAction());
		Integer toUserId = Integer.valueOf(iqMessage.getParams().get("to").toString());
		iqMessage.setSubtype(IqType.Response.getId());
		JsonNode iqJson = Json.toJson(iqMessage);
		if (Integer.valueOf(iqMessage.getParams().get("chatType")).equals(ChatType.One2One.getId().intValue())) {
			// send message to receiver
			userConnectionService.sendMessageToActor(toUserId, iqJson, null);
		} else {
			// send message to group
			sendMessageToGroup(userConnection, toUserId, iqJson, false);
		}
	}

	private void sendMessageToGroup(UserConnection connection, Integer groupId, JsonNode iqJson,
			boolean sendToCurrentUser) {
		Integer currentUserId = connection.getUserContext().getUser().getId();
		Set<Integer> memberIds = cacheGroupDao.getGroupMembersSet(groupId);
		if (memberIds != null && !memberIds.isEmpty()) {
			if (memberIds.contains(currentUserId) && !sendToCurrentUser) {
				memberIds.contains(currentUserId);
			}
			userConnectionService.sendMessageToActorSet(memberIds, iqJson, null);			
		}
		logger.debug("Completed sending group messages to actors in groupid : " + groupId);
	}


	private void handleGetOrgContacts(UserConnection userConnection, IqMessage iqMessage){
		UserContext userContext = userConnection.getUserContext();
		if (!commonUtil.isMobileChatClient(userContext.getClientId())) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
					"Only Mobile ChatApps can access this API");
		}

		Map<String, String> params = iqMessage.getParams();
		Long lastSyncTime = getLastSyncTime(params);
		Integer organizationId = userContext.getUser().getOrganizationId();
		List<ChatContact> contacts = contactDao.getOrgContacts(organizationId, userContext.getUser().getId(), lastSyncTime);
		createChatContacts(userContext, contacts);
		RmsMessage rmsOut = rmsOut(iqMessage, "orgContacts", contacts);
		userConnection.getActorRef().tell(rmsOut, null);
	}

	private void handleGetGroupContacts(UserConnection userConnection, IqMessage iqMessage){
		UserContext userContext = userConnection.getUserContext();
		if (!commonUtil.isMobileChatClient(userContext.getClientId())) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
					"Only Mobile ChatApps can access this API");
		}

		Map<String, String> params = iqMessage.getParams();
		Long lastSyncTime = getLastSyncTime(params);
		Integer organizationId = userContext.getUser().getOrganizationId();
		List<ChatContact> contacts = contactDao.getOrgGroupContacts(organizationId, userContext.getUser().getId(), lastSyncTime);
		createChatContacts(userContext, contacts);
		RmsMessage rmsOut = rmsOut(iqMessage, "groupContacts", contacts);
		userConnection.getActorRef().tell(rmsOut, null);
	}

	private void handleGetNonOrgContacts(UserConnection userConnection, IqMessage iqMessage){
		UserContext userContext = userConnection.getUserContext();
		if (!commonUtil.isMobileChatClient(userContext.getClientId())) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
					"Only Mobile ChatApps can access this API");
		}

		Map<String, String> params = iqMessage.getParams();
		Long lastSyncTime = getLastSyncTime(params);
		Integer organizationId = userContext.getUser().getOrganizationId();
		List<ChatContact> contacts = contactDao.getNonOrgContacts(organizationId, userContext.getUser().getId(), lastSyncTime);
		createChatContacts(userContext, contacts);
		RmsMessage rmsOut = rmsOut(iqMessage, "nonOrgContacts", contacts);
		userConnection.getActorRef().tell(rmsOut, null);
	}

	private void handleGetGroupAndNonOrgContacts(UserConnection userConnection, IqMessage iqMessage){
		UserContext userContext = userConnection.getUserContext();
		if (!commonUtil.isMobileChatClient(userContext.getClientId())) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
					"Only Mobile ChatApps can access this API");
		}

		Map<String, String> params = iqMessage.getParams();
		Long lastSyncTime = getLastSyncTime(params);
		Integer organizationId = userContext.getUser().getOrganizationId();
		List<ChatContact> nonOrgContacts = contactDao.getNonOrgContacts(organizationId, userContext.getUser().getId(), lastSyncTime);
		List<ChatContact> groupContacts = contactDao.getOrgGroupContacts(organizationId, userContext.getUser().getId(), lastSyncTime);
		ObjectNode node = Json.newObject();
		node.set("nonOrgContacts", Json.toJson(nonOrgContacts));
		node.set("groupContacts", Json.toJson(groupContacts));
		RmsMessage rmsOut = rmsOut(iqMessage, node);
		userConnection.getActorRef().tell(rmsOut, null);
	}

	private long getLastSyncTime(Map<String, String> params) {
		if (params.containsKey("timeRanges") && params.get("timeRanges").trim().length() > 0){
			JsonNode timeRanges = Json.parse(params.get("timeRanges"));
			if (timeRanges.size() > 0){
				JsonNode timeRange = timeRanges.get(0);
				if (timeRange.has("end")){
					//since this old
					return timeRange.get("end").asLong();
				}

			}
		}
		return 0;
	}

	private RmsMessage rmsOut(IqMessage iqMessage, String dataKey, Object data){
		ObjectNode node = Json.newObject();
		node.set(dataKey, Json.toJson(data));
		return rmsOut(iqMessage, node);
	}

	private RmsMessage rmsOut(IqMessage iqMessage, ObjectNode body){
		iqMessage.setBody(body);
		iqMessage.setSubtype(IqType.Response.getId());
		JsonNode iqJson = Json.toJson(iqMessage);
		return new RmsMessage(iqJson, RmsMessageType.Out);
	}
}