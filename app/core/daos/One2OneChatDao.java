package core.daos;

import java.util.List;
import java.util.Map;

import core.entities.ChatSummary;
import core.entities.One2OneChat;

public interface One2OneChatDao extends JpaDao<One2OneChat> {
	public List<One2OneChat> getOne2OneChatHistory(Integer from, Integer to, Boolean isGuest, Long lastMsgDate,
			Integer offset, Integer limit);

	public One2OneChat createChatHistory(One2OneChat one2OneChat, Long sysMsgDuration);

	public void UpdateOne2OneChatReadStatus(Integer to, Long mid);

	public Map<Integer, ChatSummary> getUnReadMsgCountAndLastMsgUsers(Integer to, List<String> contactList);

	public Long getLastMsgIdBySender(Integer senderId, Integer recipientId);

	public One2OneChat getMsgById(Long Id, Integer senderId);
}
