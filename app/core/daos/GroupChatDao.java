package core.daos;

import java.util.List;

import core.entities.Attachment;
import core.entities.GroupChat;
import core.entities.MessageReadInfo;

public interface GroupChatDao extends JpaDao<GroupChat> {
	public GroupChat createGroupChatHistory(final GroupChat groupChat);

	public List<GroupChat> getChatHistory(Integer groupId, Integer currentUserId, Boolean isMemberActive,
			Long lastMsgDate, Integer offset, Integer limit);

	public void createGroupChatMsgRecipient(Integer groupId, Integer ExcludeConsumerId);

	public List<MessageReadInfo> getGroupChatMsgReadInfo(Long msgId, Integer currentUserId);

	public Long getLastMsgIdByGroup(Integer groupId, Integer recipientId);

	public List<Attachment> assignAttachmentRecipient(Long groupId, String attachmentId, String entityType ,Integer createdById);

	public void updateWelcomeMessage(GroupChat groupChat);

	List<GroupChat> getChatHistoryByShortUri(Integer groupId, Integer offset, Integer limit);

	GroupChat createVideoKycGroupChatHistory(GroupChat groupChat);
}
