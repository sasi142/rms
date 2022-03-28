package core.services;

import java.util.List;
import java.util.Optional;

import core.entities.ChatContact;
import core.entities.Contact;
import core.entities.GroupMember;
import core.entities.UserContext;
import core.entities.projections.UnreadCountSummary;

public interface UserService {
	List<Contact> getContacts(UserContext userContext, Integer offset, Integer limit, String searchKey);

	List<Contact> getContactsV2(UserContext userContext, Integer offset, Integer limit, String searchKey, Integer contactId, Integer inputChatType);

	public Contact getUserInfo(Integer loggedInUserId, Integer contactId);

	public Boolean isAdminUser(Integer userId);
	
	public List<Contact> getPresenceLocationUnreadCount(Integer loggedInUserId, List<Contact> contacts);
	
	public ChatContact getChatContactDetail(UserContext userContext, Integer contactId, Integer inputChatType);

	Boolean isBroadcaster(Integer userId);

	Optional<UnreadCountSummary> unreadCountSummary(Integer orgId, String userName);

	GroupMember getGroupMemberFromGroup(UserContext userContext, Integer contactId);
}
