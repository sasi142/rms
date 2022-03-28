package core.daos;

import java.util.List;

import core.entities.BroadcastMessage;
import core.entities.BroadcastRecipient;

public interface BroadcastMessageDao {
	
	public BroadcastMessage getBroadcastMessageById(Integer msgId, Integer userId);
	public List<BroadcastMessage> getAllBroadcastMessages(Integer id);
	public void createBroadcastMessage(BroadcastMessage message,BroadcastRecipient recipient);
	public Integer getUnreadMessageCount(Integer id);
}
