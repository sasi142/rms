package core.daos;

import java.util.List;
import java.util.Map;

import core.entities.ChatContact;
import core.entities.Contact;
import core.entities.UserContext;

public interface ContactDao {
	public List<Contact> getContacts(UserContext userContext, Integer offset, Integer limit, String searchKey);

	public List<Contact> getChatContacts(UserContext userContext, Integer offset, Integer limit);

	public String getChatContactsFromIms(UserContext userContext, Integer offset, Integer limit, String searchKey, Integer contactId, Integer inputChatType);
	
	public List<ChatContact> getChatContacts(UserContext userContext, Integer offset, Integer limit, String syncDates, Boolean isFirstTimeSync);
	
	public Integer getContactCountToSync(UserContext userContext, String syncDates);
	
	public ChatContact getChatContactDetail(UserContext userContext,
			Integer contactId, Integer inputChatType);

	List<ChatContact> getNonOrgContacts(Integer organizationId, Integer userId, Long lastSyncTime);

	List<ChatContact> getOrgGroupContacts(Integer organizationId, Integer userId, Long lastSyncTime);

	public List<ChatContact> getContactsV2(UserContext userContext, Integer offset, Integer limit, String syncDates);
	public Map<String,Object> getSyncChatContacts(UserContext userContext, Integer offset, Integer limit, String syncDates, Boolean isProfileInfo);

	List<ChatContact> getOrgContacts(Integer organizationId, Integer userId, Long lastSyncTime);
}
