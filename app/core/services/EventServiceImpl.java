package core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.workapps.common.core.services.DataEncryptionService;
import core.daos.*;
import core.entities.*;
import core.exceptions.BadRequestException;
import core.utils.*;
import core.utils.Enums.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import play.libs.Json;

import java.io.IOException;
import java.util.*;

@Service
public class EventServiceImpl implements EventService {
	final static Logger logger = LoggerFactory.getLogger(EventServiceImpl.class);

	@Autowired
	private PresenceService presenceService;

	@Autowired
	private UserConnectionService userConnectionService;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private CacheGroupDao cacheGroupDao;

	@Autowired
	private ChatHistoryService chatHistoryService;

	@Autowired
	private EventNotificationBuilder eventNotificationBuilder;

	@Autowired
	private CacheUserDao cacheUserDao;

	@Autowired
	private CacheVideoKycDao cacheVideoKycDao;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private CacheOpenMapInfoDao cacheOpenMapInfoDao;

	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	private RecordingService recordingService;

	@Autowired
	private GroupChatDao groupChatDao;

	@Autowired
	private GroupChatMessageService groupChatMessageService;

	@Autowired
	private VideokycService videokycService;

	@Autowired
	private VideokycAgentQueueDao videokycAgentQueueDao;

	@Autowired
	private MeetingUtil meetingUtil;
	
	@Autowired
	private JsonUtil jsonUtil;


	@Autowired
	private DataEncryptionService dataEncryptionService;

	@Autowired
	private OrgUtil orgUtil;

	@Override
	public void handleEvent(Event event) {
		EventType eventType = Enums.get(EventType.class, event.getType());
		switch(eventType){
		case SocketConnect:
			socketConnect(event);
			break;
		case SocketDisConnect:
			socketDisconnect(event);
			break;
		case ContactAdd:
			contactAdd(event);
			break;
		case LockUser:
		case ResetPassword:
			lockUserOrResetPassword(event);
			break;
		case ContactAddOrg:
			contactAddOrg(event);
			break;
		case CloseConnection:
			closeConnection(event);
			break;
		case UserUpdate:
			userUpdate(event);
			break;
		case AddGroup:
		case ChatGuestAdded:
			addGroupOrChatGuestAdded(event);
			break;
		case CloseGroup:
			closeGroup(event);
			break;
		case LeaveGroup:
			leaveGroup(event);
			break;
		case ExitGroup:
			exitGroup(event);
			break;
		case UpdateGroup:
			updateGroup(event);
			break;
		case UpdateUseCaseStatus:
			updateUseCaseStatus(event);
			break;
		case DeleteUser:
			deleteUser(event);
			break;
		case Logout:
			logout(event);
			break;
		case SessionExpired:
			sessionExpired(event);
			break;
		case CreateMemo:
			createMemo(event);
			break;
		case DeviceUpdated:
			deviceUpdated(event);
			break;
		case GuestChatDeviceInfoUpdate:
			guestChatDeviceInfoUpdate(event);
			break;
		case CreateVideoKyc:
			createVideoKyc(event);
			break;
		case UpdateVideoKycStatus:
			updateVideoKycStatus(event);
			break;		
		case ChatCustomerForwardGuestAdded:
			chatCustomerForwardGuestAdded(event);
			break;
		case UpdateGroupBySystem:
			updateGroupBySystem(event);
			break;
		case UploadAttachment:
			uploadAttachment(event);
			break;
		case AgentAssignedToVideoKyc:
			agentAssignedToVideoKyc(event);
			break;
		default:
			logger.error("Unsupported event type received {} in event Data {}", eventType, event.toString());
		}
	}

	private void closeConnection(Event event) {
		Integer Id = Integer.valueOf(event.getData().get("Id"));
		userConnectionService.closeAllUserConnection(Id);
	}

	private void socketConnect(Event event) {
		Map<String, String> data = event.getData();
		if (data != null) {
			Integer userId = Integer.valueOf(data.get("userId"));
			logger.info("socket connect id : " + userId);
			User user = cacheService.getUser(userId, false);
			videokycService.onUserConnection(user);
		}
	}

	private void socketDisconnect(Event event) {
		Map<String, String> data = event.getData();
		if (data != null) {
			Integer userId = Integer.valueOf(data.get("userId"));
			logger.info("socket disconnect id : " + userId);
			User user = cacheService.getUser(userId, false);
			videokycService.onUserDisconnect(user);
		}
	}

	private void lockUserOrResetPassword(Event event) {
		Integer Id = Integer.valueOf(event.getData().get("Id").toString());
		ObjectNode node = getJsonEventNode(event);
		node.put("id", Id);
		userConnectionService.sendMessageToActor(Id, node, null);
		sendEventToAllContacts(Id, node);
		userConnectionService.closeAllUserConnection(Id);
	}

	private void addGroupOrChatGuestAdded(Event event) {
		Group group = getGroup(event);
		logger.info("received AddGroup event for group : " + group.getName());
		GroupChat groupChatCreateGroup = chatHistoryService
				.createGroupChatHistory(EventType.getEventTypeById(event.getType()), group);
		logger.info("Created Group chat history for group : " + group.getName());
		group = getGroup(event);
		GroupChat deviceInfoSystemMessage = creatSystemMessageForNewGuestGroup(group, event);

		List<Integer> members = cacheGroupDao.getGroupMembers(group);
		videokycService.addAgentInQueue(members, group.getId().longValue());
		logger.debug("received members for group : " + group.getName() + " as : " + members.toString());
		for (Integer memberId : members) {
			if (!group.getCreatedById().equals(memberId)) {
				group.setAffectedMemberId(memberId);
				logger.info("Added affected member : " + memberId);
				chatHistoryService.createGroupChatHistory(EventType.AddGroup_AddMember, group);
			}
		}
		Integer ExcludeConsumerId = null;
		if (group.getGroupType() == GroupType.ConsumerDirectGroupChat.getId().byteValue()
				|| group.getGroupType() == GroupType.ConsumerGroupChat.getId().byteValue()) {
			if (group.getGuestUserId() != null) {
				ExcludeConsumerId = group.getGuestUserId();
			}
		}
		chatHistoryService.createGroupChatMsgRecipient(group.getId(), ExcludeConsumerId);


		if (event.getData().get("videoKYC") != null) {
			groupChatCreateGroup = createVideoKycHistoryV0(event, groupChatCreateGroup);
		}

		logger.info("sending AddGroup event for group : " + group.getName());
		groupChatMessageService.sendAddGroupEventMessage(event, group, groupChatCreateGroup);
		logger.info("sent AddGroup event for group : " + group.getName());

		Boolean openChatEnabled = event.getData().get("OpenChatEnabled") == null ? null
				: Boolean.parseBoolean(event.getData().get("OpenChatEnabled"));
		if (openChatEnabled != null && openChatEnabled) {
			handleOpenChatEnabledChange(event, group);
		}
	}

	private void uploadAttachment(Event event) {
		try {
			Group group = getGroup(event);
			logger.info("received AddGroup event for group : " + group.getName());

			ObjectMapper mapper = new ObjectMapper();
			String inputAattachment = event.getData().get("Attachment");
			ChatMessage message = getChatMessage(group);
			String aadharUploadMessageRequired = event.getData().get("AadharUploadMessageRequired");
			String senderId = event.getData().get("SenderId");
			logger.info("------------senderId--------- "+senderId);
			if(senderId != null && !senderId.isEmpty()) {
				message.setFrom(Integer.valueOf(senderId));
			}            
			message.setChatMessageType(ChatMessageType.CustomerDetails.getId().intValue());

			if (inputAattachment != null) {
				logger.info("------------Create  Attachment--------- ");
				Attachment attachment = mapper.readValue(inputAattachment, Attachment.class);
				if(group.getGuestUserId() != null) {
					message.setExcludedRecipients(group.getGuestUserId().toString());
				}               
				GroupChat videoKycChat  =createAttachmentChatHistory(group, message, attachment);
				String date =CommonUtil.getDateTimeWithTimeZone(videoKycChat.getCreatedDate(), null);
				message.setDate(date);
			}
			;
			List<Integer> members = cacheGroupDao.getGroupMembers(group);
			for (Integer memberId : members) {
				if (!memberId.equals(group.getGuestUserId())) {
					userConnectionService.sendMessageToActor(memberId, Json.toJson(message), null);
				}
			}
			//   String aadharUploadMessageRequired = event.getData().get("AadharUploadMessageRequired");
			if(Boolean.valueOf(aadharUploadMessageRequired)) {
				User guestUser = cacheService.getUser(group.getGuestUserId(), false);

				ChatMessage textMessage = getChatMessage(group);
				textMessage.setUtcDate(System.currentTimeMillis());
				textMessage.setChatMessageType(ChatMessageType.ChatMessage.getId().intValue());
				textMessage.setName(guestUser.getName());
				textMessage.setGroupName(group.getName());
				textMessage.setText(PropertyUtil.getProperty(Constants.UPLOAD_ADHAAR_XML_MESSAGE, "Aadhaar XML uploaded successfully"));
				GroupChat chatMessage = chatHistoryService.createGroupChatHistory(textMessage, group);
				textMessage.setCid(chatMessage.getId().toString());
				String date =CommonUtil.getDateTimeWithTimeZone(chatMessage.getCreatedDate(), null);
				textMessage.setDate(date);
				for (Integer memberId : members) {
					userConnectionService.sendMessageToActor(memberId, Json.toJson(textMessage), null);
				}
			}
		} catch (IOException e) {
			logger.error("exception occured: " + e);
			throw new BadRequestException(ErrorCode.Internal_Server_Error, "Create chat history Failed", e);
		}
	}


	private void updateGroupBySystem(Event event) {
		String data = event.getData().get("Groups").toString();
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode groupJsonArray = mapper.readTree(data);
			if (groupJsonArray.isArray()) {
				for (final JsonNode objNode : groupJsonArray) {
					Integer groupId = objNode.get("GroupId").asInt();
					logger.info(" groupId: " + groupId);
					Group group = cacheService.getGroupDetails(groupId);
					group.setId(groupId);
					//	List<Integer> excludeMemberIds = cacheGroupDao.getGroupMembers(group);
					//List<String> excludedRecipients = excludeMemberIds.stream().map(memberId -> memberId.toString()).collect(Collectors.toList());
					//	String excludeMembers = String.join(",", excludedRecipients);
					//	group.setExcludedRecipients(excludeMembers);
					if (objNode.has("AddedMemberIds")) {
						String addedMemberIds = objNode.get("AddedMemberIds").asText();
						logger.info(" addedMemberIds: " + addedMemberIds);
						handleSystemGroupMemberAddition(group, addedMemberIds);
					}
					if (objNode.has("RemovedMemberIds")) {
						String removedMemberIds = objNode.get("RemovedMemberIds").asText();
						logger.info(" removedMemberIds: " + removedMemberIds);
						handleSystemGroupMemberRemoval(group, removedMemberIds);
					}
				}
			}
		} catch (IOException e) {
			logger.error("excetion occured: " + e);
		}
	}

	private void chatCustomerForwardGuestAdded(Event event) {
		Group group = getGroup(event);
		logger.info("received chat customer-forward guest event for group : " + group.getName());
		GroupChat groupChatCreateGroup = chatHistoryService
				.createGroupChatHistory(EventType.getEventTypeById(event.getType()), group);
		logger.info("Created Group chat history for group : " + group.getName());

		List<Integer> members = cacheGroupDao.getGroupMembers(group);
		logger.info("received members for group : " + group.getName() + " as : " + members.toString());
		for (Integer memberId : members) {
				if (!group.getCreatedById().equals(memberId)) {
			group.setAffectedMemberId(memberId);
			logger.info("Added affected member : " + memberId);
			chatHistoryService.createGroupChatHistory(EventType.AddGroup_AddMember, group);
			}
		}
		logger.info("sending AddGroup event for group : " + group.getName());
		groupChatMessageService.sendAddGroupEventMessage(event, group, groupChatCreateGroup);
		logger.info("sent AddGroup event for group : " + group.getName());
		String welcomeMessage = event.getData().get("WelcomeMessage");
		logger.info("welcomeMessage : {}", welcomeMessage);		
		if (StringUtils.isNotBlank(welcomeMessage)) {
			ChatMessage message = getMessage(group);
			message.setText(welcomeMessage);
			message.setChatMessageType(ChatMessageType.CustomerWelcomMessage.getId().intValue());
			logger.info("Created text for videoKycs text message: ");
			chatHistoryService.createGroupChatHistory(message, group);
		}
		String messageText = event.getData().get("messageText");
		if (StringUtils.isNotBlank(messageText)) {
			createCustomerDetailsChatHistory(group, messageText, event);
			logger.info("created customer details history.");
		}
	}


	private void updateVideoKycStatus(Event event) {
		logger.info("video kyc status change event");
		Map<String, String> props = event.getData();

		Integer guestGroupId = Integer.valueOf(props.get("guestGroupId"));
		Integer agentId = Integer.valueOf(props.get("agentId"));
		Byte status = Byte.valueOf(props.get("status"));
		Boolean signatureTaken = Boolean.valueOf(props.get("signatureTaken"));
		logger.info("guestGroupId: " + guestGroupId + ", agentId: " + agentId + ", status: " + status);
		if (VideoKYCStatus.Successful.getId().equals(status) || VideoKYCStatus.Rejected.getId().equals(status) ||
				VideoKYCStatus.Unable.getId().equals(status)) {
			logger.info("send logout event");
			Integer guestUserId = Integer.valueOf(props.get("guestUserId"));
			sendSessionExpiredMessage(guestUserId, SessionExpiryReason.KycClosed.getName(), status.intValue(), signatureTaken);

			logger.info("handle member add/remove event");
			handleMemberExit(agentId, guestGroupId);

			logger.info("change agent status in queue");
			videokycService.updateAgentStatusInAgentQueue(agentId);

			Integer kycId = Integer.valueOf(props.get("kycId"));
			Group group = cacheGroupDao.getGroup(guestGroupId);
			Integer orgId = group.getCreatorOrganizationId();

			boolean canMakeAuditorReady = VideoKYCStatus.Successful.getId().equals(status)
					|| (
							VideoKYCStatus.Rejected.getId().equals(status) &&
							orgUtil.getPreferenceAsBoolean(orgId, OrgUtil.AssignRejectedKycToAuditor)
							);
			logger.info("Can make auditor ready for org {}, group {}, kyc {} is {}", orgId, guestGroupId, kycId, canMakeAuditorReady);
			if (canMakeAuditorReady && isRecordingAvailable(guestGroupId)) {
				logger.info("AuditorReady change status for KYC {}", kycId);
				videokycAgentQueueDao.changeVideoKycStatus(kycId, VideoKYCStatus.AuditorReady.getName(), null, orgId);
			}
			logger.info("updating bandwidth info for all meetings of kyc with groupId:" + guestGroupId);
			meetingUtil.getAndUpdateMeetingBandWidthInfo(guestGroupId);
			logger.info("bandwidth info update call is completed");

		} else if (VideoKYCStatus.AgentAssigned.getId().equals(status)) {
			logger.info("handle agent assigned");
			handleMemberAddRemove(event, agentId, guestGroupId);
		}
	}

	private void createVideoKyc(Event event) {
		Group group = getGroup(event);
		logger.info("received AddGroup event for group : " + group.getName());

		logger.info("Created Group chat history for group : " + group.getName());
		GroupChat groupChatCreateGroup = chatHistoryService.createGroupChatHistory(EventType.getEventTypeById(event.getType()), group);
		List<Integer> members = cacheGroupDao.getGroupMembers(group);
		String videoKycStr = event.getData().get("VideoKYC");
		logger.debug("received Video KYC string : " + videoKycStr);
		ObjectMapper mapper = new ObjectMapper();
		Integer agentId= null;
		try {
			JsonNode jsonNode = mapper.readTree(videoKycStr);
			for (Integer memberId : members) {
				if (!memberId.equals(group.getGuestUserId())) {
					group.setAffectedMemberId(memberId);
					logger.info("Added affected member : " + memberId);
					GroupChat groupChat = chatHistoryService.createGroupChatHistory(EventType.AddGroup_AddMember, group);
					event.setType(EventType.AddGroup.getId());
					ObjectNode node = getJsonEventNode(event);
					node.put("grpId", group.getId());
					node.put("grpCrtId", group.getCreatedById());
					node.put("groupName", group.getName());
					node.put("groupType", group.getGroupType());
					User user = cacheService.getUser(memberId, false);

					JsonNode flow = jsonNode.get("flow");
					if (flow != null) {
						node.put("videoKycFlow", flow.asText());
					}
					List<String> clientIds = CommonUtil.getSupportedClientIds(ChatType.GroupChat);
					node.put("guestId", group.getGuestUserId());
					node.put("affMemId", memberId);
					if (event.getData().get("meetingId") != null && event.getData().get("recordingId") != null){
						node.put("meetingId", event.getData().get("meetingId"));
						node.put("recordingId", event.getData().get("recordingId"));
					}

					Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(memberId, node,
							null, clientIds);

					if(msgReceiverClients != null) {
						String message = getCustomerAssignedMessage(group.getName(), groupChat.getCreatedDate(),null);

						//ObjectNode pnObject = createPushNotificationJSON(node);						
						//	notificationService.sendMobilePushNotification(new ArrayList<Integer>(Arrays.asList(memberId)), pnObject);

						//final ObjectNode messageNode = jsonUtil.read(message);
						node.put("to", group.getId());
						node.put("from", group.getCreatedById());
						node.put("chatType", 1); //GroupChat
						node.put("chatMessageType", 2); //SystemMessage
						node.put("text",message);
						node.put("utcDate", System.currentTimeMillis());		
						node.put("name", user.getName());
						ChatMessage chatMessage = jsonUtil.read(jsonUtil.write(node), ChatMessage.class);
						logger.info("pnObject: {} ", chatMessage);	
						notificationService.sendMobileNotification(NotificationType.AddGroup.getId().intValue(), chatMessage, Collections.singletonList(memberId), PushNotificationVisibility.All);
						agentId=memberId;

					}
				}
			}
			createVideoKycHistory(event, groupChatCreateGroup);

			Integer orgId = jsonNode.get("organizationId").asInt();		
			JsonNode productPref = orgUtil.getPreferenceValue(orgId, "ProductPreference");
			logger.info("productPref {}",productPref);
			if(productPref != null) {			

				addYouTubeLinkInChatHistory(agentId, group, productPref, jsonNode.get("productName").asText());
			}


		} catch (IOException e) {
			logger.error("exception occured " + e);
		}
	}

	private String getCustomerAssignedMessage(String userName, Long messageDate, String timeZone) {
		String customerAssignedMessage = PropertyUtil.getProperty(Constants.CUSTOMER_ASSIGNED_MESSAGE);
		logger.debug("customer Assigned Message : " + customerAssignedMessage);
		customerAssignedMessage = customerAssignedMessage.replace("{userName}", userName);
		logger.info("customer Assigned Message : " + customerAssignedMessage);
		customerAssignedMessage = CommonUtil.addDateToChatMessage(messageDate, customerAssignedMessage, timeZone);
		return customerAssignedMessage;
	}


	private void createMemo(Event event) {
		Memo memo = getMemo(event);
		if (memo != null) {
			User creator = cacheService.getUser(memo.getCreatedById(), false);
			memo.setCreator(creator);

			ObjectNode node = getJsonEventNode(event);
			node.put("memoId", memo.getId());
			ObjectNode data = Json.newObject();
			data.put("memoId", memo.getId());
			data.put("alert", "New Memo: " + memo.getSubject());
			data.put("senderId", memo.getCreatedById());
			data.put("name", memo.getCreator().getName());
			node.put("data", data);

			List<Long> recipents = memo.getRecipientIds();
			ObjectNode pushNotification = createPushNotificationJSONMemo(memo);
			Set<Integer> members = new HashSet<>();
			//List<Integer> pushRecipient = new ArrayList<>();
			for (Long receipinetMemberId : recipents) {// send message to all group members
				List<Integer> recipients = new ArrayList<>();
				recipients.add(receipinetMemberId.intValue());
				members.add(receipinetMemberId.intValue());
			}
			List<Integer> msgReceivedUserIds = userConnectionService.sendMessageToActorSet(members, node, null);
			notificationService.sendMobilePushNotification(msgReceivedUserIds, pushNotification, NotificationType.Memo);
		}
	}

	private void sessionExpired(Event event) {
		Integer loggedOutUserId = Integer.valueOf(event.getData().get("UserId"));
		logger.info("send session expired message to userId: " + loggedOutUserId);
		Integer groupId = Integer.valueOf(event.getData().get("GroupId"));
		Group group = cacheGroupDao.getGroup(groupId);
		if (GroupType.VideoKycGuestGroupChat.getId().byteValue() == group.getGroupType().byteValue()) {
			logger.info("remove customer from queue: " + loggedOutUserId);
			videokycService.deleteCustomerById(loggedOutUserId);
		}
		String reason = event.getData().getOrDefault("reason", SessionExpiryReason.Unknown.getName());
		sendSessionExpiredMessage(loggedOutUserId, reason, null, null);
	}

	private void logout(Event event) {
		Integer loggedOutUserId = Integer.valueOf(event.getData().get("UserId"));
		String loggedOutToken = event.getData().get("AuthToken");
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.Event.getId());
		node.put("subtype", EventType.Logout.getId());
		node.put("AuthToken", loggedOutToken);	
		String[] role = cacheService.getUserRole(loggedOutUserId);
		if (Arrays.asList(role).contains(RoleName.VideoKYCAgent.getName())) {
			videokycService.updateAgentStatusInAgentQueue(loggedOutUserId);
		}
		userConnectionService.sendMessageToActor(loggedOutUserId, node, null);
	}

	private void deleteUser(Event event) {
		Integer deletedUserId = Integer.valueOf(event.getData().get("Id"));
		handleGroupMemberDelete(event, deletedUserId);
		logger.debug("Sent Group Events for delete User : " + deletedUserId);
		handleDeleteUser(event, deletedUserId);
		logger.debug("Sent UserDelete Event for delete User : " + deletedUserId);
	}

	private void updateUseCaseStatus(Event event) {
		Group group = getGroup(event);
		logger.debug("Create group Chat History for update use case status : " + group.getId());
		GroupChat updateUseCaseSystemMessage = createSystemMessageForUpdateUseCaseStatus(group, event);
		logger.debug("send event of update use case status : " + group.getId());
		groupChatMessageService.sendUpdateUseCaseStatusEvent(EventType.UpdateUseCaseStatus, group, updateUseCaseSystemMessage);
		logger.info("sent ChatGuestBrowserInfoEvent");
	}

	private void updateGroup(Event event) {
		Group group = getGroup(event);
		String oldName = event.getData().get("OldName");
		String newName = event.getData().get("Name");
		String addedMemberIds = event.getData().get("AddedMemberIds");
		String reovedMemberIds = event.getData().get("RemovedMemberIds");
		String changedRoleMemberIds = event.getData().get("ChangedRoleMemberIds");
		Boolean openChatEnabled = event.getData().get("OpenChatEnabled") == null ? null
				: Boolean.parseBoolean(event.getData().get("OpenChatEnabled"));
		if (reovedMemberIds != null && !reovedMemberIds.isEmpty()) {// Members are removed
			handleGroupMemberRemoval(event, group, reovedMemberIds);
		}
		if (changedRoleMemberIds != null && !changedRoleMemberIds.isEmpty()) {// Members role changed
			handleGroupMemberRoleChange(event, group, changedRoleMemberIds);
		}
		if (newName != null && !newName.isEmpty() && oldName != null && !oldName.isEmpty()
				&& !oldName.equals(newName)) {// Name is changed
			handleGroupNameChange(event, group, oldName, newName);
		}
		if (addedMemberIds != null && !addedMemberIds.isEmpty()) {// Members are added
			handleGroupMemberAddition(event, group, addedMemberIds);
		}
		if (openChatEnabled != null && openChatEnabled) {
			handleOpenChatEnabledChange(event, group);
		}
	}

	private void exitGroup(Event event) {
		Integer exitMemberId = Integer.valueOf(event.getData().get("MemberId"));
		Byte memberStatus = event.getData().get("MemberStatus") == null ? null
				: Byte.valueOf(event.getData().get("MemberStatus"));
		Group group = getGroup(event);
		group.setAffectedMemberId(exitMemberId);

		GroupChat groupChat = null;
		if (memberStatus == null || memberStatus == 1) {// Active Member exiting without leave
			groupChat = chatHistoryService.createGroupChatHistory(EventType.LeaveGroup, group);
			sendLeaveGroupEvent(event, group, groupChat, false);
		}

		if (groupChat == null) {// do not create chat history in DB
			String data = eventNotificationBuilder.createNotificationBuildingData(EventType.ExitGroup, group);
			groupChat = new GroupChat(group.getCreatedById(), group.getId(), "", data,
					ChatMessageType.SystemMessage.getId(), System.currentTimeMillis(), exitMemberId,
					EventNotificationType.MemberRemoved.getId());
		}
		List<Integer> memberIds = new ArrayList<>();
		memberIds.add(exitMemberId);
		videokycService.removeAgentFromQueue(memberIds, group.getId().longValue());
		sendExitGroupEvent(event, group, groupChat);
	}

	private void leaveGroup(Event event) {
		Integer leftMemberId = Integer.valueOf(event.getData().get("MemberId"));
		Group group = getGroup(event);
		group.setAffectedMemberId(leftMemberId);
		GroupChat groupChat = chatHistoryService.createGroupChatHistory(EventType.LeaveGroup, group);
		sendLeaveGroupEvent(event, group, groupChat, true);
		List<Integer> memberIds = new ArrayList<>();
		memberIds.add(leftMemberId);
		videokycService.removeAgentFromQueue(memberIds, group.getId().longValue());
	}

	private void closeGroup(Event event) {
		Group group = getGroup(event);
		GroupChat groupChat = chatHistoryService.createGroupChatHistory(EventType.CloseGroup, group);
		//	List<GroupMember> memberObjectList = group.getMembers();
		sendCloseGroupEvent(event, group, groupChat);
		List<Integer> memberIds = cacheGroupDao.getGroupMembers(group);
		videokycService.removeAgentFromQueue(memberIds, group.getId().longValue());
	}

	private void userUpdate(Event event) {
		Integer userId = Integer.valueOf(event.getData().get("Id"));
		ObjectNode node = getJsonEventNode(event);
		node.put("id", userId);
		String eventSubType = event.getData().get("metadata");
		if (eventSubType != null) {
			ObjectNode data = Json.newObject();
			data.put("eventSubType", eventSubType);
			node.put("metadata", data);
		}
		logger.info("user update for id: " + userId);

		String[] role = cacheService.getUserRole(userId);
		if (Arrays.asList(role).contains(RoleName.VideoKYCAgent.getName())) {
			videokycService.updateAgentStatusInAgentQueue(userId);
		}
		sendUserUpdateEventToContacts(userId, node);
		userConnectionService.sendMessageToActor(userId, node, null);
	}

	private void contactAddOrg(Event event) {
		Integer Id = Integer.valueOf(event.getData().get("Id").toString());
		event.setType(EventType.ContactAdd.getId());

		ObjectNode node1 = getJsonEventNode(event);
		node1.put("id", Id);

		// BULK
		List<Integer> contacts = cacheService.getAllContacts(Id);
		if (contacts != null && !contacts.isEmpty()) {
			Set<Integer> members = new HashSet<>(contacts);
			userConnectionService.sendMessageToActorSet(members, node1, null);
		}
	}

	private void contactAdd(Event event) {
		Integer Id1 = Integer.valueOf(event.getData().get("Id1"));
		Integer Id2 = Integer.valueOf(event.getData().get("Id2"));
		logger.debug("event: Id1: " + Id1 + " , Id2: " + Id2);
		// send online presence of Id2 to Id1
		ObjectNode node = getJsonEventNode(event);
		if (EventType.ContactAdd.getId().equals(event.getType())) {
			// TODO changed true to false in getPresence() call so as to send presence as
			// unavailable
			Presence presence = presenceService.getPresence(Id2, false);
			node.put("presence", Json.toJson(presence));
		}
		node.put("id", Id2);
		userConnectionService.sendMessageToActor(Id1, node, null);

		// send online presence of Id1 to Id2
		ObjectNode node1 = getJsonEventNode(event);
		if (EventType.ContactAdd.getId().equals(event.getType())) {
			Presence presence = presenceService.getPresence(Id1, true);
			node1.put("presence", Json.toJson(presence));
		}
		node1.put("id", Id1);
		userConnectionService.sendMessageToActor(Id2, node1, null);
	}

	public ChatMessage getChatMessage(Group group) {
		ChatMessage message = new ChatMessage();
		message.setActive(true);
		message.setType(MessageType.Chat.getId());
		message.setSubtype(ChatType.GroupChat.getId().intValue());
		message.setTo(group.getId());
		if(group.getGuestUserId() !=null)
			message.setFrom(group.getGuestUserId());
		message.setUtcDate(System.currentTimeMillis());
		return message;
	}

	@Override
	public void agentAssignedToVideoKyc(Event event) {
		Map<String, String> data = event.getData();
		Integer guestGroupId = Integer.valueOf(data.get("guestGroupId"));
		Integer agentId = Integer.valueOf(data.get("agentId"));

		logger.info("handle agent assigned");
		handleMemberAddRemove(event, agentId, guestGroupId);
	}

	private void handleMemberAddRemove(Event event, Integer userId, Integer guestGroupId) {
		logger.info("received agentAssigned event for user : " + userId);
		Group group = cacheGroupDao.getGroup(guestGroupId);
		group.setAffectedMemberId(userId);
		group.setId(guestGroupId);
		logger.info("send customerAssigned event to user : " + guestGroupId);

		GroupChat groupChatCreateGroupMember = chatHistoryService
				.createGroupChatHistory(EventType.UpdateGroup_AddMember, group);

		GroupChat groupChat = new GroupChat();
		groupChat.setSenderId(userId);
		groupChat.setCreatedDate(System.currentTimeMillis());
		groupChat.setChatMessageType(ChatMessageType.CustomerWelcomMessage.getId());
		groupChat.setGroupId(guestGroupId);

		logger.info("send customerAssigned event to user : " + guestGroupId);
		chatHistoryService.updateWelcomeMessage(groupChat);
		Map<String, String> data = event.getData();
		Integer KycId = Integer.valueOf(data.get("kycId"));
		ObjectNode kyc = cacheVideoKycDao.get(KycId);
		Integer orgId = kyc.findPath("organizationId").asInt();				
		JsonNode productPref = orgUtil.getPreferenceValue(orgId, "ProductPreference");
		logger.info("product pref  : " +productPref);
		/*if(productPref != null) {	
			logger.info("product   : " +kyc.findPath("product").asText());
			addYouTubeLinkInChatHistory(userId, group, productPref, kyc.findPath("productName").asText());
		}*/


		if (!data.containsKey("status")) {
			logger.info("status field is not present..adding status:" + VideoKYCStatus.AgentAssigned.getId().toString() + " to event");
			data.put("status", VideoKYCStatus.AgentAssigned.getId().toString());
		}
		event.setData(data);

		logger.info("send customerAssigned event to user : " + userId);
		groupChatMessageService.sendAddGroupEventMessage(event, group, groupChatCreateGroupMember);
	}

	protected void addYouTubeLinkInChatHistory(Integer userId, Group group, JsonNode productPreference, String product) {
		JsonNode youTubeLinkPreference = findProductPreference(productPreference, product, "youTubeLink");		
		logger.info("youTubeLinkPref {} " , youTubeLinkPreference);
		if (youTubeLinkPreference != null && youTubeLinkPreference.get("youTubeLink") != null) {
			logger.info("youTubeLink text {} " , youTubeLinkPreference.get("youTubeLink"));
			ChatMessage message = getMessage(group);
			message.setChatMessageType(ChatMessageType.Video.getId().intValue());
			if(userId != null) {
				message.setExcludedRecipients(userId.toString());
			}

			message.setText(youTubeLinkPreference.get("youTubeLink").asText());
			chatHistoryService.createGroupChatHistory(message, group);
			logger.info("chat history created successfully ");
		}
	}

	private void handleMemberExit(Integer userId, Integer guestGroupId) {
		Group group = cacheGroupDao.getGroup(guestGroupId);
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.Event.getId());
		node.put("subtype", EventType.ExitGroup.getId());
		node.put("grpId", guestGroupId);
		node.put("groupType", group.getGroupType());
		node.put("affMemId", userId);
		group.setAffectedMemberId(userId);
		group.setId(guestGroupId);
		GroupChat groupChatCreateGroupMember = chatHistoryService
				.createGroupChatHistory(EventType.ExitGroup, group);

		userConnectionService.sendMessageToActor(userId, node, null);
	}

	private Boolean isRecordingAvailable(Integer guestGroupId) {
		logger.info("isRecordingAvailable:  " + guestGroupId);
		Boolean available = false;
		List<Recording> recordingList = recordingService.getRecordingsByGroupId(guestGroupId);
		logger.info("recording available: " + recordingList.size());
		if (recordingList.isEmpty() && recordingList.size() == 0) {
			available = true;
		} else if (recordingList != null && recordingList.size() > 0) {
			for (Recording recording : recordingList) {
				if (recording.getAttachmentId() != null) {
					logger.info("recording available: " + recording.getAttachmentId());
					available = true;
					break;
				}
			}
		}
		return available;
	}

	private void sendSessionExpiredMessage(Integer loggedOutUserId, String reason, Integer status, Boolean signatureTaken) {
		//need to check videoKycgroup type & update queue set customer active 0
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.Event.getId());
		node.put("subtype", EventType.SessionExpired.getId());
		node.put("reason", reason);
		if (status != null) {
			node.put("videokyc_status", status);
		} if(signatureTaken != null) {
			node.put("signatureTaken", signatureTaken);
		}		
		userConnectionService.sendSessionExpiredMessageToActor(loggedOutUserId, node);
	}

	private void guestChatDeviceInfoUpdate(Event event) {
		logger.info("creating device info update system message for gu and ru");
		Group group = getGroup(event);
		// GroupChat DeviceInfoUpdatesystemMessage =
		// creatSystemMessageForNewGuestGroup(group,event);
		GroupChat DeviceInfoUpdatesystemMessage = createDeviceInfoSystemMessage(event, group);
		logger.info("created system message");
		groupChatMessageService.sendChatGuestBrowserInfoEvent(EventType.GuestChatDeviceInfoUpdate, group,
				DeviceInfoUpdatesystemMessage);
		logger.info("done with sendChatGuestDeviceInfoUpdate");
		sendChatGuestDeviceInfoUpdate(event, group, DeviceInfoUpdatesystemMessage);
	}

	private GroupChat createDeviceInfoSystemMessage(Event event, Group group) {
		logger.info("In createDeviceInfoSystemMessage");
		ObjectMapper mapper = new ObjectMapper();
		String deviceInfo = event.getData().get("deviceInfo");
		GroupChat groupChatGuestDeviceInfoUpdate = null;
		logger.info("deviceInfo : " + deviceInfo);

		try {
			JsonNode jsonNode = mapper.readTree(deviceInfo);
			String os = jsonNode.get("vendor").asText();
			os = os != null && os.equalsIgnoreCase("Web") ? "Windows" : os;

			String[] browserStr = jsonNode.get("deviceName").asText().split(" ");
			String browser = "Chrome";
			Integer version = 0;
			if (browserStr != null) {
				if (browserStr.length == 1) {
					browser = browserStr[0].trim().isEmpty() ? browser : browserStr[0];
				} else {
					browser = browserStr[0];
					version = Integer.parseInt(browserStr[1]);
				}
			}

			logger.debug("os:" + os + "browserStr" + browserStr);

			ChatMessage message = new ChatMessage();
			ObjectNode node = Json.newObject();
			setChatMessageProperties(message, node, jsonNode, group, EventType.GuestChatDeviceInfoUpdate.getId());
			node.put("templateName", "browser.info");
			node.put("deviceBrowserInfo", os + "-" + browser + "-version " + version);
			message.setData(node);

			groupChatGuestDeviceInfoUpdate = chatHistoryService.createGroupChatHistory(message, group);

		} catch (Exception e) {
			logger.error("failed to parse deviceInfo json", e);

		}
		return groupChatGuestDeviceInfoUpdate;
	}

	private void sendChatGuestDeviceInfoUpdate(Event event, Group group, GroupChat groupChatGuestDeviceInfoUpdate) {
		logger.info("sending event sendChatGuestDeviceInfoUpdate ");
		List<Integer> recipients = cacheGroupDao.getGroupMembers(group);
		// ObjectNode node = getJsonEventNode(event);
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.Event.getId());
		node.put("subtype", EventType.AddGroup.getId());

		List<String> clientIds = CommonUtil.getSupportedClientIds(ChatType.GroupChat);

		// send to active users
		for (Integer recipient : recipients) {
			String message = eventNotificationBuilder.buildMessage(EventType.GuestChatDeviceInfoUpdate,
					groupChatGuestDeviceInfoUpdate.getData(), recipient);
			node.put("text", message);
			addSystemChatMsgToEventNode(node, groupChatGuestDeviceInfoUpdate);
			Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(recipient, node, null, clientIds);
			logger.info("sent message to Actor for " + msgReceiverClients.size() + " recipients");

			Boolean isSendPN = commonUtil.isAlwaysSendPushNofication(msgReceiverClients);
			if (isSendPN) {
				List<Integer> recs = new ArrayList<>();
				recs.add(recipient);
				Long utcDate = groupChatGuestDeviceInfoUpdate.getCreatedDate();
				Long mid = groupChatGuestDeviceInfoUpdate.getId();
				notificationService.sendMobileNotification(NotificationType.GuestChatDeviceInfoUpdate.getId().intValue(),
						group.getId(), group.getCreatedById(), null, message, null, utcDate, mid, recs, null, groupChatGuestDeviceInfoUpdate.getChatMessageType(),
						PushNotificationVisibility.Recipients);
				logger.info("sent MobileNotification for receiptent:" + recipient);
			}
		}
	}

	private GroupChat creatSystemMessageForNewGuestGroup(Group group, Event event) {
		logger.info("creating system message for gu and ru");

		ObjectMapper mapper = new ObjectMapper();
		String deviceInfo = event.getData().get("deviceInfo");
		logger.info("deviceInfo : " + deviceInfo);
		GroupChat groupChat = null;

		try {
			JsonNode jsonNode = mapper.readTree(deviceInfo);
			String os = jsonNode.get("vendor").asText();
			os = os != null && os.equalsIgnoreCase("Web") ? "Windows" : os;

			String[] browserStr = jsonNode.get("deviceName").asText().split(" ");
			String browser = "Chrome";
			Integer version = 0;
			if (browserStr != null) {
				if (browserStr.length == 1) {
					browser = browserStr[0].trim().isEmpty() ? browser : browserStr[0];
				} else {
					browser = browserStr[0];
					version = Integer.parseInt(browserStr[1]);
				}
			}

			logger.debug("os:" + os + "browserStr" + browserStr);

			ChatMessage message = new ChatMessage();
			ObjectNode node = Json.newObject();
			setChatMessageProperties(message, node, jsonNode, group, EventType.GuestChatDeviceInfo.getId());
			node.put("templateName", "browser.info");
			node.put("deviceBrowserInfo", os + "-" + browser + "-version " + version);
			message.setData(node);

			groupChat = chatHistoryService.createGroupChatHistory(message, group);

			/*
			 * message = new ChatMessage(); node = Json.newObject();
			 * setChatMessageProperties(message,node,jsonNode,group,EventType.
			 * GuestChatDeviceInfo.getId());
			 *
			 * Integer orgId = group.getCreatorOrganizationId(); Map<String,Integer>
			 * supportedBrowser = getSupportedBrowsers(orgId); String msgText =
			 * getOsBrowserVersionSpecificMessage(supportedBrowser, os, browser, version);
			 * node.put("templateName", msgText); message.setData(node);
			 * message.setExcludedRecipients(jsonNode.get("userId").asText());
			 *
			 * chatHistoryService.createGroupChatHistory(message, group);
			 */
		} catch (Exception e) {
			logger.error("failed to parse deviceInfo json", e);
		}
		return groupChat;
	}


	private GroupChat createSystemMessageForUpdateUseCaseStatus(Group group, Event event) {
		logger.info("create history of use case updated started");
		Group toGroup = group;
		Long time = System.currentTimeMillis();
		String date = commonUtil.getDateTimeWithTimeZone(time, null);

		ChatMessage message = new ChatMessage();
		message.setActive(true);
		message.setType(MessageType.Chat.getId());
		message.setSubtype(ChatType.GroupChat.getId().intValue());
		message.setTo(group.getId());
		message.setFrom(group.getCreatedById());
		message.setChatMessageType(ChatMessageType.SystemMessage.getId().intValue());
		message.setUtcDate(System.currentTimeMillis());

		Map<String, String> args = new HashMap<String, String>();
		JsonNode toUserJson = null;
		String messageText = null;
		String userName = null;

		String textMsg = PropertyUtil.getProperty(Constants.MESSAGE_FOR_UPDATE_USECASE_STATUS);
		String status = getUseCaseStatus(group.getId());
		args.put("UseCaseStatus", status);
		messageText = StrSubstitutor.replace(textMsg, args, "{", "}");
		message.setText(messageText);
		logger.debug("upadte use case system message:" + messageText);
		GroupChat dbMsg = chatHistoryService.createGroupChatHistory(message, toGroup);
		message.setMid(dbMsg.getId());
		return dbMsg;

	}

	private String getUseCaseStatus(Integer groupId) {
		String useCaseStatus = "";
		Group group = cacheGroupDao.getGroup(groupId);
		Byte status = group.getUseCaseStatus();
		logger.debug("useCaseStatusId:" + status);
		useCaseStatus = GroupUseCaseStatus.getGroupUseCaseStatus(status).toString();
		logger.info("useCaseStatus Name:" + useCaseStatus);
		return useCaseStatus;
	}

	private void setChatMessageProperties(ChatMessage message, ObjectNode node, JsonNode jsonNode, Group group, Integer eventType) {
		logger.debug("set chatMessageProperties");
		message.setActive(true);
		message.setType(MessageType.Chat.getId());
		message.setSubtype(ChatType.GroupChat.getId().intValue());
		message.setTo(group.getId());
		message.setFrom(group.getCreatedById());
		message.setChatMessageType(ChatMessageType.SystemMessage.getId().intValue());
		message.setUtcDate(System.currentTimeMillis());

		if (jsonNode != null) {
			node.put("guestUserId", jsonNode.get("userId"));
		}
		node.put("eventType", eventType);
		logger.debug("chatMessageProperties are set");
	}

	private void handleOpenChatEnabledChange(Event event, Group group) {
		group.setActionTakerId(Integer.valueOf(event.getData().get("ActionTakerId").toString()));
		GroupChat groupChat = chatHistoryService.createGroupChatHistory(EventType.OpenChatEnabledInGroup, group);
		List<Integer> recipients = cacheGroupDao.getGroupMembers(group);
		ObjectNode node = getJsonEventNode(event);
		node.put("grpId", group.getId());
		// send to active users
		for (Integer recipient : recipients) {
			String message = eventNotificationBuilder.buildMessage(EventType.OpenChatEnabledInGroup,
					groupChat.getData(), recipient);
			node.put("text", message);
			addSystemChatMsgToEventNode(node, groupChat);
			userConnectionService.sendMessageToActor(recipient, node, null);
		}

		// Send to self
		String message = eventNotificationBuilder.buildMessage(EventType.OpenChatEnabledInGroup, groupChat.getData(),
				group.getActionTakerId());
		node.put("text", message);
		addSystemChatMsgToEventNode(node, groupChat);
		userConnectionService.sendMessageToActor(group.getActionTakerId(), node, null);
	}


	private ObjectNode createPushNotificationJSONMemo(Memo memo) {
		ObjectNode data = Json.newObject();
		data.put("memoId", memo.getId());
		data.put("senderId", memo.getCreatedById());
		data.put("name", memo.getCreator().getName());
		data.put("subtype", NotificationType.Memo.getId());
		data.put("alert", "New Memo: " + memo.getSubject());	
		return createPushNotificationJSON(data);
	}

	private ObjectNode createPushNotificationJSON(ObjectNode data) {
		ObjectNode pushNotification = Json.newObject();
		ObjectNode aps = Json.newObject();
		aps.put("alert", data.get("alert"));
		aps.put("sound", "default");
		data.put("type", MessageType.Notification.getId());
		data.put("utcDate", System.currentTimeMillis());
		pushNotification.set("aps", aps);
		pushNotification.set("data", data);
		return pushNotification;
	}

	private Memo getMemo(Event event) {
		Memo memo = null;
		if (event.getData().get("Id") != null && event.getData().get("RecipientIds") != null
				&& event.getData().get("CreatedById") != null && event.getData().get("Subject") != null) {
			memo = new Memo();
			memo.setId(Integer.valueOf(event.getData().get("Id").toString()));
			memo.setCreatedById(Integer.valueOf(event.getData().get("CreatedById").toString()));
			memo.setSubject(event.getData().get("Subject").toString());

			String strRecipients = event.getData().get("RecipientIds").toString();
			List<String> strIdList = Arrays.asList(strRecipients.split(","));
			List<Long> castedIds = new ArrayList<>();
			for (String strId : strIdList) {
				castedIds.add(Long.parseLong(strId));
			}
			memo.setRecipientIds(castedIds);
		} else {
			logger.error("Id and RecipientIds can not bu null.");
		}
		return memo;
	}

	private void handleDeleteUser(Event event, Integer deletedUserId) {
		// sets all chat messages (1To1+Group) as read ad also sets the unreadMsg count
		// to 0
		chatHistoryService.deleteChatHistory(deletedUserId);

		// send event to all contacts as this user is deleted
		String strContacts = event.getData().get("ContactIds") == null ? null
				: event.getData().get("ContactIds").toString();
		logger.debug("closeing All User Connection for user " + deletedUserId + "  Received contcats from IMS as : "
				+ strContacts);
		ObjectNode node = getDeleteUserJsonEventNode(deletedUserId);
		// send to self
		userConnectionService.sendMessageToActor(deletedUserId, node, null);
		// send to all
		if (strContacts != null && !strContacts.trim().isEmpty()) {
			String[] contactIds = strContacts.split(",");
			Set<Integer> members = new HashSet<>();
			for (String contactId : contactIds) {
				members.add(Integer.valueOf(contactId));
			}
			userConnectionService.sendMessageToActorSet(members, node, null);
		}
		userConnectionService.closeAllUserConnection(deletedUserId);
	}

	private ObjectNode getDeleteUserJsonEventNode(Integer deletedUserId) {
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.Event.getId());
		node.put("subtype", EventType.DeleteUser.getId());
		node.put("id", deletedUserId);
		return node;
	}

	private void handleGroupMemberDelete(Event event, Integer deletedUserId) {
		// only Chat history would be created for all of the groups where the user is
		// member.No leave/exit event will be send, all contacts will get delete user
		// event UI to handle group UI accordingly
		String groupInfo = event.getData().get("DeletedUserGroups") == null ? null
				: event.getData().get("DeletedUserGroups").toString();
		if (groupInfo != null && !groupInfo.trim().isEmpty()) {
			List<Group> groups = getGroupsOfDeletedUser(groupInfo, deletedUserId);
			GroupChat groupChat = null;
			for (Group group : groups) {
				if (group.getCreatedById().intValue() == deletedUserId.intValue()) {// send close & leave event if
					// deleted user is creator
					if (group.getGroupStatus() == 1) {// and group is in open status
						logger.debug("sending group close event for group :  " + group.getId());
						event.setType(EventType.CloseGroup.getId());
						group.setActionTakerId(group.getCreatedById());
						groupChat = chatHistoryService.createGroupChatHistory(EventType.CloseGroup, group);
						sendCloseGroupEvent(event, group, groupChat);
					} else {// No close event if group is already closed

					}

					if (group.getDeletedMemberStatus() == 1) {// send leave event only iff deleted user is in active
						// status
						logger.debug("sending group member left event for group :  " + group.getId());
						event.setType(EventType.LeaveGroup.getId());
						groupChat = chatHistoryService.createGroupChatHistory(EventType.LeaveGroup, group);
						sendLeaveGroupEvent(event, group, groupChat, false);
					} else {// No leave event to be sent

					}

				} else {// send Leave event only if deleted user is member
					if (group.getDeletedMemberStatus() == 1) {// send leave event only iff deleted user is in active
						// status
						logger.debug("sending group member left event for group :  " + group.getId());
						event.setType(EventType.LeaveGroup.getId());
						groupChat = chatHistoryService.createGroupChatHistory(EventType.LeaveGroup, group);
						sendLeaveGroupEvent(event, group, groupChat, false);
					} else {// No leave event to be sent

					}
				}
			}
		}
	}

	private List<Group> getGroupsOfDeletedUser(String strGroupInfo, Integer deletedUserId) {
		List<Group> groups = new ArrayList<Group>();
		String[] groupInfoArray = strGroupInfo.split(",");

		Group group = null;
		Integer groupId = null;
		Byte groupStatus = null;
		Byte deletedMemberStatus = null;
		for (String groupInfo : groupInfoArray) {
			String[] groupDetails = groupInfo.split(":");
			groupId = Integer.valueOf(groupDetails[0]);
			groupStatus = Byte.valueOf(groupDetails[1]);
			deletedMemberStatus = Byte.valueOf(groupDetails[2]);
			group = cacheGroupDao.getGroup(groupId);
			group.setId(groupId);
			group.setAffectedMemberId(deletedUserId);
			group.setGroupStatus(groupStatus);
			group.setDeletedMemberStatus(deletedMemberStatus);

			groups.add(group);
		}
		return groups;
	}

	private void handleGroupMemberAddition(Event event, Group group, String addedMemberIds) {
		List<Integer> recipients = cacheGroupDao.getGroupMembers(group);
		List<Integer> memberIds = convertToList(addedMemberIds);
		ObjectNode node = getJsonEventNode(event);
		node.put("grpId", group.getId());
		node.put("grpCrtId", group.getCreatedById());
		node.put("groupName", group.getName());
		node.put("groupType", group.getGroupType());
		JsonNode userJson = cacheUserDao.find(group.getCreatedById());
		String name = userJson.findPath("firstName").asText();
		List<String> clientIds = CommonUtil.getSupportedClientIds(ChatType.GroupChat);

		if (name != null) {
			node.put("name", name);
		}

		logger.info("add agent in queue started here");
		videokycService.addAgentInQueue(memberIds, group.getId().longValue());

		for (Integer memberId : memberIds) {
			group.setAffectedMemberId(memberId);
			node.put("affMemId", memberId);
			GroupChat groupChatCreateGroupMember = chatHistoryService.createGroupChatHistory(EventType.UpdateGroup_AddMember, group);
			// send to active users
			for (Integer recipient : recipients) {
				//dont send to Guest User
				if (!recipient.equals(group.getGuestUserId())) {
					String message = eventNotificationBuilder.buildMessage(EventType.UpdateGroup_AddMember,
							groupChatCreateGroupMember.getData(), recipient);
					node.put("text", message);
					addSystemChatMsgToEventNode(node, groupChatCreateGroupMember);
					Set<String> msgReceiverClients = userConnectionService.sendMessageToActor(recipient, node, null,
							clientIds);
					Boolean isSendPN = commonUtil.isAlwaysSendPushNofication(msgReceiverClients);
					if (isSendPN) {
						List<Integer> recs = new ArrayList<>();
						recs.add(recipient);
						Long utcDate = groupChatCreateGroupMember.getCreatedDate();
						Long mid = groupChatCreateGroupMember.getId();
						notificationService.sendMobileNotification(NotificationType.AddGroupMember.getId().intValue(),
								group.getId(), group.getCreatedById(), null, message, null, utcDate, mid, recs, null, groupChatCreateGroupMember.getChatMessageType(),
								PushNotificationVisibility.Recipients);
					}
				}
			}
		}

	}

	private void handleSystemGroupMemberAddition(Group group, String addedmemberId) {
		List<Integer> memberIds = convertToList(addedmemberId);
		logger.info("added member Ids: " + memberIds);
		logger.info("add agent in queue started here");
		videokycService.addAgentInQueue(memberIds, group.getId().longValue());
		group.setAffectedMemberId(memberIds.get(0));
		chatHistoryService.createGroupChatHistory(EventType.UpdateGroupBySystem, group);
	}


	private void handleSystemGroupMemberRemoval(Group group, String reovedMemberIds) {
		List<Integer> memberIds = convertToList(reovedMemberIds);
		logger.info("REMOVED member Ids: " + memberIds);
		videokycService.removeAgentFromQueue(memberIds, group.getId().longValue());
		group.setAffectedMemberId(memberIds.get(0));
		chatHistoryService.createGroupChatHistory(EventType.UpdateGroupBySystem_RemoveMember, group);
	}


	private void addSystemChatMsgToEventNode(ObjectNode node, GroupChat groupChat) {
		node.put("utcDate", groupChat.getCreatedDate());
		if (groupChat.getId() != null) {
			node.put("mid", groupChat.getId());
		}
	}

	private void handleGroupNameChange(Event event, Group group, String oldName, String newName) {
		group.setOldName(oldName);
		group.setNewName(newName);
		GroupChat groupChatChangeName = chatHistoryService.createGroupChatHistory(EventType.UpdateGroup_ChangeGroupName,
				group);
		List<Integer> recipients = cacheGroupDao.getGroupMembers(group);
		ObjectNode node = getJsonEventNode(event);
		node.put("grpId", group.getId());
		// send to active users
		for (Integer recipient : recipients) {
			String message = eventNotificationBuilder.buildMessage(EventType.UpdateGroup_ChangeGroupName,
					groupChatChangeName.getData(), recipient);
			node.put("text", message);
			addSystemChatMsgToEventNode(node, groupChatChangeName);
			userConnectionService.sendMessageToActor(recipient, node, null);
		}
	}

	private void handleGroupMemberRemoval(Event event, Group group, String reovedMemberIds) {
		List<Integer> recipients = cacheGroupDao.getGroupMembers(group);
		List<Integer> memberIds = convertToList(reovedMemberIds);

		ObjectNode node = getJsonEventNode(event);
		node.put("grpId", group.getId());
		node.put("groupType", group.getGroupType());
		videokycService.removeAgentFromQueue(memberIds, group.getId().longValue());
		for (Integer memberId : memberIds) {
			group.setAffectedMemberId(memberId);
			node.put("affMemId", memberId);
			GroupChat groupChat = chatHistoryService.createGroupChatHistory(EventType.UpdateGroup_RemoveMember, group);

			// send to active users
			for (Integer recipient : recipients) {
				//Dont send to guest
				if (!recipient.equals(group.getGuestUserId())) {
					String message = eventNotificationBuilder.buildMessage(EventType.UpdateGroup_RemoveMember,
							groupChat.getData(), recipient);
					node.put("text", message);
					addSystemChatMsgToEventNode(node, groupChat);
					userConnectionService.sendMessageToActor(recipient, node, null);
				}
			}

			String message = eventNotificationBuilder.buildMessage(EventType.UpdateGroup_RemoveMember,
					groupChat.getData(), memberId);
			node.put("text", message);
			addSystemChatMsgToEventNode(node, groupChat);
			userConnectionService.sendMessageToActor(memberId, node, null);
		}
	}

	private void handleGroupMemberRoleChange(Event event, Group group, String changedRoleMemberIds) {
		List<Integer> recipients = cacheGroupDao.getGroupMembers(group);
		List<Integer> memberIds = convertToList(changedRoleMemberIds);

		ObjectNode node = getJsonEventNode(event);
		node.put("grpId", group.getId());
		for (Integer memberId : memberIds) {
			group.setAffectedMemberId(memberId);
			node.put("affMemId", memberId);
			GroupMember member = cacheGroupDao.getGroupMember(group, memberId);
			GroupChat groupChat = null;
			EventType eventType = EventType.UpdateGroup_AddAdmin;
			if (member.getMemberRole().byteValue() == 2) {
				eventType = EventType.UpdateGroup_RemoveAdmin;
			}
			groupChat = chatHistoryService.createGroupChatHistory(eventType, group);

			// send to active users
			for (Integer recipient : recipients) {
				if (!recipient.equals(group.getGuestUserId())) {
					String message = eventNotificationBuilder.buildMessage(eventType, groupChat.getData(), recipient);
					node.put("text", message);
					addSystemChatMsgToEventNode(node, groupChat);
					userConnectionService.sendMessageToActor(recipient, node, null);
				}
			}
		}
	}

	private void sendExitGroupEvent(Event event, Group group, GroupChat groupChat) {
		Integer leftMemberId = group.getAffectedMemberId();
		ObjectNode node = getJsonEventNode(event);
		node.put("grpId", group.getId());
		node.put("affMemId", leftMemberId);
		String message = eventNotificationBuilder.buildMessage(EventType.ExitGroup, groupChat.getData(), leftMemberId);
		node.put("text", message);
		addSystemChatMsgToEventNode(node, groupChat);
		userConnectionService.sendMessageToActor(leftMemberId, node, null);
	}

	private void sendLeaveGroupEvent(Event event, Group group, GroupChat groupChat, boolean sendToSelf) {
		Integer leftMemberId = group.getAffectedMemberId();
		List<Integer> groupMembers = cacheGroupDao.getGroupMembers(group);
		ObjectNode node = getJsonEventNode(event);
		node.put("grpId", group.getId());
		node.put("affMemId", leftMemberId);
		if (groupMembers != null && !groupMembers.isEmpty()) {
			for (Integer receipinetMemberId : groupMembers) {// send message to all group members
				String message = eventNotificationBuilder.buildMessage(EventType.LeaveGroup, groupChat.getData(),
						receipinetMemberId);
				node.put("text", message);
				addSystemChatMsgToEventNode(node, groupChat);
				userConnectionService.sendMessageToActor(receipinetMemberId, node, null);
			}
		}
		if (sendToSelf) {// Send to self
			String message = eventNotificationBuilder.buildMessage(EventType.LeaveGroup, groupChat.getData(),
					leftMemberId);
			node.put("text", message);
			addSystemChatMsgToEventNode(node, groupChat);
			userConnectionService.sendMessageToActor(leftMemberId, node, null);
		}
	}

	private void sendCloseGroupEvent(Event event, Group group, GroupChat groupChat) {
		List<Integer> groupMembers = cacheGroupDao.getGroupMembers(group);
		if (groupMembers != null && !groupMembers.isEmpty()) {
			ObjectNode node = getJsonEventNode(event);
			node.put("grpId", group.getId());
			for (Integer receipinetMemberId : groupMembers) {// send message to all group members
				String message = eventNotificationBuilder.buildMessage(EventType.CloseGroup, groupChat.getData(),
						receipinetMemberId);
				node.put("text", message);
				addSystemChatMsgToEventNode(node, groupChat);
				userConnectionService.sendMessageToActor(receipinetMemberId, node, null);
			}
		}
	}

	private void sendEventToAllContacts(Integer Id, ObjectNode node) {
		List<Integer> contacts = cacheService.getAllContacts(Id);
		if (contacts != null && !contacts.isEmpty()) {
			Set<Integer> members = new HashSet<>(contacts);
			userConnectionService.sendMessageToActorSet(members, node, null);
		}
	}

	private void sendUserUpdateEventToContacts(Integer Id, ObjectNode node) {
		logger.info("send user update event to contact whose windows are open for Id: " + Id);
		List<Integer> contactUsers = new ArrayList<Integer>();
		JsonNode openWindowObject = cacheService.getUserObjectWithOpenChatWindow(Id);

		if (openWindowObject != null && !openWindowObject.isMissingNode()) {
			JsonNode contactIds = openWindowObject.get("users");
			logger.debug("Getting array of users who has to send presence " + contactIds);
			if (contactIds != null && !contactIds.isMissingNode()) {
				for (JsonNode connectionNode : contactIds) {
					Integer contactId = connectionNode.asInt();
					logger.debug("send user update event to contact: " + contactId);
					;
					userConnectionService.sendMessageToActor(contactId, node, null);
					contactUsers.add(contactId);
				}
			}
			JsonNode groupIds = openWindowObject.get("guestGroups");
			logger.info("Getting array of guestGroups " + groupIds);
			if (groupIds != null && !groupIds.isMissingNode()) {
				for (JsonNode groupIdNode : groupIds) {
					Integer guestGroupId = groupIdNode.asInt();
					logger.debug("send presence of registered user to guest user: " + guestGroupId);
					Group group = cacheService.getGroupDetails(guestGroupId);
					// For GuestDirectGroup Chat send customStatus of creator only
					if (group.getGroupType().equals(GroupType.GuestDirectGroupChat.getId().byteValue())) {
						userConnectionService.sendMessageToActor(group.getGuestUserId(), node, null);
						logger.debug("sent userupdate event To guest " + group.getGuestUserId());
					}
				}
			}
		}
		Set<Integer> contacts1 = cacheService.getUsersWithOpenMapWindow(Id);
		// remove contacts event sent to them already
		contacts1.removeAll(contactUsers);
		if (contactUsers != null && !contactUsers.isEmpty()) {
			logger.info("contactUsers " + contactUsers);
			for (Integer contactId : contactUsers) {
				int sentCount = userConnectionService.sendMessageToActorWithOpenMapWindow(contactId, node);
				if (sentCount == 0) {
					cacheOpenMapInfoDao.remove(contactId);
				}
			}
		}
	}

	private void deviceUpdated(Event event) {
		Integer entityId = Integer.valueOf(event.getData().get("EntityId").toString());
		Integer userCategory = Integer.valueOf(event.getData().get("UserCategory").toString());
		if (userCategory.equals(UserCategory.Guest.getId())) {
			Integer groupId = Integer.valueOf(event.getData().get("groupId").toString());
			List<Integer> recipients = cacheGroupDao.getGroupMembers(groupId);
			ObjectNode node = getJsonEventNode(event);
			node.put("grpId", groupId);
			// send to active users
			node.put("userId", entityId);
			Set<Integer> members = new HashSet<>(recipients);
			userConnectionService.sendMessageToActorSet(members, node, null);
		} else if (userCategory.equals(UserCategory.Employee.getId())) {
			List<Integer> contactUsers = cacheService.getUsersWithOpenChatWindow(entityId);
			ObjectNode node = getJsonEventNode(event);
			if (contactUsers != null && !contactUsers.isEmpty()) {
				node.put("userId", entityId);
				for (Integer contactId : contactUsers) {
					userConnectionService.sendMessageToActor(contactId, node, null);
				}
			}
		}
	}

	private ObjectNode getJsonEventNode(Event event) {
		ObjectNode node = Json.newObject();
		node.put("type", MessageType.Event.getId());
		node.put("subtype", event.getType());
		return node;
	}

	private Group getGroup(Event event) {
		Integer groupId = Integer.valueOf(event.getData().get("GroupId").toString());
		Group group = cacheGroupDao.getGroup(groupId);
		if (group == null) {
			throw new BadRequestException(ErrorCode.InvalidGroup, groupId);
		}
		group.setId(groupId);
		if (event.getData().containsKey("ActionTakerId")) {
			Integer actionTakerId = Integer.valueOf(event.getData().get("ActionTakerId").toString());
			group.setCreatedById(actionTakerId);
		}
		return group;
	}

	private List<Integer> convertToList(String strMemberIds) {
		if (strMemberIds.contains("[")) {
			strMemberIds = strMemberIds.replace("[", "");
		}
		if (strMemberIds.contains("]")) {
			strMemberIds = strMemberIds.replace("]", "");
		}
		strMemberIds = strMemberIds.trim();
		String[] strMemberIdArray = strMemberIds.split(",");
		List<Integer> memberIds = new ArrayList<Integer>();
		for (String strMemberId : strMemberIdArray) {
			memberIds.add(Integer.valueOf(strMemberId.trim()));
		}
		return memberIds;
	}

	private List<Attachment> assignAttachmentRecipient(Long groupId, String attachmentId, String entityType, Integer createdById) {
		return groupChatDao.assignAttachmentRecipient(groupId, attachmentId, entityType, createdById);
	}


	private Map<String, Integer> getSupportedBrowser(String[] browsersVersions) {
		Map<String, Integer> browsersVersionsMap = new HashMap<>();
		if (browsersVersions != null && browsersVersions.length > 0) {
			for (String browsersVersion : browsersVersions) {
				logger.info("browsersVersion:" + browsersVersion);
				String[] versions = browsersVersion.split(":");
				if (versions != null && versions.length > 0) {
					browsersVersionsMap.put(versions[0], Integer.valueOf(versions[1]));
				}
			}
		}
		return browsersVersionsMap;
	}

	private GroupChat createVideoKycHistoryV0(Event event, GroupChat groupChatCreateGroup) {
		Group group = getGroup(event);
		String videoKycStr = event.getData().get("videoKYC");
		logger.debug("received Video KYC string : " + videoKycStr);
		ChatMessage message = new ChatMessage();
		message.setActive(true);
		message.setType(MessageType.Chat.getId());
		message.setSubtype(ChatType.GroupChat.getId().intValue());
		message.setTo(group.getId());
		message.setFrom(group.getCreatedById());
		message.setChatMessageType(ChatMessageType.ChatMessage.getId().intValue());
		message.setUtcDate(System.currentTimeMillis());

		try {
			ObjectMapper mapper = new ObjectMapper();
			logger.info("videoKycStr : " + videoKycStr);

			JsonNode jsonNode = mapper.readTree(videoKycStr);
			JsonNode trackingId = jsonNode.get("trackingId");
			boolean showMessageToGuest = true;
			Integer organizationId = group.getCreatorOrganizationId();
			;
			JsonNode orgJson = cacheService.getOrgJson(organizationId);
			JsonNode prefs = orgJson.findPath("settings");
			if (prefs != null && prefs.isArray()) {
				for (JsonNode pref : prefs) {
					if ("ShowSignupHistoryToGuest".equalsIgnoreCase(pref.findPath("preference").asText())) {
						showMessageToGuest = Boolean.valueOf(pref.findPath("value").asText());
						break;
					}
				}
			}

			if (!showMessageToGuest) {
				logger.info("Exclude Guest userId : " + group.getGuestUserId());
				message.setExcludedRecipients(group.getGuestUserId().toString());
			}
			if (trackingId != null) {
				// JsonNode trackingIdValue = mapper.readTree(trackingId.asText());
				String text = "";
				text = "TrackingId" + " : " + trackingId.asText();
				logger.info("Get tracking Id from videoKYC : " + trackingId.asText());
				message.setText(text);
				GroupChat videoDisplayKycChat = null;
				logger.info("Create Group Chat histoty of message : " + message);
				videoDisplayKycChat = chatHistoryService.createGroupChatHistory(message, group);
				logger.info("successfully Tracking Id stored in chat history");
				groupChatCreateGroup = videoDisplayKycChat;
			}


			// Chat history put video msg
			JsonNode displayKycNode = jsonNode.get("displayKycProperties");
			logger.info("Get displayKycProperties from videoKYC: " + displayKycNode.asText());
			if (displayKycNode != null) {
				JsonNode kycTextNode = mapper.readTree(displayKycNode.asText());
				String text = "";
				for (Iterator<Map.Entry<String, JsonNode>> it = kycTextNode.fields(); it.hasNext(); ) {
					Map.Entry<String, JsonNode> field = it.next();
					String key = field.getKey();
					JsonNode value = field.getValue();
					text = text + key + " : " + value.asText() + "\n";
				}
				if (group.getGroupType() == GroupType.GuestGroupChat.getId().byteValue()) {
					message.setFrom(group.getGuestUserId());
				}
				message.setText(text);
				logger.info("Created text for displayKycProperties text message: " + text);
				message.setUtcDate(System.currentTimeMillis());
				GroupChat videoDisplayKycChat = null;
				videoDisplayKycChat = chatHistoryService.createGroupChatHistory(message, group);
				logger.info("successfully displayKycProperties stored in chat history");
				groupChatCreateGroup = videoDisplayKycChat;
			}

			JsonNode attachmentNode = jsonNode.get("attachmentsIds");
			logger.info("Get attachmentsIds from videoKYC: " + attachmentNode);
			if (attachmentNode != null) {
				String idsStr = attachmentNode.asText();
				logger.info("Get attachmentsIds from videoKYC: " + idsStr);
				if (idsStr != null) {
					List<Attachment> attachmentList = assignAttachmentRecipient(Long.valueOf(group.getId()), idsStr, "ChatGroup", group.getCreatedById());
					logger.debug("Update attachment recipient in owb successfully.");
					if (attachmentList != null && !attachmentList.isEmpty()) {
						for (Attachment attachment : attachmentList) {
							try {
								logger.debug("Update attachment recipient in owb successfully.");
								message.setText("1 file(s) attached");
								ObjectNode node = Json.newObject();
								node.put("type", 1);
								ArrayNode arrayNode = mapper.createArrayNode();
								ObjectNode objectNode = mapper.createObjectNode();
								objectNode.put("id", attachment.getId());
								objectNode.put("fileName", attachment.getFileName());
								String attachmentName = attachment.getFileName();
								String extension = attachmentName.split("[.]")[1];
								logger.debug("put attachment extension as: " + extension);
								objectNode.put("fileSize", attachment.getFileSize());
								objectNode.put("fileExt", extension);
								if (attachment.getThumbnailWidth() != null
										&& attachment.getThumbnailHeight() != null
										&& Integer.parseInt(attachment.getThumbnailWidth()) > 0
										&& Integer.parseInt(attachment.getThumbnailHeight()) > 0) {
									objectNode.put("isThumbnail", "true");
									logger.debug("attachment has thumbnail then set extension as : " + "image");
									objectNode.put("fileExt", "image");
								} else {
									objectNode.put("isThumbnail", "false");
								}
								objectNode.put("docType", attachment.getDocType());
								arrayNode.add(objectNode);
								node.put("attachments", arrayNode);
								message.setData(node);
								message.setUtcDate(System.currentTimeMillis());
								GroupChat videoKycChat = null;
								videoKycChat = chatHistoryService.createGroupChatHistory(message, group);
								logger.debug("attachment message added successfully in Group");
								groupChatCreateGroup = videoKycChat;
							} catch (Exception e) {
								logger.error("failed to map video kyc json string ", e);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("failed to map video kyc json string ", e);
		}
		return groupChatCreateGroup;
	}

	private GroupChat createVideoKycHistory(Event event, GroupChat groupChatCreateGroup) {
		Group group = getGroup(event);
		String videoKycStr = event.getData().get("VideoKYC");
		logger.debug("received Video KYC string : " + videoKycStr);
		ChatMessage message = getMessage(group);

		try {

			String text = videoKycChatHistory(videoKycStr, message, group);
			logger.info("successfully videoKyc Properties stored in chat history");
			//	text = videoKycDisplayChatHistory(videoKycStr, message, text);
			logger.info("successfully videoKyc Properties stored in chat history");
			message.setExcludedRecipients(group.getGuestUserId().toString());
			message.setText(text);
			message.setChatMessageType(ChatMessageType.CustomerDetails.getId().intValue());
			logger.info("Created text for videoKycs text message: ");
			message.setUtcDate(System.currentTimeMillis());
			chatHistoryService.createVideoKycGroupChatHistory(message, group);
			ObjectMapper mapper = new ObjectMapper();
			String signatureAttachment = event.getData().get("SignatureAttachment");
			if (signatureAttachment != null) {
				logger.info("------------Create Signature Attachment--------- ");
				Attachment attachment = mapper.readValue(signatureAttachment, Attachment.class);
				message.setChatMessageType(ChatMessageType.SentFromRE.getId().intValue());
				GroupChat videoKycChat = createAttachmentChatHistory(group, message, attachment);
				groupChatCreateGroup = videoKycChat;
			}
			String panAttachment = event.getData().get("PanAttachment");
			if (panAttachment != null) {
				logger.info("Create history for attachment started. ");
				logger.info("------------panAttachment--------- ");
				Attachment attachment = mapper.readValue(panAttachment, Attachment.class);
				message.setChatMessageType(ChatMessageType.SentFromRE.getId().intValue());
				GroupChat videoKycChat = createAttachmentChatHistory(group, message, attachment);
				groupChatCreateGroup = videoKycChat;
			}
			String customerAttachment = event.getData().get("CustomerAttachment");
			if (customerAttachment != null) {
				logger.info("Create history for attachment started. ");
				logger.info("------------CustomerAttachment--------- ");
				Attachment attachment = mapper.readValue(customerAttachment, Attachment.class);
				message.setChatMessageType(ChatMessageType.SentFromRE.getId().intValue());
				GroupChat videoKycChat = createAttachmentChatHistory(group, message, attachment);
				groupChatCreateGroup = videoKycChat;
			}
			String attachmentStr = event.getData().get("Attachment");
			if (attachmentStr != null) {
				logger.info("Create history for attachment started. ");
				logger.info("------------Addhar Photo--------- ");
				Attachment attachment = mapper.readValue(attachmentStr, Attachment.class);
				message.setChatMessageType(ChatMessageType.CustomerDetails.getId().intValue());
				GroupChat videoKycChat = createAttachmentChatHistory(group, message, attachment);
				groupChatCreateGroup = videoKycChat;
			}


		} catch (Exception e) {
			logger.error("failed to map video kyc json string ", e);
		}

		return groupChatCreateGroup;
	}

	public ChatMessage getMessage(Group group) {
		ChatMessage message = new ChatMessage();
		message.setActive(true);
		message.setType(MessageType.Chat.getId());
		message.setSubtype(ChatType.GroupChat.getId().intValue());
		message.setTo(group.getId());
		message.setFrom(group.getGuestUserId());
		message.setUtcDate(System.currentTimeMillis());
		message.setChatMessageType(ChatMessageType.CustomerDetails.getId().intValue());
		return message;
	}

	private GroupChat createAttachmentChatHistory(Group group, ChatMessage message,
			Attachment attachment) {
		ObjectMapper mapper = new ObjectMapper();
		logger.debug("UCreate chat history for attachment.");
		message.setText("1 file(s) attached");
		ObjectNode node = Json.newObject();
		node.put("type", 1);
		ArrayNode arrayNode = mapper.createArrayNode();
		ObjectNode objectNode = mapper.createObjectNode();
		objectNode.put("id", attachment.getId());
		objectNode.put("fileName", attachment.getFileName());
		String attachmentName = attachment.getFileName();
		String extension = attachmentName.split("[.]")[1];
		logger.debug("put attachment extension as: " + extension);
		objectNode.put("fileSize", attachment.getFileSize());
		objectNode.put("isThumbnail", attachment.getIsThumbnail());
		if (attachment.getIsThumbnail() == true) {
			objectNode.put("fileExt", "image");
		} else {
			objectNode.put("fileExt", extension);
		}
		objectNode.put("docType", attachment.getDocType());
		objectNode.put("docTypeText", attachment.getDocTypeText());
		arrayNode.add(objectNode);
		node.put("attachments", arrayNode);
		message.setData(node);
		message.setUtcDate(System.currentTimeMillis());

		GroupChat videoKycChat = null;
		videoKycChat = chatHistoryService.createVideoKycGroupChatHistory(message, group);
		logger.info("attachment message added successfully in Group");
		return videoKycChat;
	}

	private String videoKycChatHistory(String videoKycStr, ChatMessage message, Group group) {
		logger.info("Create innitial chat History started. ");
		String text = "";
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(videoKycStr);
			logger.info("videoKycStr: " + jsonNode);
			if (jsonNode != null) {
				logger.info("successfully created videoKyc welcome message");
				JsonNode trackingId = jsonNode.get("trackingId");
				if (trackingId != null) {
					text = "Tracking ID: " + "\n" + trackingId.asText() + "\n" + "\n";
				}
				JsonNode videoKycDataList = jsonNode.get("videoKycDataList");
				String welcomeMessage = null;
				if (videoKycDataList.isArray()) {
					Map<String, String> displayInChatWindow = new HashMap<>();
					for (final JsonNode objNode : videoKycDataList) {
						if (objNode.get("kYCFieldName").asText().contentEquals("AadhaarAddress")) {
							text = text + "Permanent Address: " + "\n" + objNode.get("kYCFieldValue").asText() + "\n\n";
						} else if (objNode.get("kYCFieldName").asText().contentEquals("CommunicationAddress")) {
							text = text + "Communication Address: " + "\n" + objNode.get("kYCFieldValue").asText() + "\n\n";
						} else if (objNode.get("kYCFieldName").asText().contentEquals("WelcomeMessage")) {
							welcomeMessage =objNode.get("kYCFieldValue").asText() ;
						} else if (objNode.get("kYCFieldName") != null) {
							VideoKYCFields videoKycField = Enums.VideoKYCFields.getVideoKYCFields(objNode.get("kYCFieldName").asText());
							String displayName = videoKycField!=null ? videoKycField.getDisplayName() : objNode.get("kYCFieldName").asText();
							displayInChatWindow.put(displayName, objNode.get("kYCFieldValue").asText());
						}
					}
					for (Map.Entry<String, String> entry : displayInChatWindow.entrySet()) {
						text = text + entry.getKey() + ": " + "\n" + entry.getValue() + "\n\n";
					}
				}
				logger.info("Created welcome message started: ");
				creareVideoKycWelcomeMessage(welcomeMessage, message, group);
			}

		} catch (Exception e) {
			logger.error("failed to map video kyc json string ", e);
		}
		logger.info("Chat History created for videoKyc ");

		return text;
	}


	private void creareVideoKycWelcomeMessage(String welcomeMessage, ChatMessage message, Group group) {
		String msgText = "";
		//JsonNode welcomeMsg = videoKycNode.get("welcomeMessage");
		if (welcomeMessage != null) {
			logger.info("welcome Chat is present in input.");
			msgText = welcomeMessage;
		} else {
			logger.info("welcome Chat msg not in input, store default value.");
			msgText = PropertyUtil.getProperty(Constants.VIDEOKYC_CUSTOMER_WELCOME_MESSAGE);
		}
		JsonNode org = cacheService.getOrgJson(group.getCreatorOrganizationId());
		String orgName = org.findPath("orgName").asText();
		msgText = msgText.replace("$enterpriseName", orgName);
		message.setText(msgText);
		message.setChatMessageType(ChatMessageType.CustomerWelcomMessage.getId().intValue());
		logger.info("Created text for videoKycs text message: ");
		chatHistoryService.createVideoKycGroupChatHistory(message, group);
	}

	private void createCustomerDetailsChatHistory(@NonNull Group group, @NonNull String messageText, Event event) {
		logger.info("Created text for customer details for group with id: " + group.getId() + " started");
		ChatMessage message = new ChatMessage();
		message.setActive(true);
		message.setType(MessageType.Chat.getId());
		message.setSubtype(ChatType.GroupChat.getId().intValue());
		message.setTo(group.getId());
		message.setFrom(group.getGuestUserId());
		message.setUtcDate(System.currentTimeMillis());
		message.setChatMessageType(ChatMessageType.CustomerDetails.getId().intValue());
		message.setText(messageText);
		message.setUtcDate(System.currentTimeMillis());
		message.setExcludedRecipients(group.getGuestUserId().toString());
		chatHistoryService.createGroupChatHistory(message, group);

		try {
			ObjectMapper mapper = new ObjectMapper();
			String attachmentStr = event.getData().get("Attachment");
			if (attachmentStr != null) {
				logger.info("Create history for attachment started. ");
				logger.info("------------Addhar Photo--------- ");
				Attachment attachment = mapper.readValue(attachmentStr, Attachment.class);
				message.setChatMessageType(ChatMessageType.CustomerDetails.getId().intValue());
				message.setExcludedRecipients(group.getGuestUserId().toString());
				GroupChat videoKycChat = createAttachmentChatHistory(group, message, attachment);
				//	groupChatCreateGroup = videoKycChat;
			}
		} catch (Exception e) {

		}
		logger.info("created customer details chat history for group with id: " + group.getId() + "completed");
	}


	private JsonNode findProductPreference(JsonNode productPreferences, String productName, String preference) {
		try {
			logger.info("productPreferences: {}", productPreferences);
			JsonNode matchedProductPreference = null;
			if (productPreferences!= null && productPreferences.isArray()) {
				for (final JsonNode productPreference : productPreferences) {
					//JsonNode productPreference = productPreferences.get(i);
					String product = productPreference.get("product").asText();
					logger.info("product: {}", product);
					if (product.equals("*")) {
						logger.info("productPreference: {}", productPreference);
						matchedProductPreference = productPreference.get(preference);
					} else if (product.equalsIgnoreCase(productName)) {
						matchedProductPreference = productPreference.get(preference);
						break;
					}
				}
				logger.info("matchedProductPreference: {}", matchedProductPreference);
				if (matchedProductPreference != null) {
					return matchedProductPreference;
				}

			}
		} catch(Exception e){
			logger.error("exception is: {}", e);
			//throw new InternalServerErrorException(ErrorCode.InvalidProductPreference, "Invalid product preference  - " + orgProductPreference, e);
		}
		return null;
	}

}
