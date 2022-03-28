package core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.workapps.common.core.services.DataEncryptionService;
import core.daos.*;
import core.entities.*;
import core.exceptions.ApplicationException;
import core.exceptions.BadRequestException;
import core.exceptions.ForbiddenException;
import core.utils.ActorThreadContext;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums.*;
import core.utils.PropertyUtil;
import messages.UserConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import play.libs.Json;

import java.util.*;



@SuppressWarnings("deprecation")
@Service
@Transactional(rollbackFor = { Exception.class })
public class GroupChatMessageServiceImpl implements GroupChatMessageService {
	final static Logger logger = LoggerFactory.getLogger(GroupChatMessageServiceImpl.class);

	@Autowired
	@Qualifier("CacheImpl")
	private CacheConnectionInfoDao cacheConnectionInfoDao;

	@Autowired
	private ChatHistoryService chatHistoryService;

	@Autowired
	private UserConnectionService userConnectionService;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private DataEncryptionService dataEncryptionService;

	@Autowired
	private CacheUserDao cacheUserDao;

	@Autowired
	private CacheGroupDao cacheGroupDao;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private GroupChatDao groupChatDao;

	@Autowired
	private PresenceService presenceService;

	@Autowired
	private EventNotificationBuilder eventNotificationBuilder;

	@Autowired
	private MeetingInfoDao meetingInfoDao;
	
	@Autowired
	private EventTrackingService eventTrackingService;

	@Override
	public void sendMessage(UserConnection connection, ChatMessage message) {
		logger.info("In sendMessage, ChatMessage is valid. type = " + message.getType() + ", Subtype = "
				+ message.getSubtype());
		try {
			if (message.getType() == MessageType.Chat.getId().intValue()) {
				message.setFrom(connection.getUserContext().getUser().getId());
				message.setName(connection.getUserContext().getUser().getName());
				sendGroupChatMessage(connection, message);
			} else if (message.getType() == MessageType.Typing.getId().intValue()
					|| message.getType() == MessageType.StopTyping.getId().intValue()) {
				sendGroupTypingMessage(connection, message);
			} else if ((message.getType() == MessageType.ACK.getId().intValue())) {
				chatHistoryService.updateChatReadStatus(message.getFrom(), ChatType.GroupChat, MessageType.ACK,
						connection.getUserContext());
				logger.debug("Completed UpdateChatReadStatus, ACK/Chat Message :" + message.getText());
			} else {
				throw new BadRequestException(ErrorCode.UnsupportedTypeSubtype,
						ErrorCode.UnsupportedTypeSubtype.getName());
			}
			logger.debug("sendMessage completed successfully");
		} catch (Exception ex) {
			throw ex;
		}
	}

	@Override
	public void sendAddGroupEventMessage(Event event, Group group, GroupChat groupChat) {
		logger.info("send AddGroup EventMessage to groupMembers for group : " + group.getName());
			
		List<Integer> groupMembers = cacheGroupDao.getGroupMembers(group);
		if (groupMembers != null && !groupMembers.isEmpty()) {
			logger.debug("Received groupMembers for group : " + group.getName() + " as " + groupMembers.toString());
			// Fixed "AddGroup" type is used because guest groups are created with event
			// "ChatGuestAdded", but the client apps
			// expect that the event type will be "AddGroup"
			
			PushNotificationVisibility visibility = PushNotificationVisibility.Recipients;
			Integer notificationType = NotificationType.AddGroup.getId().intValue();			
			decryptName(group, null);

			// TODO: improve message target. Can't send in bulk on channel as message is
			// different for different people
			List<String> clientIds = CommonUtil.getSupportedClientIds(ChatType.GroupChat);
			event.setType(EventType.AddGroup.getId());
			ObjectNode node = getJsonEventNode(event);
			
			Integer createdBydId = group.getCreatedById();
			User user = null;
			for (Integer receipinetMemberId : groupMembers) {// send message to all group members
				List<Integer> pushList = new ArrayList<>();
				String message = "";
				if(GroupType.VideoKycGuestGroupChat.getId() == group.getGroupType().intValue()){
					logger.debug("Received customerAssigned for group : " + group.getName());
					user = cacheService.getUser(receipinetMemberId, false);
					message = getCustomerAssignedMessage(group.getName(), groupChat.getCreatedDate(),
							user.getTimezone());
					visibility = PushNotificationVisibility.All;
					notificationType = NotificationType.AddGroupMember.getId();
					event.setType(EventType.UpdateGroup.getId());
					node = getJsonEventNode(event);
					
					JsonNode videoKyc =cacheService.getVideoKycInfo(group.getGuestUserId());
					node.put("videoKycFlow", videoKyc.get("flow").asText());
					createdBydId = resolveCreatorId(event, createdBydId);
					logger.debug("Affected member id: " + group.getAffectedMemberId());
					node.put("affMemId", group.getAffectedMemberId());

				} else if (group.getGuestUserId() != null) {
					user = cacheService.getUser(receipinetMemberId, false);
					message = getGuestChatGroupAddedMessage(group.getName(), groupChat.getCreatedDate(),
							user.getTimezone());
					visibility = PushNotificationVisibility.All;
				} else {
					message = eventNotificationBuilder.buildMessage(EventType.AddGroup, groupChat.getData(),
							receipinetMemberId);
				}
				node.put("groupType", group.getGroupType());
				node.put("grpId", group.getId());
				node.put("grpCrtId", group.getCreatedById());
				
				
				if(group.getGuestUserId() != null) {
					node.put("guestId", group.getGuestUserId());
				}
				//	node.put("groupName", group.getName());
				
				node.put("groupName", group.getName());
				JsonNode userJson = cacheUserDao.find(createdBydId);
				String name = userJson.findPath("firstName").asText();
				if (name != null) {
					node.put("name", name);
				}
				node.put("text", message);
				node.put("visibility", visibility.getId());
				// TODO Duplicated in eventservice
				
				addSystemChatMsgToEventNode(node, groupChat);// TODO Duplicated in eventservice
				logger.debug("Sending AddGroup EventMessage to " + receipinetMemberId + " as " + node.toString());
				ChatMessage chatMessage = Json.fromJson(node, ChatMessage.class);
				chatMessage.setTo(group.getId());
				chatMessage.setFrom(group.getCreatedById());
				
				chatMessage.setChatType(ChatType.GroupChat.getId().intValue());
				chatMessage.setChatMessageType(ChatMessageType.SystemMessage.getId().intValue());
				Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(receipinetMemberId, node,
						null, clientIds);
				Boolean isSendPN = commonUtil.isAlwaysSendPushNofication(chatMessage, msgReceiverClients);
				logger.debug("isSendPN flag = " + isSendPN);
				logger.info("group.getGuestUserId(): "+group.getGuestUserId()+" receipinetMemberId: "+receipinetMemberId);
				if (isSendPN && (group.getGuestUserId() != receipinetMemberId)) {// send push notification to
					// all except group guests
					logger.info("send pushNotification to: "+receipinetMemberId);
					pushList.add(receipinetMemberId);
					Long utcDate = groupChat.getCreatedDate();
					Long mid = groupChat.getId();
					logger.info("createdBydId:"+createdBydId);
					notificationService.sendMobileNotification(notificationType, chatMessage, pushList, visibility);
					logger.info("called sent mobile push notification for member = " + receipinetMemberId);
				}
			}
		} else {
			logger.warn("Received no groupMembers for group : " + group.getName()
			+ " so not sending AddGroup EventMessage to groupMembers ");
		}
	}

	private Integer resolveCreatorId(Event event, Integer createdBydId) {
		logger.info("set CreatedById to agentId for AgentAssigned event");
		try {	
		Map<String, String> props = event.getData();
		logger.info("props:"+props.toString());
		String eventStatus = props.get("status");
		logger.info("eventStatus:"+eventStatus);
		if(eventStatus != null && !eventStatus.isBlank()) {
			Byte status = Byte.valueOf(eventStatus);
			logger.info("got eventStatus in Bytes:"+status);
			if (VideoKYCStatus.AgentAssigned.getId().equals(status)) {
				createdBydId = Integer.valueOf(props.get("agentId"));
				logger.info("AgentAssigned...agentId:"+createdBydId);
			}
		}
		}
		catch(Exception e) {
			logger.error("error while get event data",e);
			return createdBydId;
		}
		logger.info("return creatorId");
		return createdBydId;
	}

	@Override
	@Transactional(rollbackFor = { Exception.class })
	public GroupChat updateDocType(Integer messageId, Integer attachmentId, Integer docType, String docTypeText, Integer documentPurpose, String documentPurposeText,
			Integer userId) {
		GroupChat groupChat = groupChatDao.findOne(messageId.longValue());
		logger.debug("GroupChat message received with groupChat data " + groupChat.getData());

		Group group = cacheGroupDao.getGroup(groupChat.getGroupId());
		logger.debug("GroupChat message received with groupChat data " + groupChat.getData());
		boolean isMember = false;
		List<GroupMember> members = group.getMembers();
		if (members != null && !members.isEmpty()) {
			for (GroupMember groupMember : members) {
				if (userId.equals(groupMember.getId())) {
					isMember = true;
					if (groupMember.getMemberStatus().byteValue() != 1) {
						throw new ForbiddenException(ErrorCode.UserLeftGroup, userId, groupChat.getGroupId());
					}
				}
			}
		}
		if (!isMember) {
			throw new ForbiddenException(ErrorCode.UserNotInGroup, userId, groupChat.getGroupId());
		}
		if (group.getGroupStatus() == 2) {
			throw new ForbiddenException(ErrorCode.GroupClosed, userId, groupChat.getGroupId());
		}
		logger.debug("User is recipient of message messageId " + messageId);
		String data = groupChat.getData();
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode dataNode = mapper.readTree(data);
			JsonNode arrNode = dataNode.get("attachments");
			if (arrNode.isArray()) {
				for (final JsonNode jsonNode : arrNode) {
					ObjectNode objectNode = (ObjectNode) jsonNode;
					Integer id = objectNode.get("id").asInt();
					if (attachmentId.equals(id)) {
						objectNode.put("docType", docType);
						objectNode.put("documetPurpose", documentPurpose);
						objectNode.put("docTypeText", docTypeText);
						objectNode.put("documetPurposeText", documentPurposeText);
					}
				}
			}
			String text = dataNode.toString();
			groupChat.setData(text);
			logger.debug("Update docType of message in database " + docType);
			groupChatDao.update(groupChat);
			logger.debug("doc Type updated successfully ");
			groupChat = groupChatDao.findOne(messageId.longValue());

		} catch (Exception e) {
			logger.error("failed to update " + groupChat.getId(), e);
			throw new BadRequestException(ErrorCode.Internal_Server_Error, "Internal_Server_Error");
		}
		logger.debug("Return groupChat after update docType. ");
		return groupChat;
	}

	private void sendGroupChatMessage(UserConnection connection, ChatMessage message) {
		try {
			logger.debug("GroupChat message received with attachment data " + message.getData());
			Integer groupId = message.getTo();
			Group group = ActorThreadContext.get().getGroup();
			if (group == null) {
				group = cacheGroupDao.getGroup(groupId);
			}
			group.setId(groupId);
			message.setGroupName(group.getName());
			decryptName(group, message);
			if (message.getData() != null) {
				JsonNode attachmentNode = message.getData().findPath("attachments");
				if (attachmentNode.isArray()) {
					ArrayNode node = (ArrayNode) attachmentNode;
					String msgText = message.getText();
					if (msgText != null && !msgText.isBlank()) {
						message.setText(msgText);
					}/* else if (msgText != null && (msgText.equalsIgnoreCase("Live Screenshot")
							|| msgText.equalsIgnoreCase("Live Recording"))) {
						message.setText(msgText);
					}*/ else {
						msgText = node.size() + " file(s) attached";
						message.setText(msgText);
					}
				}
			}

			// For Guest chats the guest must see the screenshot and video recordings
			// messages.
			if (message.getChatMessageType() != null
					&& (message.getChatMessageType() == ChatMessageType.ScreenShot.getId().intValue()
					|| message.getChatMessageType() == ChatMessageType.VideoRecording.getId().intValue())) {
				if (group.getGuestUserId() != null) {
					// logger.debug("Skipping message for sending screenshot or videorecording to
					// guest id :"
					// + group.getGuestUserId().toString());
					message.setExcludedRecipients(group.getGuestUserId().toString());
				}
				String text = getText(message.getChatMessageType());
				message.setText(text);
			}

			// send message to all active group members in including self
			List<String> clientIds = CommonUtil.getSupportedClientIds(ChatType.GroupChat);
			Long time = System.currentTimeMillis();
			String date = commonUtil.getDateTimeWithTimeZone(time, connection.getUserContext().getUser().getTimezone());
			message.setDate(date);
			message.setUtcDate(time);

			// Video messages sent from groups with guests must be handled separately.
			if (group.getGuestUserId() != null
					&& message.getChatMessageType() == ChatMessageType.VideoCallMessage.getId().intValue()) {
				handleVideoCallMessage(connection, message, groupId, group, clientIds);
				// Client call messages sent to user.
			} else if (message.getChatMessageType() == ChatMessageType.ClientEvent.getId().intValue()) {
				String webPushClientIds = PropertyUtil.getProperty(Constants.WEB_PUSH_NOTIFICATION_CLIENT_IDS);
				String[] webApps = webPushClientIds.split(",");
				List<String> clientEventsClientIds = Arrays.asList(webApps);

				sendMessageToGroup(connection, message, false, clientEventsClientIds);
			} else {
				if (ChatMessageType.ScreenShot.getId().intValue() == message.getChatMessageType().intValue()) {
					commonUtil.switchUserForScreenshotMessage(connection, message);
				}
				// Save group chat history
				// logger.debug("Create group Chat history for message " + message.getText());
				GroupChat insertedGroupChat = chatHistoryService.createGroupChatHistory(message, group);
				// logger.debug("Created group Chat history returned messageId " +
				// insertedGroupChat.getId());

				message.setMid(insertedGroupChat.getId());
				if (insertedGroupChat.getParentMsg() != null) {
					JsonNode userJson = cacheUserDao.find(insertedGroupChat.getParentMsg().getFrom());
					String parentMsgSenderName = userJson.findPath("firstName").asText();
					if (parentMsgSenderName != null) {
						insertedGroupChat.getParentMsg().setName(parentMsgSenderName);
					}
				}
				message.setParentMsg(insertedGroupChat.getParentMsg());
				message.setUuid(connection.getUuid());
				message.setChatMessageType(Integer.valueOf(insertedGroupChat.getChatMessageType().intValue()));

				sendMessagesToRecipients(connection, message, groupId, group, clientIds);
			}

			// Send user unavailable message if required
			if (!(ChatMessageType.SystemMessage.getId().intValue() == message.getChatMessageType().intValue()
					|| ChatMessageType.VideoCallMessage.getId().intValue() == message.getChatMessageType().intValue()
					|| ChatMessageType.ScreenShot.getId().intValue() == message.getChatMessageType().intValue()
					|| ChatMessageType.ClientEvent.getId().intValue() == message.getChatMessageType().intValue())) {
				if (UserCategory.Guest.getId().intValue() == connection.getUserContext().getUser().getUserCategory()
						.intValue()) {
					// send user unavailable message to gust of GuestGroupChat(Guest of Group)
					if (GroupType.GuestGroupChat.getId().intValue() == group.getGroupType()) {
						if (!connection.getHasSentUnavailableMessage()) {
							Integer firstAvailableUser = getFirstAvailableUserInGroup(group);
							if (firstAvailableUser == null) {
								String status = commonUtil.getUserCustomStatus(group.getCreatedById());
								createMessageWhenUserNotAvailable(connection, message, clientIds, status, group);
							}
						}
					} else if (GroupType.GuestDirectGroupChat.getId().intValue() == group.getGroupType()) {

						if (!connection.getHasSentUnavailableMessage()) {
							Presence presence = presenceService.getPresence(group.getCreatedById(), true);
							String status = commonUtil.getUserCustomStatus(group.getCreatedById());
							if ((status != null && !("Available".equalsIgnoreCase(status)))
									|| (presence.getShow().intValue() != PresenceStatus.AvailableWeb.ordinal())) {
								createMessageWhenUserNotAvailable(connection, message, clientIds, status, group);
							}
						}
					}
				}
			}
		} catch (ApplicationException ex) {
			throw ex;
		}
	}

	private String getText(Integer chatMessageType) {

		String text = null;

		if (chatMessageType == ChatMessageType.ScreenShot.getId().intValue()) {
			text = "Live Screenshot";
		}

		else if (chatMessageType == ChatMessageType.VideoRecording.getId().intValue()) {
			text = "Live Recording";
		}

		return text;
	}

	/***
	 * Return the id of the first registered user member that has a status of
	 * "Available". Guests are not checked.
	 * 
	 * @param group
	 * @return
	 */
	private Integer getFirstAvailableUserInGroup(Group group) {
		Integer returnValue = null;
		List<Integer> memberIds = cacheGroupDao.getGroupMembers(group.getId());
		if (memberIds != null && !memberIds.isEmpty()) {
			String status = null;
			for (Integer memberId : memberIds) {
				status = commonUtil.getUserCustomStatus(memberId);
				if (status != null && "Available".equalsIgnoreCase(status)
						&& !group.getGuestUserId().equals(memberId)) {
					returnValue = memberId;
					break;
				}
			}
		}
		return returnValue;
	}

	private Boolean getDesktopConnectionOfCreator(Group group) {
		Integer creatorId = group.getCreatedById();
		Presence presence = presenceService.getPresence(creatorId, true);
		if (presence.getShow() == PresenceStatus.AvailableWeb.ordinal()) {
			return true;
		}
		return false;
	}

	private void sendMessagesToRecipients(UserConnection connection, ChatMessage message, Integer groupId, Group group,
			List<String> clientIds) {
		Set<Integer> memberIds = cacheGroupDao.getGroupMembersSet(groupId);
		
		
		
		if (memberIds != null && !memberIds.isEmpty()) {
			logger.debug("Received group Memnbers " + memberIds);
			
			JsonNode messageNode = Json.toJson(message);
			if (group.getGuestUserId() != null && memberIds.contains(group.getGuestUserId())) {
				if (ChatMessageType.ScreenShot.getId().intValue() == message.getChatMessageType().intValue()) {
					memberIds.remove(group.getGuestUserId());
				}
			}			
		
			if (!Strings.isNullOrEmpty(message.getExcludedRecipients())) {
				String str = message.getExcludedRecipients();
				List<String> excludeList = Arrays.asList(str.split(","));
				
				for (String excludeId : excludeList) {
					Integer id = Integer.valueOf(excludeId);
					logger.debug("excluded id: "+id);
					if (memberIds.contains(id)) {
						memberIds.remove(id);
					}
				}
			}
			
			
			
			List<Integer> pushList = userConnectionService.sendMessageToActorSet(memberIds, messageNode, clientIds);
			logger.info("group type: "+group.getGroupType().byteValue());
			if (GroupType.GuestDirectGroupChat.getId().byteValue() == (group.getGroupType().byteValue()) ||
					GroupType.GuestRoundRobinGroupChat.getId().byteValue() == (group.getGroupType().byteValue()) ||
					GroupType.GuestGroupChat.getId().byteValue() == (group.getGroupType().byteValue()) ||
					GroupType.VideoKycGuestGroupChat.getId().byteValue() == (group.getGroupType().byteValue())) {
				logger.info("its guest group. send msg to sender that recipient is not available");
				Integer userId = connection.getUserContext().getUser().getId();		
				sendRecipientNotAvailableMessage(userId, pushList, clientIds, message.getCid());
			}
			
			if (!pushList.isEmpty()) {
				notificationService.sendMobileNotification(NotificationType.GroupChat.getId().intValue(), message,
						pushList, PushNotificationVisibility.Recipients);
				logger.debug("called sent mobile push notification for members = " + pushList.toString());
			}
		}
	}
	
	
	private void sendRecipientNotAvailableMessage(Integer userId, List<Integer> pushList, List<String> clientIds, String cid) {		
		logger.info("send msg to "+userId);
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.Error.getId());
		node.put("subtype", ErrorType.RecipientNotAvailable.getId());
		node.put("cid", cid);
		ArrayNode useridsNode = node.putArray("userids");
		for (Integer id: pushList) {
			useridsNode.add(id);
		}
		
		logger.info("msg sent to sender that recipient not available : "+node.asText());
		
		userConnectionService.sendMessageToActor(userId, node, null, clientIds);
	}	

	private void handleVideoCallMessage(UserConnection connection, ChatMessage message, Integer groupId, Group group,
			List<String> clientIds) {
		User guestUser = cacheService.getUser(group.getGuestUserId(), false);

		// ChatType.One2OneVideoChat in the following line is misleading but it return
		// clientid's used for all video chats.
		List<String> videoCallClientIds = CommonUtil.getSupportedClientIds(ChatType.One2OneVideoChat);

		// For messages requesting to start a video call the system must send 2
		// different messages. The original chat message
		// and a separate system message.

		int videoCallType = message.getVideoCallMessageType().intValue();
		if (videoCallType == VideoCallMessageType.TwoWayVideoCallRequest.getId().intValue()
				|| videoCallType == VideoCallMessageType.OneWayVideoCallRequest.getId().intValue()
				|| videoCallType == VideoCallMessageType.AudioCallRequest.getId().intValue()) {

			// This sends the system messages to all required members
			if(!GroupType.VideoKycGuestGroupChat.getId().equals(group.getGroupType().intValue())) {
				sendVideoCallRequestSystemMessage(connection, message, clientIds);				
			} else {
				message.setGroupType(group.getGroupType());
				if (group.getParentGroupId() != null) {
					message.setParentGrpId(group.getParentGroupId());
				}
				if (group.getCreatedById() != null) {
					message.setGrpCreatorId(group.getCreatedById());
				}
			}

			// The following chat messages should only be sent to web clients

			// send regular chat message to self
			userConnectionService.sendMessageToActor(connection.getUserContext().getUser().getId(), connection, message,
					videoCallClientIds);
			logger.debug("sent group message to self :" + message.getText());

			logger.info("user category is: " + guestUser.getUserCategory());
			message.setUuid(connection.getUuid());
			
			JsonNode data = message.getData();
			JsonNode meetingIdNode = data.findPath("meetingId");
			eventTrackingService.sendMeetingEvent(meetingIdNode.asInt(), connection.getUserContext().getUser().getId(), groupId, MeetingEventType.CustomerInvite.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);

			// send regular chat message to guest
			Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(guestUser.getId(), connection,
					message, videoCallClientIds);
			logger.debug("send group Message To Actor returned clients who received messages as "
					+ msgReceiverClients.toString());
		} else {
			if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallEnded.getId().intValue()){
				logger.debug("update meeting status to Ended");
				Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Ended.getId());
				logger.debug("updated meeting status to ended for meetingId:" + meetingId);
				//	sendVideoCallRequestSystemMessage(connection, message, clientIds);
				if(!GroupType.VideoKycGuestGroupChat.getId().equals(group.getGroupType().intValue())) {
					sendVideoCallRequestSystemMessage(connection, message, clientIds);
				}
				eventTrackingService.sendMeetingEvent(meetingId, connection.getUserContext().getUser().getId(), groupId, MeetingEventType.Ended.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);
			}
			else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallRejected.getId().intValue()) {
				logger.debug("update meeting status to Rejected");
				Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Rejected.getId());
				logger.debug("updated meeting status to rejected for meetingId:" + meetingId);
				eventTrackingService.sendMeetingEvent(meetingId, connection.getUserContext().getUser().getId(), groupId, MeetingEventType.Rejected.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);
			}
			else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallAccepted.getId().intValue()) {
				logger.debug("update meeting status to Accepted");
				Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Accepted.getId());
				logger.debug("updated meeting status to accepted for meetingId:" + meetingId);
				eventTrackingService.sendMeetingEvent(meetingId, connection.getUserContext().getUser().getId(), groupId, MeetingEventType.Accepted.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);
			}
			else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallConnected.getId().intValue()) {
				logger.debug("update meeting status to Connected");
				Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Connected.getId());
				logger.debug("updated meeting status to connected for meetingId:" + meetingId);
				eventTrackingService.sendMeetingEvent(meetingId, connection.getUserContext().getUser().getId(), groupId, MeetingEventType.Connected.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);
			}
			else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallDisconnected.getId().intValue()) {
				logger.debug("update meeting status to Disconnected");
				Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Disconnected.getId());
				logger.debug("updated meeting status to disconnected for meetingId:" + meetingId);
				eventTrackingService.sendMeetingEvent(meetingId, connection.getUserContext().getUser().getId(), groupId, MeetingEventType.Disconnected.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);
			}
			else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallIgnored.getId().intValue()) {
				logger.debug("update meeting status to Missed");
				Integer meetingId = updateMeetingInfo(message.getFrom(), message.getData(), VideoCallStatus.Missed.getId());
				logger.debug("updated meeting status to missed for meetingId:" + meetingId);
				eventTrackingService.sendMeetingEvent(meetingId, connection.getUserContext().getUser().getId(), groupId, MeetingEventType.Ignored.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(),null);
			}
			else if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallRating.getId().intValue()) {
				logger.debug("update meeting rating");
				Integer meetingId;
				JsonNode meetingIdNode = message.getData();
				if(meetingIdNode!=null) {
					meetingIdNode = meetingIdNode.findPath("meetingId");

				}
				else {
					meetingId =  0;
				}

				if (meetingIdNode != null && meetingIdNode.isMissingNode() == false) {
					meetingId = meetingIdNode.asInt();
				} else {
					meetingId =  0;
				}
				meetingInfoDao.updateMeetingRating(meetingId, (byte) 0);
				logger.debug("updated meeting rating for meetingId:" + meetingId);
			}

			sendMessageToGroup(connection, message, true, videoCallClientIds);
			logger.debug("sent group message to group id " + groupId.toString());
		}
	}

	@Transactional
	public Integer updateMeetingInfo(Integer from, JsonNode meetingIdNode,Integer videoCallStatus) {

		Integer meetingId ;
		if(meetingIdNode!=null) {
			meetingIdNode = meetingIdNode.findPath("meetingId");
		}
		else {
			meetingId =  0;
		}		
		if (meetingIdNode != null && meetingIdNode.isMissingNode() == false) {
			meetingId = meetingIdNode.asInt();
		} else {
			meetingId =  0;
		}
		meetingInfoDao.updateMeetingStatus(meetingId, videoCallStatus.byteValue(),from);
		return meetingId;
	}

	private void sendGroupTypingMessage(UserConnection connection, ChatMessage message) {
		try {
			logger.debug("send Group typing message");
			message.setFrom(connection.getUserContext().getUser().getId());
			message.setName(connection.getUserContext().getUser().getName());
			message.setStatus(null);
			Group group = cacheGroupDao.getGroup(message.getTo());
			decryptName(group, message);
			sendMessageToGroup(connection, message, false, null);
			logger.debug("sent Group typing message");
		} catch (ApplicationException ex) {
			throw ex;
		}
	}

	private void sendMessageToGroup(UserConnection connection, ChatMessage message, boolean sendToCurrentUser,
			List<String> clientIds) {
		logger.debug("sending Group typing message to members of group " + message.getTo());
		Integer groupId = message.getTo();
		Integer currentUserId = connection.getUserContext().getUser().getId();
		Set<Integer> memberIds = cacheGroupDao.getGroupMembersSet(groupId);
		logger.debug("Received " + (memberIds == null ? null : memberIds.toString()) + "Group members of group "
				+ message.getTo());

		if (clientIds == null) {
			clientIds = CommonUtil.getSupportedClientIds(ChatType.GroupChat);
		}

		if (memberIds.contains(currentUserId.intValue()) && !sendToCurrentUser) {
			memberIds.remove(currentUserId);
		}

		if (!memberIds.isEmpty()) {
			JsonNode messageNode = Json.toJson(message);
			userConnectionService.sendMessageToActorSet(memberIds, messageNode, clientIds);
		}
	}

	private ObjectNode getJsonEventNode(Event event) {
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.Event.getId());
		node.put("subtype", event.getType());
		return node;
	}

	private void addSystemChatMsgToEventNode(ObjectNode node, GroupChat groupChat) {
		node.put("utcDate", groupChat.getCreatedDate());
		if (groupChat.getId() != null) {
			node.put("mid", groupChat.getId());
		}
	}

	private String getGuestChatGroupAddedMessage(String userName, Long messageDate, String timeZone) {		
		String messageText = "New guest user \"" + userName + "\"";
		messageText = CommonUtil.addDateToChatMessage(messageDate, messageText, timeZone);
		return messageText;
	}

	private String getCustomerAssignedMessage(String userName, Long messageDate, String timeZone) {
		String customerAssignedMessage = PropertyUtil.getProperty(Constants.CUSTOMER_ASSIGNED_MESSAGE);
		logger.debug("customer Assigned Message : " + customerAssignedMessage);
		customerAssignedMessage = customerAssignedMessage.replace("{userName}", userName);
		logger.info("customer Assigned Message : " + customerAssignedMessage);
				customerAssignedMessage = CommonUtil.addDateToChatMessage(messageDate, customerAssignedMessage, timeZone);
		return customerAssignedMessage;
	}

	private void createMessageWhenUserNotAvailable(UserConnection connection, ChatMessage message,
			List<String> clientIds, String status, Group group) {
		Group toGroup = group;
		Long time = System.currentTimeMillis();
		String date = commonUtil.getDateTimeWithTimeZone(time, connection.getUserContext().getUser().getTimezone());

		ChatMessage msg = new ChatMessage();
		msg.setType(MessageType.Chat.getId());
		msg.setSubtype(message.getSubtype());
		msg.setName(message.getName());
		msg.setUtcDate(time);
		msg.setDate(date);
		msg.setFrom(message.getFrom());
		msg.setTo(message.getTo());
		decryptName(toGroup, msg);

		Map<String, String> args = new HashMap<String, String>();
		JsonNode toUserJson = null;

		msg.setChatMessageType(Integer.valueOf(ChatMessageType.SystemMessage.getId()));
		String textMsg = PropertyUtil.getProperty(Constants.SYS_MESSAGE_FOR_UNAVAILABLE_USER);
		msg.setText(textMsg);
		GroupChat dbMsg = chatHistoryService.createGroupChatHistory(msg, toGroup);
		if (dbMsg.getId().intValue() <= message.getMid()) {
			logger.debug("Group System Message already sent with messageId " + dbMsg.getId());
		} else {
			msg.setMid(dbMsg.getId());
			msg.setUuid(connection.getUuid());
			sendMessageToGroup(connection, msg, true, clientIds);
			logger.debug("Sent Group System Message messageId " + msg.getMid());
		}
		connection.setHasSentUnavailableMessage(true);
	}

	private void sendVideoCallRequestSystemMessage(UserConnection connection, ChatMessage message,
			List<String> clientIds) {
		Group group = null;
		Long time = System.currentTimeMillis();
		String date = commonUtil.getDateTimeWithTimeZone(time, connection.getUserContext().getUser().getTimezone());
		JsonNode data = message.getData();

		if (message.getSubtype().byteValue() == ChatType.GroupChat.getId().byteValue()) {
			group = ActorThreadContext.get().getGroup();
			if (group == null) {
				group = cacheGroupDao.getGroup(message.getTo());
			}
			group.setId(message.getTo());
			GroupChat groupChat = null;
			if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallEnded.getId()
					.intValue()) {
				Integer caller = null;
				// Set the affectedMemberId based on who initiated the VideoCallEnded message.
				// If the RU
				// ended the call the affectedMemberId is the guest - else its the other way
				// around.
				if (data != null) {
					JsonNode callDurationNode = data.findPath("callDuration");
					if (callDurationNode != null) {
						group.setCallDuration(callDurationNode.asInt());
					}
					JsonNode meetingIdNode = data.findPath("meetingId");
					if (meetingIdNode != null && meetingIdNode.isMissingNode() == false) {
						group.setMeetingId(meetingIdNode.asInt());
					} else {
						group.setMeetingId(0);
					}
					caller = data.findPath("caller").asInt();
				}
				group.setActionTakerId(message.getFrom());
				if (message.getFrom().equals(caller)) {
					group.setAffectedMemberId(group.getGuestUserId());
				} else {
					group.setAffectedMemberId(caller);
				}
				groupChat = chatHistoryService.createGroupChatHistory(EventType.VideoCallEnded, group);
			} 
			else { // Add more types here as the event types increase
				if (data != null) {
					JsonNode nodeIsAutoRecording = data.findPath("isAutoRecording");
					if (nodeIsAutoRecording != null) {
						group.setAutoRecordingEnabled(nodeIsAutoRecording.asBoolean());
					}
					JsonNode meetingIdNode = data.findPath("meetingId");
					if (meetingIdNode != null && meetingIdNode.isMissingNode() == false) {
						group.setMeetingId(meetingIdNode.asInt());						
					} else {
						group.setMeetingId(0);
					}
					group.setVideoCallMessageType(message.getVideoCallMessageType());
				}
				group.setAffectedMemberId(group.getGuestUserId());
				group.setActionTakerId(message.getFrom());
				groupChat = chatHistoryService.createGroupChatHistory(EventType.VideoCallRequest, group);
			}

			logger.debug("Sending video call request (" + message.getVideoCallMessageType()
			+ ") system message for group id " + message.toString());
			List<Integer> groupMembers = cacheGroupDao.getGroupMembers(group);
			if (groupMembers != null && !groupMembers.isEmpty()) {
				ChatMessage msg = new ChatMessage();
				msg.setType(MessageType.Chat.getId());
				msg.setSubtype(Integer.valueOf(ChatType.GroupChat.getId().intValue()));
				msg.setName(message.getName());
				msg.setUtcDate(message.getUtcDate());
				msg.setDate(message.getDate());
				msg.setFrom(message.getFrom());
				msg.setUtcDate(time);
				msg.setDate(date);
				msg.setTo(message.getTo());
				msg.setChatMessageType(Integer.valueOf(ChatMessageType.SystemMessage.getId()));

				if (message.getVideoCallMessageType() != null) {
					msg.setVideoCallMessageType(message.getVideoCallMessageType().intValue());
				}
				if (group.getParentGroupId() != null) {
					msg.setParentGrpId(group.getParentGroupId());
				}
				if (group.getCreatedById() != null) {
					msg.setGrpCreatorId(group.getCreatedById());
				}
				msg.setMid(groupChat.getId());
				msg.setUuid(connection.getUuid());

				for (Integer receipinetMemberId : groupMembers) {// send message to all group members
					msg.setText(
							eventNotificationBuilder.buildMessage(null, groupChat.getData(), receipinetMemberId));

					msg.setMid(groupChat.getId());
					msg.setUuid(connection.getUuid());
					Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(receipinetMemberId,
							connection, msg, clientIds);
					logger.debug("Sent video call request for group system message with messageId " + msg.getMid());
					Boolean isSendPN = commonUtil.isAlwaysSendPushNofication(msg, msgReceiverClients);
					logger.debug("isSendPN flag = " + isSendPN);
					// send push notification
					if (isSendPN && connection.getUserContext().getUser().getId().intValue() != receipinetMemberId
							.intValue()) {
						List<Integer> pushList = new ArrayList<>();
						pushList.add(receipinetMemberId);
						notificationService.sendMobileNotification(NotificationType.GroupChat.getId().intValue(),
								msg, pushList, PushNotificationVisibility.Recipients);
						logger.debug("called sent mobile push notification for members = " + pushList.toString());
					}
				}
			}
		}
	}

	@Override
	public void sendUpdateUseCaseStatusEvent(EventType event, Group group, GroupChat groupChat) {
		logger.info("send update use case status Message to groupMembers for group : " + group.getName());
		List<Integer> groupMembers = cacheGroupDao.getGroupMembers(group);
		Group inputGroup = new Group(group);
		if (groupMembers != null && !groupMembers.isEmpty()) {
			logger.info("Received groupMembers for group : " + group.getName() + " as " + groupMembers.toString());
			List<String> clientIds = CommonUtil.getSupportedClientIds(ChatType.GroupChat);
			logger.info("Received clientIds for send update use case status event of size:" + clientIds.size());
			User user = null;
			for (Integer receipinetMemberId : groupMembers) {// send message to all group members
				List<Integer> pushList = new ArrayList<>();

				ChatMessage msg = new ChatMessage();
				msg.setType(MessageType.Chat.getId());
				msg.setSubtype(Integer.valueOf(ChatType.GroupChat.getId().intValue()));
				Long time = System.currentTimeMillis();
				msg.setUtcDate(time);
				String date = commonUtil.getDateTimeWithTimeZone(time, null);
				msg.setDate(date);
				msg.setFrom(group.getCreatedById());
				msg.setTo(group.getId());
				msg.setText(groupChat.getText());
				msg.setChatMessageType(Integer.valueOf(ChatMessageType.SystemMessage.getId()));
				msg.setMid(groupChat.getId());

				Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(receipinetMemberId, null, msg,
						clientIds);
				logger.info("sent to " + msgReceiverClients.size() + "clients");

			}
		} else {
			logger.warn("Received no groupMembers for group : " + group.getName()
			+ " so not sending use case upadte EventMessage to groupMembers ");
		}
	}

	@Override
	public void sendChatGuestBrowserInfoEvent(EventType event, Group group, GroupChat groupChat) {
		logger.info("send sendChatGuestBrowserInfoEvent Message to groupMembers for group : " + group.getName());
		List<Integer> groupMembers = cacheGroupDao.getGroupMembers(group);
		if (groupMembers != null && !groupMembers.isEmpty()) {
			logger.info("Received groupMembers for group : " + group.getName() + " as " + groupMembers.toString());

			PushNotificationVisibility visibility = PushNotificationVisibility.All;
			
			List<String> clientIds = CommonUtil.getSupportedClientIds(ChatType.GroupChat);
			logger.info("Received clientIds for sendChatGuestBrowserInfoEvent of size:" + clientIds.size());
			User user = null;
			for (Integer receipinetMemberId : groupMembers) {// send message to all group members
				List<Integer> pushList = new ArrayList<>();

				ChatMessage msg = new ChatMessage();
				msg.setType(MessageType.Chat.getId());
				msg.setSubtype(Integer.valueOf(ChatType.GroupChat.getId().intValue()));
				Long time = System.currentTimeMillis();
				msg.setUtcDate(time);
				String date = commonUtil.getDateTimeWithTimeZone(time, null);
				msg.setDate(date);
				msg.setFrom(group.getCreatedById());
				msg.setTo(group.getId());
				String text = eventNotificationBuilder.buildMessage(event, groupChat.getData(), receipinetMemberId);// event-EventType.GuestChatDeviceInfoUpdate,EventType.GuestChatDeviceInfo
				msg.setText(text);
				msg.setChatMessageType(Integer.valueOf(ChatMessageType.SystemMessage.getId()));
				msg.setMid(groupChat.getId());
				msg.setChatType(Integer.valueOf(ChatType.GroupChat.getId()));
				JsonNode fromUserJson = cacheUserDao.find(group.getCreatedById());
				String name = fromUserJson.findPath("firstName").asText();
				msg.setName(name);
				Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(receipinetMemberId, null, msg, clientIds);
				logger.info("sent to " + msgReceiverClients.size() + "clients");
				// (receipinetMemberId, node,null, clientIds);
		     	Boolean isSendPN = commonUtil.isAlwaysSendPushNofication(msg, msgReceiverClients);
				logger.info("isSendPN flag = " + isSendPN);
				if (isSendPN && (group.getGuestUserId() != null
						&& !group.getGuestUserId().equals(receipinetMemberId.intValue()))) {
					pushList.add(receipinetMemberId);
					Long utcDate = groupChat.getCreatedDate();
					Long mid = groupChat.getId();
				/*	notificationService.sendMobileNotification(
							NotificationType.GuestChatDeviceInfoUpdate.getId().intValue(), group.getId(),
							group.getCreatedById(), null, text, null, utcDate, mid, pushList, null,  groupChat.getChatMessageType(), visibility);*/
					notificationService.sendMobileNotification(NotificationType.GuestChatDeviceInfoUpdate.getId().intValue(), msg, pushList, visibility);
					logger.info("called sent mobile push notification for member = " + receipinetMemberId);
				} 
			}
		} else {
			logger.warn("Received no groupMembers for group : " + group.getName()
			+ " so not sending GuestChatDeviceInfoUpdate EventMessage to groupMembers ");
		}
	}

	private void decryptName(Group group, ChatMessage message){
		if(GroupType.VideoKycGuestGroupChat.getId() == group.getGroupType().intValue()) {
			//Boolean isEncrypted = kmsUtil.checkEncryptionRequired(group.getCreatorOrganizationId());
			if(group.getDataEncryptionKeyId() != null) {
				logger.info("Decrypt value is required for given organization: ",group.getCreatorOrganizationId());
				if(message!= null ) {
					if(message.getName() != null  && !message.getName().isEmpty()) {
						String updatedName = dataEncryptionService.decrypt(group.getDataEncryptionKeyId(), message.getName());
						logger.info("Decrypt value is done");
						message.setName(updatedName);
					}
					if(message.getGroupName()!= null && !message.getGroupName().isEmpty() ) {
						String updatedGroupName = dataEncryptionService.decrypt(group.getDataEncryptionKeyId(), message.getGroupName());
						logger.info("Decrypt value is done");
						message.setGroupName(updatedGroupName);
					}
				}
//				String updatedGroupName = dataEncryptionService.decrypt(group.getDataEncryptionKeyId(), group.getName());
//				logger.info("Decrypt value is done");
//				group.setName(updatedGroupName);
			}
		}
	}
}
