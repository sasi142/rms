package core.daos;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import core.entities.ChatMessage;
import core.entities.ChatSummary;
import core.entities.UserContext;
import core.entities.projections.UnreadCountSummary;
import core.utils.Enums.ChatType;

public interface ChatSummaryDao extends JpaDao<ChatSummary> {
	public Map<String, ChatSummary> getContactChatSummary(Integer currentUserId, Map<Byte, List<Integer>> contactList);

	public void updateChatReadStatus(Integer toUserGroupId, Integer currentUserId, ChatType chatType);
	
	public List<ChatMessage> updateChatReadStatusV2(Integer currentUserId, String one2oneMsgIds, String groupMsgIds);

	public List<ChatSummary> getUnReadMsgsPerContact(Integer to);

	public void deactivateOne2OneChatSummary(Integer deletedUserId);

	public List<ChatSummary> getUnReadMsgContact(List<Integer> userIds, Integer offset, Integer limit);

	public Long getUnReadSenderCount(Integer to);

	public List<ChatMessage> getChatHistory(UserContext userContext, Integer offset, Integer limit, String timeRanges, Boolean isFirstTimeSync,
			Integer perUserMsgCout);
	public List<ChatSummary> getUnReadMsgsForContacts(Integer to, List<Integer> userIds, List<Integer> groupIds);

	List<ChatSummary> getRecipientUnreadCount(Integer recipientId);

	public ChatSummary getChatSummary(Integer contactId, Integer recipientId, ChatType chatType);
	
	public boolean deleteChatMessages(Integer currentUserId, String msgIds, Integer contactId, Integer deleteOption, Integer chatType, Long deletedDate);

	Optional<UnreadCountSummary> unreadCountSummary(Integer orgId, String userName);
}
