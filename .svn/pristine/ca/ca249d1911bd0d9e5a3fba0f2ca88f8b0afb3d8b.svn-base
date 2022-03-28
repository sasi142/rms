package core.services;

import core.entities.ChatMessage;
import core.entities.Event;
import core.entities.Group;
import core.entities.GroupChat;
import core.utils.Enums.EventType;
import messages.UserConnection;

public interface GroupChatMessageService {
	public void sendMessage(UserConnection connection, ChatMessage message);	
	public void sendAddGroupEventMessage(Event event, Group group, GroupChat groupChat);
	public GroupChat updateDocType(Integer messageId, Integer attachmentId, Integer docType, String docTypeText, Integer documentPurpose, String documentPurposeText,
			Integer userId);	
	public void sendChatGuestBrowserInfoEvent(EventType event, Group group, GroupChat groupChat);
	public void sendUpdateUseCaseStatusEvent(EventType event, Group group, GroupChat groupChat);
	
	
}
