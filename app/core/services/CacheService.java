package core.services;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import core.entities.ClientCertificate;
import core.entities.Group;
import core.entities.User;

public interface CacheService {
	public Map<String, ArrayNode> getAll();

	public void remove(Integer userId, String uuid);

	public JsonNode getUserJson(Integer id);
	
	public JsonNode getOrgJson(Integer orgId);

	public User getUser(Integer userId, boolean all);

	ArrayNode getAppSettingsJson(Integer orgId);

	public String[] getUserRole(Integer userId);

	public List<Integer> getAllContacts(Integer userId);

	public Boolean isInContact(Integer userId, Integer contactId);

	public Boolean isInContact(Integer userId, List<Integer> contactId);

	public List<Integer> getAllOrgUserIds(Integer organizationId);
	
	public List<Integer> getAllChannelContacts(Integer channelId);

	public List<Integer> getUsersWithOpenChatWindow(Integer userId);

	public Set<Integer> getUsersWithOpenMapWindow(Integer userId);
	
	public ClientCertificate getClientCertificate(String bundleKey);
	
	public Integer refreshClientCertificateCache();
	
	public List<Integer> getBrandChatOrgs(String clientId);

	public boolean isFollowing(Integer channelId, List<Integer> recipientIds);

	Boolean isFollowing(Integer channelId, Integer recipientId);
	
	public JsonNode getChannelJson(Integer channelId);

	public Group getGroupDetails(Integer groupId);
	
	public Boolean isInOrgContact(Integer userOrgId, Integer contactId);

	public JsonNode getUserObjectWithOpenChatWindow(Integer userId);

	public Boolean isInContact(Integer userOrgId, Integer userId, Integer contactId);
	
	public JsonNode getVideoKycInfo(Integer userId);	
	
	public String getUserCustomStatus(Integer userId);

	public List<JsonNode> getUserJsons(List<Integer> userIds);


	void markKycAgentAssigned(Integer kycId, Integer agentId, Long groupId);


	void storeUserUnreadCount(Integer userId, String data);
	String getUserUnreadCount(Integer userId);

}
