package core.daos;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

public interface CacheContactsDao {
	public List<Integer> findAll(Integer userId, Integer orgId);

	public Boolean isInContact(Integer userId, Integer contactId);
	
	public Boolean isFollowing(Integer channelId, Integer contactId);
	
	public Boolean isInOrgContact(Integer organizationId, Integer contactId);

	public List<Integer> findAllOrgContacts(Integer organizationId);
	
	public List<Integer> findAllChannelContacts(Integer channelId);

	public JsonNode getUserObjectWithOpenChatWindow(Integer userId);
	
	public List<Integer> getUsersWithOpenChatWindow(Integer userId);	
	
	public Set<Integer> getUsersWithOpenMapWindow(Integer userId, Integer orgId);
}
