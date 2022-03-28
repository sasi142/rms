package core.validator;

import java.util.List;
import java.util.Map;

import messages.UserConnection;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import akka.http.javadsl.model.headers.Connection;
import controllers.CacheController;
import core.daos.CacheGroupDao;
import core.entities.ActorExecutionContext;
import core.entities.ChatMessage;
import core.entities.Event;
import core.entities.Group;
import core.entities.GroupMember;
import core.entities.GroupPreference;
import core.entities.IqMessage;
import core.entities.User;
import core.exceptions.ApplicationException;
import core.exceptions.BadRequestException;
import core.exceptions.ForbiddenException;
import core.services.CacheService;
import core.utils.ActorThreadContext;
import core.utils.Constants;
import core.utils.Enums.ChatMessageType;
import core.utils.Enums.VideoCallMessageType;
import core.utils.ThreadContext;
import core.utils.Enums.ChatType;
import core.utils.Enums.DeleteOption;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.EventType;
import core.utils.Enums.GroupMemberRole;
import core.utils.Enums.IqActionType;
import core.utils.Enums.MessageType;
import core.utils.Enums.UserCategory;

@Component
public class InputDataValidator implements InitializingBean, Validator {

	final static Logger logger = LoggerFactory.getLogger(CacheController.class);

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private CacheGroupDao cacheGroupDao;

	@Autowired
	public Environment env;

	private Integer maxTextLength = 1024;

	@Override
	public void afterPropertiesSet() throws Exception {
		maxTextLength = Integer.valueOf(env.getProperty(Constants.MAX_CHAT_TEXT_LENGTH));
		logger.info("max chat text length : " + maxTextLength);
	}

	public void validate(UserConnection connection, ChatMessage message) {
		logger.debug("validate message data for connection " + connection);	
		if (message.getType() == MessageType.Chat.getId()) {
			Integer toUserId = message.getTo();
			Integer fromUserId = connection.getUserContext().getUser().getId();
			if (toUserId == null) {
				throw new BadRequestException(ErrorCode.InvalidTo, ErrorCode.InvalidTo.getName());
			}

			if (message.getSubtype().byteValue() == ChatType.One2One.getId().byteValue()) {

				// Validate Recipient and sender are not same
				if (toUserId.intValue() == fromUserId.intValue()) {
					throw new ForbiddenException(ErrorCode.toShouldNotBeSelf, ErrorCode.toShouldNotBeSelf.getName());
				}
				// Validate that both users are not customers
				//User fromUser = cacheService.getUser(fromUserId, false);
				User fromUser = connection.getUserContext().getUser();
				User toUser = cacheService.getUser(toUserId, false);
				ActorExecutionContext context = new ActorExecutionContext(toUser);
				ActorThreadContext.set(context);

				if ((UserCategory.Customer.getId().byteValue() == fromUser.getUserCategory().byteValue()
						&& UserCategory.Customer.getId().byteValue() == toUser.getUserCategory().byteValue())
						|| (UserCategory.Guest.getId().byteValue() == fromUser.getUserCategory().byteValue()
						&& UserCategory.Guest.getId().byteValue() == toUser.getUserCategory().byteValue())) {
					throw new ForbiddenException(ErrorCode.Invalid_Chat_SenderReceipient, fromUserId,
							fromUser.getUserCategory(), toUserId, toUser.getUserCategory());
				}
				// Validate if users are in contacts
				Boolean isInContact = cacheService.isInContact(fromUser.getOrganizationId(), fromUserId, toUserId);
				if (!isInContact) {
					throw new ForbiddenException(ErrorCode.NotInContact, toUserId, fromUserId);
				}
			} else if (message.getSubtype().byteValue() == ChatType.GroupChat.getId().byteValue()) {
				// validate group message sending
				// User fromUser = cacheService.getUser(fromUserId, false);
				User fromUser = connection.getUserContext().getUser();
				// Customer can not talk to group
				if (UserCategory.Customer.getId().byteValue() == fromUser.getUserCategory().byteValue()) { 
					throw new ForbiddenException(ErrorCode.Invalid_Chat_SenderReceipient, fromUserId,
							fromUser.getUserCategory(), toUserId, "Group");
				}
				validateSendingMsgToGroup(message, fromUserId);
			}

			// validate text message and attachments data.
			String text = message.getText();
			JsonNode data = message.getData();

			if (message.getChatMessageType() == null) {
				message.setChatMessageType(ChatMessageType.ChatMessage.getId().intValue());
			}

			// Skip the text check for video message
			if (message.getChatMessageType().intValue() != ChatMessageType.VideoCallMessage.getId()
					&& ((text == null || "".equalsIgnoreCase(text) || text.length() > 1024) && data == null)) {
				throw new BadRequestException(ErrorCode.InvalidText, ErrorCode.InvalidText.getName());
			}
			if (data != null) {
				JsonNode node = data.findPath("type");
				if (node == null || node.isMissingNode()) {
					throw new BadRequestException(ErrorCode.Invalid_Data, ErrorCode.Invalid_Data.getName());
				}
				int type = node.asInt();
				switch (type) {
				case 1:
					JsonNode node1 = data.findPath("attachments");
					if (node1 == null || node1.isMissingNode()) {
						throw new BadRequestException(ErrorCode.Invalid_Data, ErrorCode.Invalid_Data.getName());
					}
					break;
				case 0: // Here we need to check message types
					if (message.getChatMessageType().intValue() == ChatMessageType.VideoCallMessage.getId()) {
						if (message.getVideoCallMessageType().intValue() == VideoCallMessageType.VideoCallEnded.getId()
								&& (data.findPath("caller").isNull() || data.findPath("receiver").isNull()
										|| data.findPath("callDuration").isNull())) {
							throw new BadRequestException(ErrorCode.Invalid_Data, ErrorCode.Invalid_Data.getName());
						}
					}
					break;
				default:
					throw new BadRequestException(ErrorCode.Invalid_Data, ErrorCode.Invalid_Data.getName());
				}
			}
		}
		if ((message.getType() == MessageType.ACK.getId()) && (message.getSubtype() == MessageType.Chat.getId())) {
			if (message.getMid() == null) {
				throw new BadRequestException(ErrorCode.Invalid_Mid, ErrorCode.Invalid_Mid.getName());
			}
			// No UUID available for messages coming from curtain as no socket connection
			// available.
			/*
			 * if (message.getUuid() == null) { throw new
			 * BadRequestException(ErrorCode.Invalid_Uuid,
			 * ErrorCode.Invalid_Uuid.getName()); }
			 */
		}
		logger.debug("validate message data for connection " + connection);
	}

	private void validateSendingMsgToGroup(ChatMessage message, Integer senderUserId) {
		Integer groupId = message.getTo();
		logger.debug("validating SendingMsgToGroup for groupId " + groupId);

		Group group = cacheGroupDao.getGroup(groupId);
		if (group == null) {
			throw new BadRequestException(ErrorCode.InvalidGroup, groupId);
		}

		ActorExecutionContext context = new ActorExecutionContext(group);
		ActorThreadContext.set(context);

		boolean isMember = false;
		List<GroupMember> members = group.getMembers();
		if (members != null && !members.isEmpty()) {
			for (GroupMember groupMember : members) {
				if (senderUserId.equals(groupMember.getId())) {
					isMember = true;
					if (groupMember.getMemberStatus().byteValue() != 1) {
						throw new ForbiddenException(ErrorCode.UserLeftGroup, senderUserId, groupId);
					}
				}
			}
		}
		if (!isMember) {
			throw new ForbiddenException(ErrorCode.UserNotInGroup, senderUserId, groupId);
		}
		if (group.getGroupStatus() == 2) {
			throw new ForbiddenException(ErrorCode.GroupClosed, senderUserId, groupId);
		}
		if (message.getType() == MessageType.Chat.getId().intValue()) {
		  Boolean isOneWayGroup = isOneWayGroup(group);
		  if(isOneWayGroup) {
			  GroupMember member=  members.stream().filter(p -> senderUserId.equals(p.getId())).findFirst().get();
			  if(GroupMemberRole.Member.getId().byteValue() == member.getMemberRole().byteValue()) {
				  throw new ForbiddenException(ErrorCode.Send_Message_Not_Allowed, senderUserId, groupId);
			  }
		  }
		}		
	
		logger.debug("validated SendingMsgToGroup for groupId " + groupId);
	}

	private Boolean isOneWayGroup(Group group) {
		//List<GroupPreference> prefrences = group.getPreferences();
		if(group.getPreferences() != null) {
		GroupPreference pref = group.getPreferences().stream().filter(p -> p.getPreference().contentEquals("EnableOneWayGroupChat")).findAny().orElse(null);
		if(pref != null) {
			return Boolean.valueOf(pref.getValue());
		}
		}
		return false;
	}

	@SuppressWarnings("unused")
	@Override
	public void validate(Event event) {
		logger.debug("validate event data for event " + event);
		if (event.getData() == null || event.getType() == null) {
			throw new BadRequestException(ErrorCode.Event_Invalid_Data, "data,type");
		}

		try {
			if (EventType.ContactAdd.getId() == event.getType() || EventType.ContactRemove.getId() == event.getType()) {
				Integer Id1 = Integer.valueOf(event.getData().get("Id1"));
				Integer Id2 = Integer.valueOf(event.getData().get("Id2"));
				if (Id1 == null || Id2 == null) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "Id1,Id2");
				}
			} else if (EventType.LockUser.getId() == event.getType() || EventType.DeleteUser.getId() == event.getType()
					|| EventType.ResetPassword.getId() == event.getType()
					|| EventType.UserUpdate.getId() == event.getType()
					|| EventType.ContactAddOrg.getId() == event.getType()
					|| EventType.CloseConnection.getId() == event.getType()) {
				Integer Id = Integer.valueOf(event.getData().get("Id"));
				if (Id == null) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "Id");
				}
			} else if (EventType.AddGroup.getId() == event.getType() || EventType.UpdateGroup.getId() == event.getType()
					|| EventType.CloseGroup.getId() == event.getType()
					|| EventType.LeaveGroup.getId() == event.getType()
					|| EventType.ExitGroup.getId() == event.getType()
					|| EventType.UpdateUseCaseStatus.getId() == event.getType()
					|| EventType.CreateVideoKyc.getId() == event.getType()) {
				try {
					Integer groupId = Integer.valueOf(event.getData().get("GroupId").toString());
				} catch (Exception e) {
					throw new BadRequestException(ErrorCode.InvalidGroup, event.getData().get("GroupId"));
				}
			} else if (EventType.Logout.getId() == event.getType()) {
				Integer userId = Integer.valueOf(event.getData().get("UserId").toString());
				String token = event.getData().get("AuthToken").toString();
				if (userId == null || token == null) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "userId,token");
				}
			}else if (EventType.SessionExpired.getId() == event.getType()) {
				Integer userId = Integer.valueOf(event.getData().get("UserId").toString());				
				if (userId == null) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "userId");
				}
			}else if (EventType.ChatGuestAdded.getId().equals(event.getType())) {
				try {
					Integer groupId = Integer.valueOf(event.getData().get("GroupId").toString());
				} catch (Exception e) {
					throw new BadRequestException(ErrorCode.InvalidGroup, event.getData().get("GroupId"));
				}
			} else if (EventType.ProcessedVideoRecording.getId().equals(event.getType())) {
				Integer to = Integer.valueOf(event.getData().get("to"));
				Integer from = Integer.valueOf(event.getData().get("from"));
				Integer recordingId = Integer.valueOf(event.getData().get("recordingId"));
				String statusMapString = event.getData().get("statusMap");
				if (to == null || from == null || recordingId == null || StringUtils.isEmpty(statusMapString)) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "to,from,recordingId");
				}
			} else if (EventType.FailedVideoRecording.getId().equals(event.getType())) {
				String recordingIds = event.getData().get("recordingList");
				if (StringUtils.isEmpty(recordingIds)) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "recordingIds");
				}
			} else if (EventType.DeviceUpdated.getId().equals(event.getType())) {
				Integer entityId = Integer.valueOf(event.getData().get("EntityId"));
				Integer userCategory = Integer.valueOf(event.getData().get("UserCategory"));
				if (entityId == null || userCategory == null) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "entityId,userCategory");
				}
			}

			else if(EventType.GuestChatDeviceInfoUpdate.getId().equals(event.getType()))
			{ 
				logger.info("eventDetails:"+event);
			} else if (EventType.UpdateVideoKycStatus.getId() == event.getType()) {
				Integer kycId = Integer.valueOf(event.getData().get("kycId").toString());				
				if (kycId == null) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "kycId");
				}
			} else if (EventType.UpdateUserRole.getId() == event.getType()) {
				Integer userId = Integer.valueOf(event.getData().get("userId").toString());				
				if (userId == null) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "userId");
				}
			} else if (EventType.ChatCustomerForwardGuestAdded.getId().equals(event.getType())) {
				Integer groupId = Integer.valueOf(event.getData().get("GroupId"));
				if (groupId == null) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "groupId");
				}
			} else if (EventType.UpdateGroupBySystem.getId().equals(event.getType())) {
				String groups = event.getData().get("Groups");
				if (groups == null) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "groupId");
				}
			} else if (EventType.UploadAttachment.getId().equals(event.getType())) {
				String groups = event.getData().get("GroupId");
				if (groups == null) {
					throw new BadRequestException(ErrorCode.Event_Invalid_Data, "groupId");
				}
			}
			else {
				throw new BadRequestException(ErrorCode.EventType_Not_Supported, "event type not supported");
			}
			logger.debug("validate event data for event " + event);
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new BadRequestException(ErrorCode.Event_Invalid_Data, ErrorCode.Event_Invalid_Data.getName());
		}
	}

	@Override
	public void validate(UserConnection connection, IqMessage iqMessage) {
		logger.debug("validate IqMessage for input msg " + iqMessage);
		if (iqMessage.getAction().equalsIgnoreCase(IqActionType.UpdateChatStatusRead.getName())) {
			Map<String, String> maps = iqMessage.getParams();
			if (maps == null || maps.get("to") == null) {
				throw new BadRequestException(ErrorCode.Invalid_iqMessage_Data, "to");
			}
		}
		User currentUser = connection.getUserContext().getUser();
		if (iqMessage.getAction().equalsIgnoreCase(IqActionType.GetUser.getName())
				|| iqMessage.getAction().equalsIgnoreCase(IqActionType.GetUserInfo.getName())) {
			Integer contactId = Integer.valueOf(iqMessage.getParams().get("id").toString());
			User contact = cacheService.getUser(contactId, false);
			if (((UserCategory.Customer.getId().byteValue() == currentUser.getUserCategory().byteValue())
					&& (UserCategory.Customer.getId().byteValue() == contact.getUserCategory().byteValue()))
					|| ((UserCategory.Guest.getId().byteValue() == currentUser.getUserCategory().byteValue())
							&& (UserCategory.Guest.getId().byteValue() == contact.getUserCategory().byteValue()))) {
				throw new ForbiddenException(ErrorCode.NotInContact, contactId, currentUser.getId());
			}
		}
		if (iqMessage.getAction().equalsIgnoreCase(IqActionType.UpdateUserMapWindowStatus.getName())) {
			if ((UserCategory.Customer.getId().byteValue() == currentUser.getUserCategory().byteValue())
					|| (UserCategory.Guest.getId().byteValue() == currentUser.getUserCategory().byteValue())) {
				throw new ForbiddenException(ErrorCode.Action_Not_Supported, "Action: " + iqMessage.getAction()
				+ " not supported for Customer/Guest with Id: " + currentUser.getId());
			}
		}

		if (iqMessage.getAction().equalsIgnoreCase(IqActionType.StartVideoRecording.getName())) {
			Map<String, String> maps = iqMessage.getParams();
			if (maps == null || maps.get("to") == null) {
				throw new BadRequestException(ErrorCode.Invalid_iqMessage_Data, "to");
			}
			if (maps == null || maps.get("recordingType") == null) {
				throw new BadRequestException(ErrorCode.Invalid_iqMessage_Data, "recordingType");
			}
		}

		if (iqMessage.getAction().equalsIgnoreCase(IqActionType.DeleteChatMessage.getName())) {
			Integer chatType = Integer.valueOf(iqMessage.getParams().get("chatType"));
			if (!(chatType.intValue() == ChatType.One2One.getId().intValue()
					|| chatType.intValue() == ChatType.GroupChat.getId().intValue())) {
				throw new BadRequestException(ErrorCode.Invalid_iqMessage_Data,
						ErrorCode.Invalid_iqMessage_Data.getName() + " for field : chatType");
			}
			Integer deleteOption = Integer.valueOf(iqMessage.getParams().get("deleteOption"));
			if (!(deleteOption.intValue() == DeleteOption.DeleteMessageForAll.getId().intValue()
					|| deleteOption.intValue() == DeleteOption.DeleteMessageForMe.getId().intValue())) {
				throw new BadRequestException(ErrorCode.Invalid_iqMessage_Data,
						ErrorCode.Invalid_iqMessage_Data.getName() + " for field : deleteOption");
			}
			Integer contactId = Integer.valueOf(iqMessage.getParams().get("contactId"));
			Boolean isInContact = false;
			if (ChatType.One2One.getId().intValue() == chatType.intValue()) {
				isInContact = cacheService.isInContact(currentUser.getId(), contactId);
				logger.info("Check currentUserId " + currentUser.getId() + " and contactId : " + contactId
						+ ". isInContact returned " + isInContact);
			} else if (ChatType.GroupChat.getId().intValue() == chatType.intValue()) {
				isInContact = cacheGroupDao.isInGroup(contactId, currentUser.getId());
				logger.info("Check currentUserId " + currentUser.getId() + " and groupId : " + contactId
						+ ". isInGroup returned " + isInContact);
			}
			if (!isInContact) {
				throw new BadRequestException(ErrorCode.Invalid_iqMessage_Data,
						ErrorCode.Invalid_iqMessage_Data.getName() + " for field : contactId");
			}
			JsonNode orgJson = cacheService.getOrgJson(currentUser.getOrganizationId());
			JsonNode prefs = orgJson.findPath("settings");
			boolean allowDeleteMsg = true;
			if (prefs != null && prefs.isArray()) {
				for (JsonNode pref : prefs) {
					if ("AllowDeleteMessage".equalsIgnoreCase(pref.findPath("preference").asText())) {
						allowDeleteMsg = Boolean.valueOf(pref.findPath("value").asText());
						break;
					}
				}
			}
			if (!allowDeleteMsg) {
				throw new ForbiddenException(ErrorCode.Delete_Message_Not_Allowed, currentUser.getOrganizationId());
			}
		}
		logger.debug("validated IqMessage for input msg " + iqMessage);
	}
}
