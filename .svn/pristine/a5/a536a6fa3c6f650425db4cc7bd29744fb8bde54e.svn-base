package core.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.daos.CacheGroupDao;
import core.daos.CacheUserDao;
import core.daos.ChatSummaryDao;
import core.daos.GroupChatDao;
import core.daos.MeetingInfoDao;
import core.daos.One2OneChatDao;
import core.entities.ChatMessage;
import core.entities.ChatSummary;
import core.entities.Contact;
import core.entities.Group;
import core.entities.GroupChat;
import core.entities.GroupMember;
import core.entities.IqMessage;
import core.entities.MessageReadInfo;
import core.entities.One2OneChat;
import core.entities.User;
import core.entities.UserContext;
import core.exceptions.BadRequestException;
import core.exceptions.ForbiddenException;
import core.exceptions.InternalServerErrorException;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums.ACKType;
import core.utils.Enums.ChatMessageType;
import core.utils.Enums.ChatType;
import core.utils.Enums.ClientType;
import core.utils.Enums.DeleteOption;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.EventNotificationType;
import core.utils.Enums.EventType;
import core.utils.Enums.MessageType;
import core.utils.Enums.NotificationType;
import core.utils.Enums.UserCategory;
import core.utils.Enums.VideoCallStatus;
import core.utils.Enums.GroupType;
import core.utils.PropertyUtil;
import core.utils.ThreadContext;
import messages.UserConnection;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


import play.libs.Json;

@SuppressWarnings("deprecation")
@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {

	final static Logger logger = LoggerFactory.getLogger(ChatHistoryServiceImpl.class);
	@Autowired
	private One2OneChatDao one2OneChatDao;

	@Autowired
	private GroupChatDao groupChatDao;

	@Autowired
	private ChatSummaryDao chatSummaryDao;

	@Autowired
	private CacheGroupDao cacheGroupDao;

	@Autowired
	private UserConnectionService userConnectionService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private EventNotificationBuilder eventNotificationBuilder;

	@Autowired
	private CacheUserDao cacheOrgDao;

	@Autowired
	private CacheUserDao cacheUserDao;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private MeetingInfoDao meetingInfoDao;
	
	@Autowired
	private UserService userService;

	public ChatHistoryServiceImpl() {

	}

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public One2OneChat createOne2OneChatHistory(ChatMessage message) {

		logger.debug("create chat history");
		One2OneChat chat = new One2OneChat();
		chat.setText(message.getText());
		chat.setCreatedDate(message.getUtcDate());
		chat.setFrom(message.getFrom());
		chat.setTo(message.getTo());
		chat.setLastUpdated(message.getUtcDate());
		chat.setActive(true);
		chat.setStatus(false);
		chat.setParentMsgId(message.getParentMsgId());
		Long sysMsgDuration = 0L;
		if (message.getData() != null) {
			chat.setData(message.getData().toString());
		}
		// set ChatMessageType to insert in DB and to send it as push notification
		if (ChatType.One2OneVideoChat.getId().intValue() == message.getSubtype()) {
			chat.setChatMessageType(ChatMessageType.VideoCallMessage.getId());
			message.setChatMessageType(Integer.valueOf(ChatMessageType.VideoCallMessage.getId()));
		} else if (ChatType.One2One.getId().intValue() == message.getSubtype()
				&& ChatMessageType.VideoCallMessage.getId().intValue() == message.getChatMessageType()) {
			chat.setChatMessageType(ChatMessageType.VideoCallMessage.getId());
			message.setChatMessageType(Integer.valueOf(ChatMessageType.VideoCallMessage.getId()));
		} else if (Integer.valueOf(ChatMessageType.SystemMessage.getId()).equals(message.getChatMessageType())) {
			chat.setChatMessageType(ChatMessageType.SystemMessage.getId());
			sysMsgDuration = Long.valueOf(PropertyUtil.getProperty(Constants.SYS_MSG_DURATION_CHECK));
		} else if (Integer.valueOf(ChatMessageType.VideoRecording.getId()).equals(message.getChatMessageType())) {
			chat.setChatMessageType(ChatMessageType.VideoRecording.getId());
		} else if (Integer.valueOf(ChatMessageType.ScreenShot.getId()).equals(message.getChatMessageType())) {
			chat.setChatMessageType(ChatMessageType.ScreenShot.getId());
			chat.setText("Live Screenshot");
			chat.setShowText(true);
		} else if (Integer.valueOf(ChatMessageType.VideoCallMessage.getId()).equals(message.getChatMessageType())) {
			chat.setChatMessageType(ChatMessageType.VideoCallMessage.getId());
		} else {
			chat.setChatMessageType(ChatMessageType.ChatMessage.getId());
			message.setChatMessageType(Integer.valueOf(ChatMessageType.ChatMessage.getId()));
		}
		One2OneChat dbChatMessage = one2OneChatDao.createChatHistory(chat, sysMsgDuration);
		logger.debug("created chat history with id: " + dbChatMessage.getId());
		return dbChatMessage;
	}

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public One2OneChat createOne2OneChatHistory(EventType eventType, ChatMessage message) {
		logger.debug("Adding event data to chat message from " + message.getFrom() + " to " + message.getTo()
		+ " with eventType " + eventType.getId());

		JsonNode messageData = null;
		if (message.getData() != null) {
			messageData = message.getData();
		}

		if (EventType.VideoCallRequest.getId() == eventType.getId()) {
			((ObjectNode) messageData).put("eventType", EventType.VideoCallRequest.getId());
		} else if (EventType.VideoCallEnded.getId() == eventType.getId()) {
			((ObjectNode) messageData).put("eventType", EventType.VideoCallEnded.getId());
		} else if (EventType.NotRegisteredMessage.getId() == eventType.getId()) {
			((ObjectNode) messageData).put("eventType", EventType.NotRegisteredMessage.getId());
		}
		return createOne2OneChatHistory(message);
	}


	/*	@Override
	@Transactional(rollbackFor = { Exception.class })
	public One2OneChat createOne2OneChatHistory(EventType eventType, ChatMessage message) {
		logger.debug("Adding event data to chat message from " + message.getFrom() + " to " + message.getTo()
				+ " with eventType " + eventType.getId());

		JsonNode messageData = null;
		if (message.getData() != null) {
			messageData = message.getData();
		}

		if (EventType.VideoCallRequest.getId() == eventType.getId()) {
			((ObjectNode) messageData).put("eventType", EventType.VideoCallRequest.getId());
		} else if (EventType.VideoCallEnded.getId() == eventType.getId()) {
			((ObjectNode) messageData).put("eventType", EventType.VideoCallEnded.getId());
			JsonNode meetingId = messageData.findPath("meetingId");
			logger.debug("update meeting status to Ended");
			meetingInfoDao.updateMeetingStatus(meetingId.asInt(), VideoCallStatus.Ended.getId().byteValue(),
					message.getFrom());
			logger.debug("updated meeting status to ended for meetingId:" + meetingId.asInt());
		}
		else if (EventType.VideoCallAccepted.getId() == eventType.getId()) {
			JsonNode meetingId = messageData.findPath("meetingId");
			logger.debug("update meeting status to Accepted");
			meetingInfoDao.updateMeetingStatus(meetingId.asInt(), VideoCallStatus.Accepted.getId().byteValue(),
					message.getFrom());
			logger.debug("updated meeting status to accepted for meetingId:" + meetingId.asInt());

		}
		else if (EventType.VideoCallRejected.getId() == eventType.getId()) {
			JsonNode meetingId = messageData.findPath("meetingId");
			logger.debug("update meeting status to Rejected");
			meetingInfoDao.updateMeetingStatus(meetingId.asInt(), VideoCallStatus.Rejected.getId().byteValue(),
					message.getFrom());
			logger.debug("updated meeting status to rejected for meetingId:" + meetingId.asInt());
		}
		else if (EventType.VideoCallConnected.getId() == eventType.getId()) {
			JsonNode meetingId = messageData.findPath("meetingId");
			logger.debug("update meeting status to Connected");
			meetingInfoDao.updateMeetingStatus(meetingId.asInt(), VideoCallStatus.Connected.getId().byteValue(),
					message.getFrom());
			logger.debug("updated meeting status to connected for meetingId:" + meetingId.asInt());
		}
		else if (EventType.VideoCallIgnored.getId() == eventType.getId()) {
			JsonNode meetingId = messageData.findPath("meetingId");
			logger.debug("update meeting status to Ignored");
			meetingInfoDao.updateMeetingStatus(meetingId.asInt(), VideoCallStatus.Missed.getId().byteValue(),
					message.getFrom());
			logger.debug("updated meeting status to ignored for meetingId:" + meetingId.asInt());
		}
		else if (EventType.VideoCallDisconnected.getId() == eventType.getId()) {
			JsonNode meetingId = messageData.findPath("meetingId");
			logger.debug("update meeting status to Disconnected");
			meetingInfoDao.updateMeetingStatus(meetingId.asInt(), VideoCallStatus.Disconnected.getId().byteValue(),
					message.getFrom());
			logger.debug("updated meeting status to disconnected for meetingId:" + meetingId.asInt());
		}
		else if (EventType.VideoCallRating.getId() == eventType.getId()) {
			JsonNode meetingId = messageData.findPath("meetingId");
			logger.debug("update meeting rating");
			meetingInfoDao.updateMeetingRating(meetingId.asInt(), (byte) 3);
			logger.debug("updated meeting rating for meetingId:" + meetingId.asInt());
		}
		else if (EventType.NotRegisteredMessage.getId() == eventType.getId()) {
			((ObjectNode) messageData).put("eventType", EventType.NotRegisteredMessage.getId());
		}
		return createOne2OneChatHistory(message);	
	}*/

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public GroupChat createGroupChatHistory(ChatMessage message, Group inputGroup) {
		
		GroupChat groupChat = createGroupChat(message, inputGroup);
		GroupChat insertedGroupChat = groupChatDao.createGroupChatHistory(groupChat);
		logger.debug("created group chat history with id: " + insertedGroupChat.getId());
		return insertedGroupChat;
	}
	
	
	@Override
	@Transactional(rollbackFor = {Exception.class})
	public GroupChat createVideoKycGroupChatHistory(ChatMessage message, Group inputGroup) {
		
		GroupChat groupChat = createGroupChat(message, inputGroup);
		GroupChat insertedGroupChat = groupChatDao.createVideoKycGroupChatHistory(groupChat);
		logger.debug("created videoKyc group chat history with id: " + insertedGroupChat.getId());
		return insertedGroupChat;
	}

	protected GroupChat createGroupChat(ChatMessage message, Group inputGroup) {
		
		logger.debug("create group chat history");
		Integer memberId = 0;
		Byte eventNotificationType = 0;
		String dataJson = "";
		if (message.getData() != null) {
			dataJson = message.getData().toString();
		}
		// ChatMessageType is set by the caller for video calls (introduced for guests)
		// for
		// all other cases we assume the old behavior of defaulting to a ChatMessage
		// type.
		Byte chatMessageType = null;
		if (message.getChatMessageType() != null) {
			chatMessageType = message.getChatMessageType().byteValue();
		} else {
			chatMessageType = ChatMessageType.ChatMessage.getId();
		}
		GroupChat groupChat = new GroupChat(message.getFrom(), message.getTo(), message.getText(), dataJson,
				chatMessageType, message.getUtcDate(), memberId, eventNotificationType);
		groupChat.setParentMsgId(message.getParentMsgId());
		groupChat.setExcludedRecipients(message.getExcludedRecipients());
		groupChat.setGroupType(inputGroup.getGroupType().intValue());
		setGroupChatMessageTextProperties(groupChat);
		return groupChat;
	}

	@Transactional(rollbackFor = {Exception.class})
	public GroupChat createGroupChatHistory(EventType eventType, Group inputGroup) {
		GroupChat groupChat = null;
		Group group = new Group(inputGroup);
		String data = eventNotificationBuilder.createNotificationBuildingData(eventType, group);
		logger.debug("create group chat history for data " + data + " and eventType " + eventType.getId());
		if (EventType.AddGroup.getId().equals(eventType.getId())
				|| EventType.ChatGuestAdded.getId().equals(eventType.getId())
				|| EventType.ChatCustomerForwardGuestAdded.getId().equals(eventType.getId())) {
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), group.getCreatedById(),
					EventNotificationType.MemeberAdded.getId());
		} else if (EventType.CreateVideoKyc.getId() == eventType.getId()) {
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), inputGroup.getGuestUserId(),
					EventNotificationType.MemeberAdded.getId());
			groupChat.setExcludedRecipients(inputGroup.getGuestUserId().toString());
		} else if (EventType.AddGroup_AddMember.getId() == eventType.getId()) {
			Integer memberAddedId = group.getAffectedMemberId();
			ChatSummary summary = new ChatSummary(new Byte("1"), group.getId(), memberAddedId);
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), memberAddedId,
					EventNotificationType.MemeberAdded.getId());
			chatSummaryDao.create(summary);
		} else if (EventType.UpdateGroup_AddMember.getId() == eventType.getId()) {
			Integer memberAddedId = group.getAffectedMemberId();
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), memberAddedId,
					EventNotificationType.MemeberAdded.getId());
			if (inputGroup.getGroupType().equals(GroupType.VideoKycGuestGroupChat.getId().byteValue())) {
				logger.debug("remove system messages for all members of group");
				groupChat = setExcludedRecipients(groupChat, inputGroup);
			} else if (inputGroup.getGuestUserId() != null) {
				logger.debug("remove system messages for guest");
				groupChat.setExcludedRecipients(inputGroup.getGuestUserId().toString());
			}
		} else if (EventType.CloseGroup.getId() == eventType.getId()) {
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), group.getCreatedById(),
					EventNotificationType.NoEvent.getId());
		} else if (EventType.LeaveGroup.getId() == eventType.getId()) {
			Integer memberLeftId = group.getAffectedMemberId();
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), memberLeftId,
					EventNotificationType.MemberRemoved.getId());
		} else if (EventType.ExitGroup.getId() == eventType.getId()) {
			Integer memberLeftId = group.getAffectedMemberId();
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), memberLeftId,
					EventNotificationType.MemberRemoved.getId());
			if (inputGroup.getGroupType().equals(GroupType.VideoKycGuestGroupChat.getId().byteValue())) {
				logger.debug("remove system messages");
				groupChat = setExcludedRecipients(groupChat, inputGroup);
			} else {
				groupChat.setExcludedRecipients(inputGroup.getGuestUserId().toString());
			}
		} else if (EventType.UpdateGroup_RemoveMember.getId() == eventType.getId()) {
			Integer memberLeftId = group.getAffectedMemberId();
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), memberLeftId,
					EventNotificationType.MemberRemoved.getId());
			if (inputGroup.getGuestUserId() != null) {
				logger.info("Send system message, exclude consumerId :" + inputGroup.getGuestUserId());
				groupChat.setExcludedRecipients(inputGroup.getGuestUserId().toString());
			}
			if (group.getExcludedRecipients() != null) {
				groupChat.setExcludedRecipients(group.getExcludedRecipients());
			}
		} else if (EventType.UpdateGroup_ChangeGroupName.getId() == eventType.getId()) {
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), group.getCreatedById(),
					EventNotificationType.NoEvent.getId());
		} else if (EventType.UpdateGroup_RemoveAdmin.getId() == eventType.getId()) {
			Integer affectedMemberId = group.getAffectedMemberId();
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), affectedMemberId,
					EventNotificationType.NoEvent.getId());
			if (inputGroup.getGuestUserId() != null) {
				logger.info("Send system message, exclude consumerId :" + inputGroup.getGuestUserId());
				groupChat.setExcludedRecipients(inputGroup.getGuestUserId().toString());
			}
		} else if (EventType.UpdateGroup_AddAdmin.getId() == eventType.getId()) {
			Integer affectedMemberId = group.getAffectedMemberId();
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), affectedMemberId,
					EventNotificationType.NoEvent.getId());
			if (inputGroup.getGuestUserId() != null) {
				logger.info("Send system message, exclude consumerId :" + inputGroup.getGuestUserId());
				groupChat.setExcludedRecipients(inputGroup.getGuestUserId().toString());
			}
		} else if (EventType.VideoCallRequest.getId() == eventType.getId()) {
			groupChat = new GroupChat(group.getActionTakerId(), group.getId(), "", data,
					ChatMessageType.VideoCallMessage.getId(), System.currentTimeMillis(), group.getAffectedMemberId(),
					EventNotificationType.NoEvent.getId());
			if (inputGroup.getGroupType().equals(GroupType.VideoKycGuestGroupChat.getId().byteValue())) {
				logger.debug("remove system messages");
				groupChat = setExcludedRecipients(groupChat, inputGroup);
			}
		} else if (EventType.VideoCallEnded.getId() == eventType.getId()) {
			groupChat = new GroupChat(group.getActionTakerId(), group.getId(), "", data,
					ChatMessageType.VideoCallMessage.getId(), System.currentTimeMillis(), group.getAffectedMemberId(),
					EventNotificationType.NoEvent.getId());
			if (inputGroup.getGroupType().equals(GroupType.VideoKycGuestGroupChat.getId().byteValue())) {
				logger.debug("remove system messages");
				groupChat = setExcludedRecipients(groupChat, inputGroup);
			}
			logger.debug("update meeting status to Ended");
			meetingInfoDao.updateMeetingStatus(group.getMeetingId(), VideoCallStatus.Ended.getId().byteValue(),
					group.getActionTakerId());
			logger.debug("updated meeting status to ended for meetingId:" + group.getMeetingId());

		} else if (EventType.OpenChatEnabledInGroup.getId() == eventType.getId()) {
			groupChat = new GroupChat(group.getActionTakerId(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), group.getCreatedById(),
					EventNotificationType.NoEvent.getId());
		} else if (EventType.UpdateGroupBySystem.getId() == eventType.getId()) {
			Integer memberAddedId = group.getAffectedMemberId();

			ChatSummary summary = new ChatSummary(new Byte("1"), group.getId(), memberAddedId);

			chatSummaryDao.create(summary);            
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), memberAddedId,
					EventNotificationType.MemeberAdded.getId());			

		} else if (EventType.UpdateGroupBySystem_RemoveMember.getId() == eventType.getId()) {
			Integer memberLeftId = group.getAffectedMemberId();
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), memberLeftId,
					EventNotificationType.MemberRemoved.getId());         			

		}

		Integer groupType = Integer.valueOf(inputGroup.getGroupType());
		groupChat.setGroupType((groupType));
		if (EventType.AddGroup_AddMember.getId().byteValue() != eventType.getId().byteValue()) {
			if (inputGroup.getGroupType() == 6 || inputGroup.getGroupType() == 7) {
				if (groupChat.getChatMessageType() == ChatMessageType.SystemMessage.getId()) {
					if (inputGroup.getGuestUserId() != null) {
						logger.info("Send system message, exclude consumerId :" + group.getGuestUserId());
						groupChat.setExcludedRecipients(inputGroup.getGuestUserId().toString());
					}
				}
			}
			setGroupChatMessageTextProperties(groupChat);
			GroupChat insertedGroupChat = groupChatDao.createGroupChatHistory(groupChat);
			groupChat.setId(insertedGroupChat.getId());
		}

		logger.debug("created group chat " + groupChat + " for eventType " + eventType.getId());
		return groupChat;
	}


	private GroupChat setExcludedRecipients(GroupChat groupChat, Group inputGroup) {
		List<String> excludedRecipients = new ArrayList<String>();
		excludedRecipients.add(inputGroup.getAffectedMemberId().toString());
		excludedRecipients.add(inputGroup.getGuestUserId().toString());
		String excludeMembers = String.join(",", excludedRecipients);
		groupChat.setExcludedRecipients(excludeMembers);
		return groupChat;
	}

	@Override
	public List<One2OneChat> getOne2OneChatHistory(UserContext userContext, Integer to, Long lastMsgDate,
			Integer offset, Integer limit) {
		logger.debug("get one to one chat history for " + userContext.getUser().getId() + " & " + to);

		User fromUser = cacheService.getUser(userContext.getUser().getId(), false);
		logger.info("user category is: " + fromUser.getUserCategory());
		Boolean isGuest = false;
		if (UserCategory.Guest.getId().intValue() == fromUser.getUserCategory().intValue()) {
			logger.info("user is guest");
			isGuest = true;
		}

		List<One2OneChat> chatList = one2OneChatDao.getOne2OneChatHistory(userContext.getUser().getId(), to, isGuest,
				lastMsgDate, offset, limit);
		Map<Integer, String> fromUserIdNameMap = new HashMap<Integer, String>();
		Integer fromId = null;
		String fromName = null;
		String parentMsgSenderName = null;
		Integer currentUserId = userContext.getUser().getId();
		EventType eventType = null;
		if (chatList != null) {
			for (One2OneChat chat : chatList) {
				fromId = chat.getFrom();
				fromName = getUserName(fromUserIdNameMap, fromId);
				chat.setFromName(fromName);

				if (chat.getParentMsg() != null) {
					parentMsgSenderName = getUserName(fromUserIdNameMap, chat.getParentMsg().getFrom());
					if (parentMsgSenderName != null) {
						chat.getParentMsg().setName(parentMsgSenderName);
					}
				}
				String date = commonUtil.getDateTimeWithTimeZone(chat.getCreatedDate(),
						userContext.getUser().getTimezone());
				chat.setDate(date);

				if (ClientType.isWebClient(ThreadContext.getUserContext().getClientId()) || ClientType.OpenChat
						.getClientId().equalsIgnoreCase(ThreadContext.getUserContext().getClientId())) {// For WEB only
					if (chat.getReadDate() != null) {// Set readDate as JODO for WEB, mobile uses it as "<Long>"
						String readDateStr = commonUtil.getDateTimeWithTimeZone(Long.valueOf(chat.getReadDate()),
								userContext.getUser().getTimezone());
						chat.setReadDateStr(readDateStr);
					}
					Integer dateDiff = commonUtil.getDateDifferenceInDays(chat.getCreatedDate(),
							userContext.getUser().getTimezone());
					chat.setDateDiff(dateDiff);
					// Escape HTML text
					chat.setText(StringEscapeUtils.escapeHtml4(chat.getText()));
				}

				String data = chat.getData();
				ObjectMapper mapper = new ObjectMapper();
				try {
					if (data != null && !data.trim().isEmpty()) {
						JsonNode node = (JsonNode) mapper.readTree(data);
						// Copy the event for use later
						JsonNode eventTypeNode = node.findPath("eventType");
						if (eventTypeNode != null) {
							eventType = EventType.getEventTypeById(eventTypeNode.asInt());
						}
						chat.setDataJson(node);
					}
				} catch (Exception e) {
					logger.warn("some problem with json string. ignore attachment data " + data);
				}

				// Change all video call messages to system messages. This was done as we wanted
				// to avoid have the client apps
				// treat VideoMessages as system messages. At the time we did not want to change
				// the mobile apps.
				if (chat.getChatMessageType().byteValue() == ChatMessageType.VideoCallMessage.getId().byteValue()) {
					chat.setChatMessageType(ChatMessageType.SystemMessage.getId());
				}

				// Build and Set the chatText if message is systemMessgae
				if (chat.getChatMessageType().byteValue() == ChatMessageType.SystemMessage.getId().byteValue()
						&& (chat.getText() == null || "".equalsIgnoreCase(chat.getText().trim()))) {

					// There is a special case for NotRegistered events, which needs to be
					// split into 2 other events depending upon who is asking for this history
					if (eventType == EventType.NotRegisteredMessage) {
						if (chat.getFrom().equals(currentUserId)) {
							eventType = EventType.NotRegisteredMessageSent;
						} else {
							eventType = EventType.NotRegisteredMessageReceived;
						}
					}

					String text = eventNotificationBuilder.buildMessage(eventType, data, fromUser.getId());
					chat.setText(text);
				}
			}
		}
		logger.debug("received one to one chat history of size " + chatList.size());
		return chatList;
	}

	public List<GroupChat> getGroupChatHistory(UserContext userContext, Integer groupId, Long lastMsgDate,
			Integer offset, Integer limit) {
		List<GroupChat> chatList = null;
		Map<Integer, String> fromUserIdNameMap = new HashMap<Integer, String>();
		Integer currentUserId = userContext.getUser().getId();
		logger.debug("get GroupChat chat history for " + currentUserId + " & group " + groupId);

		if (userContext.getGroupShorturl() != null && !userContext.getGroupShorturl().isEmpty()) {
			String shortUrl = userContext.getGroupShorturl();
			hasChatGroupAccess(currentUserId, groupId, shortUrl);
			chatList = groupChatDao.getChatHistoryByShortUri(groupId, offset, limit);
			logger.info("user is accessing chatHistory using shortUrl");
		} else {
			logger.info("User is accessing ChatHistory");
			GroupMember member = cacheGroupDao.getGroupMember(groupId, currentUserId);
			if (member == null) {            	
				logger.info("User is  not in GroupCache, lets check in database ");
				member= userService.getGroupMemberFromGroup(userContext, groupId);
				if(member == null) {             	
					throw new ForbiddenException(ErrorCode.UserNotInGroup, currentUserId, groupId);
				} else {
                      cacheGroupDao.updateGroupMember(groupId, member);
				}
			}
			if (member.getMemberStatus() == 1) {
				chatList = groupChatDao.getChatHistory(groupId, currentUserId, true, lastMsgDate, offset, limit);
			} else {
				chatList = groupChatDao.getChatHistory(groupId, currentUserId, false, lastMsgDate, offset, limit);
			}
		}
	//	chatList = getUserChatHistory(userContext, groupId, lastMsgDate, offset, limit, chatList, currentUserId);
	//	logger.info("got chatList of size:" + chatList.size());

		Integer fromId = null;
		String fromName = null;
		String parentMsgSenderName = null;
		logger.debug("got GroupChat chat history for " + currentUserId + " & group " + groupId + " of size "
				+ (chatList == null ? null : chatList.size()));
		if (chatList != null) {
			for (GroupChat chat : chatList) {
				// From : Sender : user, who posted message on this group
				fromId = chat.getSenderId();
				fromName = getUserName(fromUserIdNameMap, fromId);
				chat.setFromName(fromName);

				// Set from user of the parent message
				if (chat.getParentMsg() != null) {
					parentMsgSenderName = getUserName(fromUserIdNameMap, chat.getParentMsg().getFrom());
					if (parentMsgSenderName != null) {
						chat.getParentMsg().setName(parentMsgSenderName);
					}
				}

				if (chat.getChatMessageType() != null && chat.getChatMessageType().byteValue() == ChatMessageType.ScreenShot.getId().byteValue()) {
					logger.info("Chat MessageType is Screenshot: " + chat.getChatMessageType() + "& MessageId is : " + chat.getId() + "& text is: " + chat.getText());
				}

				// Set date for web, mobile uses utcDate
				String date = commonUtil.getDateTimeWithTimeZone(chat.getCreatedDate(),
						userContext.getUser().getTimezone());
				chat.setDate(date);

				if (ClientType.isWebClient(ThreadContext.getUserContext().getClientId())) {// For WEB only
					if (chat.getReadDate() != null) {// Set readDate as JODO for WEB, mobile uses it as "<Long>"
						String readDateStr = commonUtil.getDateTimeWithTimeZone(Long.valueOf(chat.getReadDate()),
								userContext.getUser().getTimezone());
						chat.setReadDateStr(readDateStr);
					}
					Integer dateDiff = commonUtil.getDateDifferenceInDays(chat.getCreatedDate(),
							userContext.getUser().getTimezone());
					chat.setDateDiff(dateDiff);
					// Escape HTML text
					chat.setText(StringEscapeUtils.escapeHtml4(chat.getText()));
				}

				// Change all video call messages to system messages. This was done as we wanted
				// to avoid have the client apps
				// treat VideoMessages as system messages. At the time we did not want to change
				// the mobile apps.
				if (chat.getChatMessageType().byteValue() == ChatMessageType.VideoCallMessage.getId().byteValue()) {
					chat.setChatMessageType(ChatMessageType.SystemMessage.getId());
				}

				// Build and Set the chatText if message is systemMessgae
				String data = chat.getData();
				if (data != null && !"".equals(data)
						&& chat.getChatMessageType().byteValue() == ChatMessageType.SystemMessage.getId().byteValue()) {
					// String message = eventNotificationBuilder.buildMessage(null, data,
					// currentUserId);
					String message = eventNotificationBuilder.buildMessage(chat.getCreatedDate(), null, data,
							currentUserId, userContext);
					// message
					/*
					 * String msg = getOsBrowserVersionSpecificMessage("windows", "chrome", 70);
					 *
					 * message = message + msg;
					 */
					if (message != null && !"".equals(message)) {
						chat.setText(message);
					}

					data = null;
					chat.setData(null);
				}

				if (chat.getChatMessageType() != null && chat.getChatMessageType().byteValue() == ChatMessageType.ScreenShot.getId().byteValue()) {
					logger.info("Chat MessageType is Screenshot: " + chat.getChatMessageType() + "& MessageId is : " + chat.getId() + "& text is: " + chat.getText());
				}

				if (data != null && !data.isEmpty()) {
					ObjectMapper mapper = new ObjectMapper();
					try {
						JsonNode node = (JsonNode) mapper.readTree(data);
						chat.setDataJson(node);
					} catch (Exception e) {
						logger.info("some problem with json string. ignore attachment data " + data);
					}
				}
			}
		}
		logger.debug("returning GroupChat history of size " + ((chatList == null) ? 0 : chatList.size()));
		return chatList;
	}

	private void hasChatGroupAccess(Integer currentUserId, Integer groupId, String shortUrl) {

		logger.info("check chat group access for:" + currentUserId + ", groupId:" + groupId + ", groupShortUri:"
				+ shortUrl);

		Group group = cacheService.getGroupDetails(groupId);
		logger.info("Got group Details for id:" + groupId + " from cache");
		if (!group.getShortUri().equals(shortUrl)) {
			throw new ForbiddenException(ErrorCode.Forbidden, "GroupShortUri from client and cache mismatch");
		}

		if (group.getGroupType() != GroupType.GuestGroupChat.getId().byteValue()
				&& group.getGroupType() != GroupType.GuestDirectGroupChat.getId().byteValue()
				&& group.getGroupType() != GroupType.VideoKycGuestGroupChat.getId().byteValue()) {
			throw new ForbiddenException(ErrorCode.Forbidden, "GroupChatAccess is allowed only for GuestGroupChat");
		}

		if (!cacheService.isInContact(group.getCreatedById(), currentUserId)) {
			throw new ForbiddenException(ErrorCode.Forbidden, "Group creator and current user are not in same org");
		}

		logger.debug("GroupCreator and CurrentUser are in sameOrg");
		Integer creatorOrgId = cacheService.getUser(group.getCreatedById(), false).getOrganizationId();
		logger.debug("got creatorOrgId:" + creatorOrgId + " from cache");
		JsonNode orgDetails = cacheService.getOrgJson(creatorOrgId);
		logger.debug("got creatorOrgDetails from cache");
		Boolean isEnableGuestChatHistory = false;
		JsonNode jsonNodes = orgDetails.findPath("settings");
		if (jsonNodes != null) {
			for (JsonNode jsonNode : jsonNodes) {
				if (jsonNode.get("preference") != null
						&& jsonNode.get("preference").asText().contentEquals("EnableViewGuestChatHistory")) {
					isEnableGuestChatHistory = jsonNode.get("value").asBoolean();
					break;
				}
			}
		}
		logger.debug("isEnableGuestChatHistory:" + isEnableGuestChatHistory);

		if (!isEnableGuestChatHistory) {
			throw new ForbiddenException(ErrorCode.Forbidden, "EnableGuestChatHistory Org preference is false");
		}

		logger.debug("EnableGuestChatHistory is true for Org:" + creatorOrgId);
	}

	private String getUserName(Map<Integer, String> fromUserIdNameMap, Integer fromId) {
		String fromName = null;
		if (fromUserIdNameMap.containsKey(fromId)) {
			fromName = fromUserIdNameMap.get(fromId);
		} else {
			JsonNode fromUserJson = cacheUserDao.find(fromId);
			fromName = fromUserJson.findPath("firstName").asText();
			fromUserIdNameMap.put(fromId, fromName);
		}
		return fromName;
	}

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public void updateWelcomeMessage(GroupChat groupChat) {
		logger.debug("update welcomeMessage chat message  started fro groupId" + groupChat.getGroupId());
		groupChatDao.updateWelcomeMessage(groupChat);

	}

	@Override
	public List<ChatSummary> getUnReadMsgsPerContact(Integer to) {
		logger.debug("get unread messages user count for to " + to);
		List<ChatSummary> contactUnReadMsgs = chatSummaryDao.getUnReadMsgsPerContact(to);
		logger.debug("return unread messages user count : " + contactUnReadMsgs);
		return contactUnReadMsgs;
	}

	@Override
	public List<ChatSummary> getUnReadMsgContact(List<Integer> userIds, Integer offset, Integer limit) {
		logger.debug("get unread message-contact for users " + userIds);
		List<ChatSummary> chatUnreadCounts = null;
		if (userIds != null && !userIds.isEmpty()) {
			chatUnreadCounts = chatSummaryDao.getUnReadMsgContact(userIds, offset, limit);
		}
		logger.debug("got unread message-contact as " + chatUnreadCounts);
		return chatUnreadCounts;
	}

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public void updateChatReadStatus(Integer toUserGroupId, ChatType chatType, MessageType messageType,
			UserContext context) {
		Integer currentUserId = context.getUser().getId();
		logger.info("mark status read for users/groups chat sent to " + toUserGroupId + " for user " + currentUserId);
		if (ChatType.One2One.getId().byteValue() == chatType.getId().byteValue()) {
			Long msgId = one2OneChatDao.getLastMsgIdBySender(toUserGroupId, currentUserId);
			if (msgId != null) {
				List<ChatMessage> msgs = updateChatReadStatusV2(context, msgId.toString(), "");
				sendReadACKMsgs(context, msgs);
			}
		} else if (ChatType.GroupChat.getId().byteValue() == chatType.getId().byteValue()) {
			Long msgId = groupChatDao.getLastMsgIdByGroup(toUserGroupId, currentUserId);
			if (msgId != null) {
				List<ChatMessage> msgs = updateChatReadStatusV2(context, "", msgId.toString());
				sendReadACKMsgs(context, msgs);
			}
		}

		logger.info("marked status read for users/groups " + toUserGroupId + " chat sent to user " + currentUserId);

	}

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public List<ChatMessage> updateChatReadStatusV2(UserContext context, String one2oneMsgIds, String groupMsgIds) {
		Integer currentUserId = context.getUser().getId();
		logger.info("mark status read for users/groups chat sent to user " + currentUserId);
		List<ChatMessage> msgs = chatSummaryDao.updateChatReadStatusV2(currentUserId, one2oneMsgIds, groupMsgIds);
		return msgs;
	}

	@Override
	public void sendReadACKMsgs(UserContext context, List<ChatMessage> msgs) {
		Integer currentUserId = context.getUser().getId();
		Set<String> supportedClientIds = commonUtil.getOne2oneChtSupportedClients();
		// Not sending ACK to IQ requester client connection.
		if (context.getClientId() != null) {
			supportedClientIds.remove(context.getClientId());
		}
		List<String> clientIds = new ArrayList<String>(supportedClientIds);
		sendReadACK(context, msgs, clientIds);
		logger.info("marked status read for users/groups chat sent to user " + currentUserId);
	}

	@Override
	public void deleteChatMessages(UserContext context, List<Integer> msgIds, Integer contactId, Integer deleteOption,
			Integer chatType) {
		ChatMessage msg = new ChatMessage();
		msg.setChatType(chatType);
		msg.setType(MessageType.ACK.getId());
		msg.setSubtype(ACKType.DeleteChatMessage.getId().intValue());
		JsonNode message = Json.toJson(msg);
		ObjectNode node = (ObjectNode) message;
		node.put("mids", Json.toJson(msgIds));
		node.put("contactId", contactId);
		List<String> clientIds = new ArrayList<String>();
		if (ChatType.One2One.getId().intValue() == chatType.intValue()) {
			clientIds.addAll(commonUtil.getOne2oneChtSupportedClients());
		} else {
			clientIds.addAll(commonUtil.getGroupChtSupportedClients());
		}
		String smsgIds = StringUtils.join(msgIds, ',');
		boolean status = chatSummaryDao.deleteChatMessages(context.getUser().getId(), smsgIds,
				contactId, deleteOption, chatType, System.currentTimeMillis());
		if (!status) {
			throw new BadRequestException(ErrorCode.Invalid_iqMessage_Data,
					ErrorCode.Invalid_iqMessage_Data.getName() + " for field : mids = " + smsgIds);
		}
		// send delete chat message acknowledgement
		if (deleteOption.intValue() == DeleteOption.DeleteMessageForMe.getId().intValue()) {
			userConnectionService.sendMessageToActor(context.getUser().getId(), message, null, clientIds);
			List<Integer> recipients = new ArrayList<>();
			recipients.add(context.getUser().getId());
			sendPushNotificationForDeleteMessage(message, recipients);
		} else if (deleteOption.intValue() == DeleteOption.DeleteMessageForAll.getId().intValue()) {
			userConnectionService.sendMessageToActor(context.getUser().getId(), message, null, clientIds);
			List<Integer> recipients = new ArrayList<>();
			recipients.add(context.getUser().getId());
			sendPushNotificationForDeleteMessage(message, recipients);
			if (ChatType.One2One.getId().intValue() == chatType.intValue()) {
				ObjectNode tempNode = (ObjectNode) message;
				tempNode.put("contactId", context.getUser().getId());
				userConnectionService.sendMessageToActor(contactId, message, null, clientIds);
				recipients = new ArrayList<>();
				recipients.add(contactId);
				sendPushNotificationForDeleteMessage(message, recipients);
			} else {
				/*
				 * Group group = cacheGroupDao.getGroup(contactId); if (group.getMembers() !=
				 * null && !group.getMembers().isEmpty()) { for (GroupMember member :
				 * group.getMembers()) { if (!member.getId().equals(context.getUser().getId()))
				 * { userConnectionService.sendMessageToActor(member.getId(), message, null,
				 * clientIds); sendPushNotificationForDeleteMessage(message, member.getId()); }
				 * } }
				 */
				Set<Integer> members = cacheGroupDao.getGroupMembersSet(contactId);
				userConnectionService.sendMessageToActorSet(members, message, clientIds);
				sendPushNotificationForDeleteMessage(message, new ArrayList<>(members));
			}
		}
	}


	@Override
	@Transactional(rollbackFor = {Exception.class})
	public void deleteChatHistory(Integer deletedUserId) {
		chatSummaryDao.updateChatReadStatusV2(deletedUserId, "-1", "-1");
	}

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public void UpdateOne2OneChatReadStatus(Integer to, Long mid) {
		logger.info("mark status read for chat " + mid);
		one2OneChatDao.UpdateOne2OneChatReadStatus(to, mid);
		logger.info("marked read status " + mid);
	}

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public void deactivateChatSummary(Integer deletedUserId) {
		logger.info("mark status read for users chat sent by " + deletedUserId);
		chatSummaryDao.deactivateOne2OneChatSummary(deletedUserId);
	}

	@Override
	public List<ChatMessage> getChatHistory(UserConnection userConnection, IqMessage iqMessage) {
		List<ChatMessage> chatHistory = null;
		logger.info("getting Chat History as requested by iqMessage " + iqMessage);
		UserContext userContext = userConnection.getUserContext();
		logger.debug("getting Chat History as requested by iqMessage " + iqMessage + " received userContext "
				+ userContext.toString());
		if (!commonUtil.isMobileChatClient(userContext.getClientId())) {
			// throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "Only Mobile
			// ChatApps can access this API : " + IqActionType.GetChatHistory);
		}
		Integer offset = ((iqMessage.getParams().get("offset") != null)
				? Integer.valueOf(iqMessage.getParams().get("offset"))
						: 0);
		Integer limit = ((iqMessage.getParams().get("limit") != null)
				? Integer.valueOf(iqMessage.getParams().get("limit"))
						: 500);

		// Get sync Dates
		List<String> syncDateStr = new ArrayList<String>();
		if (iqMessage.getParams().get("timeRanges") != null) {
			JsonNode syncDates = Json.parse(iqMessage.getParams().get("timeRanges"));
			for (int count = 0; count < syncDates.size(); count++) {
				syncDateStr.add(((syncDates.get(count).get("end") != null) ? syncDates.get(count).get("end").asLong()
						: "0000000000000") + ":" + syncDates.get(count).get("start").asLong());
			}
		}
		logger.debug("Date Range to get messgaes " + syncDateStr.toString());
		// Check if it is first time sync
		Boolean isFirstTimeSync = iqMessage.getParams().get("isFirstTimeSync") != null
				? Boolean.valueOf(iqMessage.getParams().get("isFirstTimeSync"))
						: false;
				logger.debug("isFirstTimeSync flag in get messgaes is " + isFirstTimeSync);
				// Get the per User messages to be synced
				Integer perUserMsgCout = null;
				if (isFirstTimeSync) {
					if (iqMessage.getParams().get("perUserMsgCout") != null) {
						perUserMsgCout = Integer.valueOf(iqMessage.getParams().get("perUserMsgCout"));
					} else {
						// TODO get it from ORG preferences
						perUserMsgCout = getPerMsgUserCountForOrg(userContext.getUser().getOrganizationId());
						if (perUserMsgCout == null) {
							perUserMsgCout = 50;
						}
					}
				} else {
					perUserMsgCout = 50;
				}
				logger.debug("considering  perUserMsgCout " + perUserMsgCout);
				chatHistory = chatSummaryDao.getChatHistory(userContext, offset, limit, StringUtils.join(syncDateStr, ','),
						isFirstTimeSync, perUserMsgCout);
				logger.info("got Chat History size as " + (chatHistory == null ? null : chatHistory.size()));
				return chatHistory;
	}

	@Override
	@Transactional(rollbackFor = {Exception.class})
	public void createGroupChatMsgRecipient(Integer groupId, Integer ExcludeConsumerId) {
		logger.info("insert into group chat msg recipient for groupId: " + groupId);
		groupChatDao.createGroupChatMsgRecipient(groupId, ExcludeConsumerId);
		logger.info("chat msg recipient created for groupId: " + groupId);
	}

	@Override
	public List<Contact> getUnReadMsgsForContacts(Integer loggedInUserId, List<Contact> contacts) {
		List<Integer> userIds = new ArrayList<Integer>();
		List<Integer> groupIds = new ArrayList<Integer>();
		List<Contact> outContacts = new ArrayList<Contact>();
		for (Contact contact : contacts) {
			if (ChatType.One2One == ChatType.getChatTypeById(contact.getChatType().intValue())) {
				userIds.add(contact.getId());
			} else if (ChatType.GroupChat == ChatType.getChatTypeById(contact.getChatType().intValue())) {
				groupIds.add(contact.getId());
			}
		}
		logger.debug("Get chatSummaries for One2One " + userIds + " and for Groups " + groupIds);
		List<ChatSummary> chatSummaries = chatSummaryDao.getUnReadMsgsForContacts(loggedInUserId,
				((userIds.isEmpty()) ? null : userIds), ((groupIds.isEmpty()) ? null : groupIds));
		logger.debug("Got chatSummaries for One2One " + userIds + " and for Groups " + groupIds);

		Map<String, ChatSummary> chatSummaryMap = new HashMap<>();
		for (ChatSummary chat : chatSummaries) {
			if (ChatType.One2One == ChatType.getChatTypeById(chat.getChatType().intValue())) {
				chatSummaryMap.put(ChatType.One2One.name() + chat.getContactId(), chat);
			} else if (ChatType.GroupChat == ChatType.getChatTypeById(chat.getChatType().intValue())) {
				chatSummaryMap.put(ChatType.GroupChat.name() + chat.getContactId(), chat);
			}
		}
		for (Contact contact : contacts) {
			logger.debug("Processing contact " + contact.getId());
			Contact outContact = new Contact();
			outContact.setId(contact.getId());
			outContact.setChatType(contact.getChatType());
			outContact.setContactType(null);
			outContact.setUnreadMsg(null);
			outContacts.add(outContact);
			ChatSummary chat = null;
			if (ChatType.One2One.getId().byteValue() == contact.getChatType().byteValue()) {
				if (chatSummaryMap.containsKey(ChatType.One2One.name() + outContact.getId())) {
					chat = chatSummaryMap.get(ChatType.One2One.name() + outContact.getId());
					chat.setContactId(null);
					chat.setChatType(null);
				} else {
					chat = new ChatSummary();
					chat.setUnReadMsgCount(Short.valueOf("0"));
				}
			} else if (ChatType.GroupChat.getId().byteValue() == contact.getChatType().byteValue()) {
				if (chatSummaryMap.containsKey(ChatType.GroupChat.name() + outContact.getId())) {
					chat = chatSummaryMap.get(ChatType.GroupChat.name() + outContact.getId());
					chat.setContactId(null);
					chat.setChatType(null);
				} else {
					chat = new ChatSummary();
					chat.setUnReadMsgCount(Short.valueOf("0"));
				}
			}
			outContact.setChatSummary(chat);
			logger.debug("Processed contact " + contact.getId());
		}

		return outContacts;
	}

	@Override
	public List<Contact> getRecipientUnreadCount(Integer recipientId) {
		List<ChatSummary> chatSummaries = chatSummaryDao.getRecipientUnreadCount(recipientId);
		return chatSummaries.stream().map(cs->{
			Contact outContact = new Contact();
			outContact.setId(cs.getContactId());
			outContact.setChatType(cs.getChatType());
			outContact.setContactType(null);
			outContact.setUnreadMsg(null);
			ChatSummary chatSummary = new ChatSummary();
			chatSummary.setUnReadMsgCount(cs.getUnReadMsgCount());
			outContact.setChatSummary(chatSummary);
			return outContact;
		}).collect(Collectors.toList());
	}

	/**
	 * @param organizationId
	 * @return
	 */
	private Integer getPerMsgUserCountForOrg(Integer organizationId) {
		logger.info("get Per Msg User Count for organizationId: " + organizationId);
		Integer perUserMsgCout = null;
		try {
			JsonNode orgNode = cacheOrgDao.find(organizationId);
			JsonNode prefs = orgNode.findPath("settings");
			for (JsonNode pref : prefs) {
				if (Constants.CHAT_HISTORY_PER_USER_MSG_CNT.equalsIgnoreCase(pref.findPath("preference").asText())) {
					perUserMsgCout = Integer.parseInt(pref.findPath("value").asText());
					break;
				}
			}
		} catch (Exception e) {
			logger.warn("Faild to get Per Msg user count", e);
		}
		logger.info("got Per Msg User Count for organizationId: " + organizationId + " as " + perUserMsgCout);
		return perUserMsgCout;
	}

	@Override
	public List<MessageReadInfo> getChatMsgReadInfo(Long msgId, Integer currentUserId, Byte chatType) {
		List<MessageReadInfo> msgReadInfoList = new ArrayList<MessageReadInfo>();
		if (ChatType.GroupChat.getId().equals(chatType)) {
			msgReadInfoList = groupChatDao.getGroupChatMsgReadInfo(msgId, currentUserId);
		} else if (ChatType.One2One.getId().equals(chatType)) {
			One2OneChat chatMsg = one2OneChatDao.getMsgById(msgId, currentUserId);
			if (chatMsg != null) {
				MessageReadInfo readInfo = new MessageReadInfo(chatMsg.getTo(), chatMsg.getReadDate(), null);
				msgReadInfoList.add(readInfo);
			}
		}

		Long readDate = null;
		for (MessageReadInfo messageReadInfo : msgReadInfoList) {
			readDate = messageReadInfo.getReadDate();
			if (readDate != null && readDate > 0) {
				String readDateStr = commonUtil.getDateTimeWithTimeZone(Long.valueOf(readDate),
						Constants.TIMEZONE_INDIA);
				messageReadInfo.setReadDateStr(readDateStr);
			}
		}
		return msgReadInfoList;
	}

	private void sendReadACK(UserContext context, List<ChatMessage> msgs, List<String> clientIds) {
		for (ChatMessage msg : msgs) {
			ChatMessage msgForACK = new ChatMessage(msg.getChatType(), msg.getFrom(), msg.getTo(), msg.getMid(),
					msg.getReadDate());
			if (msg.getChatType() == 1) {
				logger.info("message chat Type:" + msg.getChatType());
				Integer groupId = msg.getTo();
				logger.info("groupId:" + groupId);
				Group group = cacheGroupDao.getGroup(groupId);
				if (GroupType.VideoKycGuestGroupChat.getId() == group.getGroupType().intValue()) {
					msg.setGroupType(group.getGroupType());
					logger.info("message:" + msg);
				}
			}
			if (ClientType.isWebClient(context.getClientId())) {// For WEB only
				if (msgForACK.getReadDate() != null) {// Set readDate as JODO for WEB, mobile uses it as "<Long>"
					String readDateStr = commonUtil.getDateTimeWithTimeZone(Long.valueOf(msgForACK.getReadDate()),
							context.getUser().getTimezone());
					msgForACK.setReadDateStr(readDateStr);
				}
			}
			msgForACK.setType(MessageType.ACK.getId());
			msgForACK.setSubtype(ACKType.ReadACK.getId().intValue());
			JsonNode message = Json.toJson(msgForACK);
			// ReadDate will be null when all of the recipients have not read the msg for
			// groupChat.
			if (msgForACK.getReadDate() != null) {
				logger.debug("Sending message To Actor with userId " + msgForACK.getFrom());
				userConnectionService.sendMessageToActor(msgForACK.getFrom(), message, null);
			}
			logger.debug("Sending message To Actor with userId " + context.getUser().getId());
			Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(context.getUser().getId(),
					message, null, clientIds);
			if (!(msgReceiverClients.contains(ClientType.AndroidChatApp.getClientId())
					|| msgReceiverClients.contains(ClientType.iOSChatApp.getClientId()))) {
				sendPushNotificationForBadge(msgForACK, context.getUser().getId());
			}
			logger.info("message sent To Actors, read-status updated");
		}
	}

	private void sendPushNotificationForBadge(ChatMessage msg, Integer currentUserId) {
		ObjectNode pushNotification = Json.newObject();
		ObjectNode aps = Json.newObject();
		pushNotification.put("aps", aps);
		List<Integer> recipients = new ArrayList<>();
		recipients.add(currentUserId);
		notificationService.sendMobilePushNotification(recipients, pushNotification, NotificationType.Chat);
	}

	private void sendPushNotificationForDeleteMessage(JsonNode msg, List<Integer> recipients) {
		ObjectNode pushNotification = Json.newObject();
		ObjectNode aps = Json.newObject();
		aps.put("content-available", "1");
		pushNotification.put("aps", aps);
		ObjectNode data = Json.newObject();
		data.put("type", MessageType.Notification.getId());
		data.put("subtype", NotificationType.DeleteMessage.getId());
		data.put("mids", msg.get("mids"));
		data.put("chatType", msg.get("chatType").intValue());
		data.put("contactId", msg.get("contactId").intValue());
		pushNotification.put("data", data);
		notificationService.sendMobilePushNotification(recipients, pushNotification, NotificationType.DeleteMessage);
	}

	private void setGroupChatMessageTextProperties(GroupChat message) {
		logger.debug("set GroupChatMessageTextProperties");
		byte value = message.getChatMessageType().byteValue();
		String text = null;
		if (value == ChatMessageType.VideoRecording.getId()) {
			text = "Live Recording";
		} else if (value == ChatMessageType.ScreenShot.getId()) {
			text = "Live Screenshot";
		}
		if (text != null) {
			message.setText(text);
			message.setShowText(true);
		}
		logger.debug("GroupChatMessageTextProperties set");
	}
}
