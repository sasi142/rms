package core.services;

import java.util.List;

import messages.UserConnection;
import core.entities.ChatMessage;
import core.entities.ChatSummary;
import core.entities.Contact;
import core.entities.Group;
import core.entities.GroupChat;
import core.entities.IqMessage;
import core.entities.MessageReadInfo;
import core.entities.One2OneChat;
import core.entities.UserContext;
import core.utils.Enums.ChatType;
import core.utils.Enums.EventType;
import core.utils.Enums.MessageType;

public interface ChatHistoryService {
	public One2OneChat createOne2OneChatHistory(ChatMessage message);

	public One2OneChat createOne2OneChatHistory(EventType eventType, ChatMessage message);

	public GroupChat createGroupChatHistory(ChatMessage message, Group inputGroup);

	public List<One2OneChat> getOne2OneChatHistory(UserContext userContext, Integer to, Long lastMsgDate,
			Integer offset, Integer limit);

	public List<ChatSummary> getUnReadMsgsPerContact(Integer to);

	public void updateChatReadStatus(Integer toUserGroupId, ChatType chatType, MessageType messageType,
			UserContext context);

	public List<ChatMessage> updateChatReadStatusV2(UserContext context, String one2oneMsgIds, String groupMsgIds);

	public void UpdateOne2OneChatReadStatus(Integer to, Long mid);

	public List<GroupChat> getGroupChatHistory(UserContext userContext, Integer groupId, Long lastMsgDate,
			Integer offset, Integer limit);

	public GroupChat createGroupChatHistory(EventType addgroup, Group group);

	public void deactivateChatSummary(Integer deletedUserId);

	public List<ChatSummary> getUnReadMsgContact(List<Integer> userIds, Integer offset, Integer limit);

	public List<ChatMessage> getChatHistory(UserConnection userConnection, IqMessage iqMessage);

	public void createGroupChatMsgRecipient(Integer groupId, Integer ExcludeConsumerId);

	public List<Contact> getUnReadMsgsForContacts(Integer loggedInUserId, List<Contact> contacts);

	List<Contact> getRecipientUnreadCount(Integer recipientId);

	public List<MessageReadInfo> getChatMsgReadInfo(Long msgId, Integer currentUserId, Byte chatType);

	public void deleteChatHistory(Integer deletedUserId);

	public void updateWelcomeMessage(GroupChat groupChat);

	public void sendReadACKMsgs(UserContext context, List<ChatMessage> msgs);

	public void deleteChatMessages(UserContext context, List<Integer> msgIds, Integer contactId, Integer deleteOption,
			Integer chatType);

	GroupChat createVideoKycGroupChatHistory(ChatMessage message, Group inputGroup);
	
	

}