package core.services;

import java.util.*;

import core.entities.projections.UnreadCountSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import core.daos.CacheConnectionInfoDao;
import core.daos.CacheGroupDao;
import core.daos.CacheOrgDao;
import core.daos.CacheUserDao;
import core.daos.ChatSummaryDao;
import core.daos.ContactDao;
import core.daos.DeviceDao;
import core.entities.ChatContact;
import core.entities.ChatSummary;
import core.entities.Contact;
import core.entities.Device;
import core.entities.DeviceLocation;
import core.entities.Group;
import core.entities.GroupContact;
import core.entities.GroupMember;
import core.entities.Presence;
import core.entities.UserContext;
import core.entities.UserPhoto;
import core.exceptions.ApplicationException;
import core.exceptions.InternalServerErrorException;
import core.exceptions.ResourceNotFoundException;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.ThreadContext;
import core.utils.Enums.ChatContactType;
import core.utils.Enums.ChatType;
import core.utils.Enums.DeviceType;
import core.utils.Enums.ErrorCode;

import core.utils.Enums.NotificationType;
import core.utils.Enums.RoleName;
import core.utils.Enums.UserCategory;
import play.libs.Json;

@Service
public class UsersServiceImpl implements UserService {
	final static Logger logger = LoggerFactory.getLogger(UsersServiceImpl.class);

	@Autowired
	@Qualifier("CacheImpl")
	private CacheConnectionInfoDao cacheConnectionInfoDao;

	@Autowired
	private CacheUserDao cacheUserDao;

	@Autowired
	private CacheGroupDao cacheGroupDao;

	@Autowired
	private ContactDao contactDao;

	@Autowired
	private ChatSummaryDao chatSummaryDao;

	@Autowired
	private PresenceService presenceService;


	@Autowired
	private CommonUtil commonUtil;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;
	@Autowired
	private CacheOrgDao cacheOrgDao;
	@Autowired
	private DeviceDao deviceDao;

	@SuppressWarnings("rawtypes")
	@Override
	public List<Contact> getContacts(UserContext userContext, Integer offset, Integer limit, String searchKey) {
		logger.info("get contacts for user " + userContext.getUser().getId() + " and search key = " + searchKey);
		List<Contact> contacts = null;
		if (searchKey != null) {
			contacts = contactDao.getContacts(userContext, 0, 99999, searchKey);
		} else {
			contacts = contactDao.getChatContacts(userContext, offset, limit);
		}
		logger.info("got contacts for user " + userContext.getUser().getId() + " with size "
				+ (contacts == null ? contacts : contacts.size()));

		if (contacts != null && !contacts.isEmpty()) {
			List<String> contactIds = new ArrayList<>();
			List<Integer> contactList = new ArrayList<>();
			for (Iterator iterator = contacts.iterator(); iterator.hasNext(); ) {
				Contact contact = (Contact) iterator.next();
				if (ChatType.One2One.getId().byteValue() == contact.getChatType().byteValue()) {
					contactIds.add(String.valueOf(contact.getId()));
					contactList.add(contact.getId());
				} else {
					iterator.remove();
				}
			}
			logger.debug("got contacts for user " + userContext.getUser().getId() + " with contactIds "
					+ contactIds.toString());
			Map<Byte, List<Integer>> contactListMap = new HashMap<Byte, List<Integer>>();
			if (!contactList.isEmpty()) {
				contactListMap.put(ChatType.One2One.getId(), contactList);
			}
			Map<String, ChatSummary> chatSummeries = chatSummaryDao.getContactChatSummary(userContext.getUser().getId(),
					contactListMap);
			logger.info("got chatSummeries for One2One contacts of size  "
					+ (chatSummeries == null ? null : chatSummeries.size()));

			for (Contact contact : contacts) {
				logger.debug("Processing contact " + contact.getId());
				if (ChatType.One2One.getId().byteValue() == contact.getChatType().byteValue()) {
					// set presence only for once2one
					Presence presence = presenceService.getPresence(contact.getId(), false);
					contact.setPresence(presence);
					// to fix defect as mobile-number & landline number needed in mobile
					setContactNumbers(contact);
				}

				// Set one2one and groupChat Summary
				ChatSummary chatSummary = chatSummeries.get(contact.getId() + "_" + contact.getChatType().byteValue());
				if (chatSummary != null) {
					contact.setChatSummary(chatSummary);
					chatSummary.setStatus(false);
					String lastMsgDate = commonUtil.getDateTimeWithTimeZone(chatSummary.getTime(),
							userContext.getUser().getTimezone());
					chatSummary.setLastMsgDate(lastMsgDate);
					chatSummary.setUtcDate(chatSummary.getTime());
					if (chatSummary.getLastMsgSenderId() != null) {
						Integer lastMsgSenderId = chatSummary.getLastMsgSenderId().intValue();
						logger.debug("get last Message for contact " + contact.getId() + " with lastMsgSenderId = "
								+ lastMsgSenderId);
						JsonNode userJson = cacheUserDao.find(lastMsgSenderId);
						chatSummary.setLastMsgSenderName(userJson.findPath("firstName").asText());
						logger.debug("last Message Sender Name set as  " + userJson.findPath("firstName").asText());
					}
					// set unread count
					if (chatSummary.getUnReadMsgCount() > 0) {
						contact.setUnreadMsg(true);
					}
				}
				logger.debug("Processed contact " + contact.getId());
			}
		}
		return contacts;
	}

	@Override
	public ChatContact getChatContactDetail(UserContext userContext, Integer contactId, Integer inputChatType) {
		ChatContact contact = null;
		logger.debug("Get chat contact " + contactId + " using UserContext = " + userContext);
		contact = contactDao.getChatContactDetail(userContext, contactId, inputChatType);
		logger.debug("Got chat contact for " + contactId + " as " + contact);
	
		createChatContact(userContext, contact);

		// Fix for iOS
		Integer contactType = contact.getContactType();
		contactType = commonUtil.overRideContactType(userContext, contactType);
		contact.setContactType(contactType);

		logger.info("created Chat Contact " + contactId + " as " + contact);
		return contact;
	}


	@Override
	public GroupMember getGroupMemberFromGroup(UserContext userContext, Integer contactId) {
		GroupMember groupMember = null;
		//	UserContext userContext = ThreadContext.getUserContext();
		logger.debug("Get chat contact " + contactId + " using UserContext = " + userContext);
		ChatContact contact = contactDao.getChatContactDetail(userContext, contactId, 1);
		logger.debug("Got chat contact for " + contactId + " as " + contact);
		if (contact.getGroupMembersStr() != null && !"".equals(contact.getGroupMembersStr().trim())) {
			JsonNode memberNodes = Json.parse(contact.getGroupMembersStr());
			for (int count = 0; count < memberNodes.size(); count++) {
				if(userContext.getUser().getId().intValue() == memberNodes.get(count).get("Id").asInt()) {
					groupMember= new GroupMember();
					groupMember.setId(userContext.getUser().getId());
					groupMember.setMemberRole(Byte.parseByte(memberNodes.get(count).get("memberRole").asText()));
					groupMember.setMemberStatus(Byte.parseByte(memberNodes.get(count).get("memberStatus").asText()));
					return groupMember;
				}
			}
		}	

		return groupMember;
	}

	@Override
	public List<Contact> getPresenceLocationUnreadCount(Integer loggedInUserId, List<Contact> contacts) {
		List<Integer> userIds = new ArrayList<Integer>();
		List<Integer> groupIds = new ArrayList<Integer>();
		List<Contact> outContacts = new ArrayList<Contact>();
		for (Contact contact : contacts) {
			if (ChatType.One2One == ChatType.getChatTypeById(contact.getChatType().intValue())) {
				userIds.add(contact.getId());
			} else if (ChatType.GroupChat == ChatType.getChatTypeById(contact.getChatType().intValue())) {
				groupIds.add(contact.getId());
			}
		}
		logger.debug("Get chatSummaries for One2One " + (userIds == null ? null : userIds) + " and for Groups "
				+ (groupIds == null ? null : groupIds));
		List<ChatSummary> chatSummaries = chatSummaryDao.getUnReadMsgsForContacts(loggedInUserId,
				((userIds.isEmpty()) ? null : userIds), ((groupIds.isEmpty()) ? null : groupIds));
		Map<String, ChatSummary> chatSummaryMap = new HashMap<>();
		for (ChatSummary chat : chatSummaries) {
			if (ChatType.One2One == ChatType.getChatTypeById(chat.getChatType().intValue())) {
				chatSummaryMap.put(ChatType.One2One.name() + chat.getContactId(), chat);
			} else if (ChatType.GroupChat == ChatType.getChatTypeById(chat.getChatType().intValue())) {
				chatSummaryMap.put(ChatType.GroupChat.name() + chat.getContactId(), chat);
			}
		}
		for (Contact contact : contacts) {
			logger.debug("Processing contact " + contact.getId());
			Contact outContact = new Contact();
			outContact.setId(contact.getId());
			outContact.setChatType(contact.getChatType());
			if (ChatType.One2One == ChatType.getChatTypeById(outContact.getChatType().intValue())) {
				// TODO changed true to false in getPresence() call so as to send presence as
				// unavailable
				Presence presence = presenceService.getPresence(outContact.getId(), true);
				outContact.setPresence(presence);
				JsonNode user = cacheUserDao.find(outContact.getId());
				if (user != null) {
					commonUtil.setUserLocation(outContact, user);
				}
			}
			outContact.setContactType(null);
			outContact.setUnreadMsg(null);
			outContacts.add(outContact);
			ChatSummary chat = null;
			if (chatSummaryMap.containsKey(ChatType.One2One.name() + outContact.getId())) {
				chat = chatSummaryMap.get(ChatType.One2One.name() + outContact.getId());
				chat.setContactId(null);
				chat.setChatType(null);
			} else if (chatSummaryMap.containsKey(ChatType.GroupChat.name() + outContact.getId())) {
				chat = chatSummaryMap.get(ChatType.GroupChat.name() + outContact.getId());
				chat.setContactId(null);
				chat.setChatType(null);
			} else {
				chat = new ChatSummary();
				chat.setUnReadMsgCount(Short.valueOf("0"));
			}
			outContact.setChatSummary(chat);
			logger.debug("Processed contact " + contact.getId());
		}
		return outContacts;
	}

	/**
	 * @param contact
	 */
	private void setContactNumbers(Contact contact) {
		JsonNode userNode = cacheUserDao.find(contact.getId());
		if (userNode != null) {
			JsonNode mobileNumberNode = userNode.findPath("mobileNumber");
			if (mobileNumberNode != null && !mobileNumberNode.isMissingNode()) {
				String mobileNumber = mobileNumberNode.asText();
				mobileNumber = mobileNumber.replace("#", "");
				logger.debug("Processed mobileNumber " + mobileNumber + " for contactId " + contact.getId());
				contact.setMobileNumber(mobileNumber);
			}
			JsonNode landLineNumberNode = userNode.findPath("landLineNumber");
			if (landLineNumberNode != null && !landLineNumberNode.isMissingNode()) {
				String landLineNumber = landLineNumberNode.asText();
				landLineNumber = landLineNumber.replace("#", "");
				logger.debug("Processed landLineNumber " + landLineNumber + " for contactId " + contact.getId());
				contact.setLandlineNumber(landLineNumber);
			}
		}

	}

	@Override
	public List<Contact> getContactsV2(UserContext userContext, Integer offset, Integer limit, String searchKey,
			Integer contactId, Integer inputChatType) {
		logger.info("get contacts for user " + userContext.getUser().getId() + " and search key = " + searchKey);
		List<Contact> contacts = getChatContacts(userContext, offset, limit, searchKey, contactId, inputChatType);
		logger.info("got contacts for user " + userContext.getUser().getId() + " with size "
				+ (contacts == null ? contacts : contacts.size()));
		if (contacts != null && !contacts.isEmpty()) {
			List<Integer> userIds = new ArrayList<Integer>();
			List<Integer> groupIds = new ArrayList<Integer>();
			for (Contact contact : contacts) {
				if (ChatType.One2One.getId().byteValue() == contact.getChatType().byteValue()) {
					userIds.add(contact.getId());
				}
				if (ChatType.GroupChat.getId().byteValue() == contact.getChatType().byteValue()) {
					groupIds.add(contact.getId());
				}
			}

			// Map<Integer, ChatSummary> map =
			// one2OneChatDao.getUnReadMsgCountAndLastMsgUsers(userContext.getUser().getId(),
			// contactIds);
			Map<Byte, List<Integer>> contactListMap = new HashMap<Byte, List<Integer>>();
			if (!userIds.isEmpty()) {
				contactListMap.put(ChatType.One2One.getId(), userIds);
			}
			if (!groupIds.isEmpty()) {
				contactListMap.put(ChatType.GroupChat.getId(), groupIds);
			}

			Map<String, ChatSummary> chatSummeries = chatSummaryDao.getContactChatSummary(userContext.getUser().getId(),
					contactListMap);
			logger.info("got chatSummeries for One2One contacts of size  "
					+ (chatSummeries == null ? null : chatSummeries.size()));
			boolean setPresence = false;
			if (contactId != null) {
				setPresence = true;
			}
			for (Contact contact : contacts) {
				logger.debug("Processing contact " + contact.getId());
				if (ChatType.One2One.getId().byteValue() == contact.getChatType().byteValue()) {// set presence only for
					// once2one
					Presence presence = presenceService.getPresence(contact.getId(), setPresence);
					contact.setPresence(presence);
				}

				// Set one2one and groupChat Summary
				ChatSummary chatSummary = chatSummeries.get(contact.getId() + "_" + contact.getChatType().byteValue());
				if (chatSummary != null) {
					contact.setChatSummary(chatSummary);
					String lastMsgDate = commonUtil.getDateTimeWithTimeZone(chatSummary.getTime(),
							userContext.getUser().getTimezone());
					chatSummary.setLastMsgDate(lastMsgDate);
					chatSummary.setUtcDate(chatSummary.getTime());
					if (chatSummary.getLastMsgSenderId() != null) {
						Integer lastMsgSenderId = chatSummary.getLastMsgSenderId().intValue();
						logger.debug("get last Message for contact " + contact.getId() + " with lastMsgSenderId = "
								+ lastMsgSenderId);
						JsonNode userJson = cacheUserDao.find(lastMsgSenderId);
						chatSummary.setLastMsgSenderName(userJson.findPath("firstName").asText());
						logger.debug("last Message Sender Name set as  " + userJson.findPath("firstName").asText());
					}
					// set unread count
					if (chatSummary.getUnReadMsgCount() > 0) {
						contact.setUnreadMsg(true);
					}
				}
				logger.debug("Processed contact " + contact.getId());
			}
		}
		return contacts;
	}

	private List<Contact> getChatContacts(UserContext userContext, Integer offset, Integer limit, String searchKey,
			Integer contactId, Integer inputChatType) {
		List<Contact> contacts = new ArrayList<>();
		try {
			logger.info("Getting Chat Contacts of " + contactId + " From Ims ");
			String contactStr = contactDao.getChatContactsFromIms(userContext, offset, limit, searchKey, contactId,
					inputChatType);
			logger.info("Got Chat Contacts of " + contactId + " From Ims ");
			if (contactStr != null && !"".equalsIgnoreCase(contactStr)) {
				ObjectMapper mapper = new ObjectMapper();
				ArrayNode node = (ArrayNode) mapper.readTree(contactStr);
				Byte chatType = null;
				Contact contact = null;
				if (node.isArray() && node.size() > 0) {
					for (int indx = 0; indx < node.size(); indx++) {
						JsonNode json = node.get(indx);

						JsonNode chatTypeNode = json.findPath("chatType");
						if (chatTypeNode != null && !chatTypeNode.isMissingNode()) {
							chatType = Byte.valueOf(chatTypeNode.asText());
						} else {
							chatType = ChatType.One2One.getId();
						}
						logger.info("Contact " + json.findPath("id").asInt() + " has chatType " + chatType);
						if (Enums.ChatType.One2One.getId().equals(chatType)) {
							contact = new Contact();
							contact.setId(json.findPath("id").asInt());
							contact.setContactType(json.findPath("contactType").asInt());
							contact.setChatType(chatType);
							contact.setUserStatus(json.findPath("userStatus").asText());
							JsonNode userCategoryNode = json.findPath("userCategory");
							if (userCategoryNode != null && !userCategoryNode.isMissingNode()) {
								contact.setUserCategory(Byte.valueOf(userCategoryNode.asText()));
							}
							JsonNode user = cacheUserDao.find(contact.getId());
							logger.debug("User found in cache with id : " + contact.getId());
							commonUtil.setUserDetails(contact, user);
							contact.setOrgId(user.findPath("orgId").asInt());
							Integer loggedInUserId = userContext.getUser().getId();
							contact.setIsJourneyMapAccessible(isJourneyMapShared(loggedInUserId, contact));
							contact.setIsUserAttendanceAccessible(isAttendanceRegisterShared(loggedInUserId, contact));
						} else if (Enums.ChatType.GroupChat.getId().equals(chatType)) {
							Integer groupId = json.findPath("id").asInt();
							Group group = cacheGroupDao.getGroup(groupId);
							if (group == null) {
								logger.warn("No group found in cache with id : " + groupId);
								continue;
							}
							logger.debug("Group found in cache with id : " + group.getId());
							contact = createGroupContact(group);
							contact.setId(groupId);
							contact.setContactType(json.findPath("contactType").asInt());
							contact.setChatType(chatType);
						}
						contacts.add(contact);
						logger.debug("Contact created with contact id : " + contact.getId() + " and chat type : "
								+ chatType);
					}
				}
			}
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					ErrorCode.Internal_Server_Error.getName(), ex);
		}
		return contacts;
	}

	private GroupContact createGroupContact(Group group) {
		GroupContact groupContact = new GroupContact();
		groupContact.setGroupStatus(group.getGroupStatus());
		groupContact.setGroupType(group.getGroupType());
		groupContact.setGroupCreatedById(group.getCreatedById());
		groupContact.setTotalMembersCount(group.getMembersCount());
		groupContact.setName(group.getName());

		List<GroupMember> members = group.getMembers();
		if (members != null && !members.isEmpty()) {
			Set<Contact> groupMembers = new LinkedHashSet<Contact>();
			for (GroupMember member : members) {
				Contact contact = new Contact();
				contact.setId(member.getId());
				contact.setMemberStatus(member.getMemberStatus());
				contact.setMemberRole(member.getMemberRole());
				if (member.getLeftDate() != null) {
					contact.setLeftDate(member.getLeftDate().toString());
				}
				JsonNode user = cacheUserDao.find(member.getId());
				commonUtil.setUserDetails(contact, user);
				logger.info("contact email : {}", contact.getEmail());
				groupMembers.add(contact);
				logger.debug("Added GroupMember " + member.getId() + " to Group " + group.getName());
			}
			groupContact.setGroupMembers(groupMembers);
			logger.debug("Set " + groupMembers.size() + " GroupMember to Group " + group.getName());
		}
		return groupContact;
	}

	@Override
	public Contact getUserInfo(Integer loggedInUserId, Integer contactId) {
		logger.debug("User " + loggedInUserId + " trying to get userInfo of user " + contactId);
		JsonNode user = cacheUserDao.find(contactId);
		if (user == null) {
			throw new ResourceNotFoundException(ErrorCode.User_not_found, ErrorCode.User_not_found.getName());
		}
		Contact contact = new Contact();
		try {
			contact.setName(user.findPath("firstName").asText());
			contact.setId(user.findPath("id").asInt());
			contact.setContactType(null);
			contact.setUnreadMsg(null);
			contact.setUserStatus(user.findPath("CustomStatus").asText());
			contact.setUserType(user.findPath("userType").asText());
			contact.setOrgId(user.findPath("orgId").asInt());

			JsonNode photoURL = user.findPath("photoURL");
			logger.debug("User " + contactId + " found with photoURL " + photoURL);
			if (photoURL != null && !photoURL.isMissingNode()) {
				String photoStr = photoURL.asText();
				if (photoStr != null && !"".equalsIgnoreCase(photoStr)) {
					photoStr = photoStr.replaceAll("//", "");
					ObjectMapper mapper = new ObjectMapper();
					JsonNode photoNode = (JsonNode) mapper.readTree(photoStr);
					if (photoNode != null && !photoNode.isMissingNode()) {
						UserPhoto photo = new UserPhoto();
						JsonNode profile = photoNode.findPath("profile");
						photo.setProfile(profile.asText());
						JsonNode thumbnail = photoNode.findPath("thumbnail");
						photo.setThumbnail(thumbnail.asText());
						contact.setPhotoURL(photo);
					}
				}
			}

			JsonNode loggedInuser = cacheUserDao.find(loggedInUserId);
			if (loggedInuser == null) {
				throw new ResourceNotFoundException(ErrorCode.User_not_found, ErrorCode.User_not_found.getName());
			}
			Integer userCategory = loggedInuser.findPath("userCategory").asInt();
			Integer categoryValue = UserCategory.Guest.getId();
			if (userCategory.equals(categoryValue)) {
				commonUtil.setCustomStatusPreference(contact, user);
			}

			logger.debug("Check if User " + loggedInUserId + " and user " + contactId + " are in contact");
			if (cacheService.isInContact(loggedInUserId, contactId) || loggedInUserId.equals(contactId)) {
				commonUtil.setUserDetails(contact, user);
				contact.setIsJourneyMapAccessible(isJourneyMapShared(loggedInUserId, contact));
				logger.debug("User " + loggedInUserId + " and user " + contactId
						+ " are in contact, set flag IsJourneyMapAccessible");
				contact.setIsUserAttendanceAccessible(isAttendanceRegisterShared(loggedInUserId, contact));
				logger.debug("set flag IsUserAttendanceAccessible");
				// set presence info
				// TODO changed true to false in getPresence() call so as to send presence as
				// unavailable
				Presence presence = presenceService.getPresence(contact.getId(), true);
				contact.setPresence(presence);
				logger.debug("set Presence info");

			}
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					ErrorCode.Internal_Server_Error.getName(), ex);
		}
		return contact;
	}

	@Override
	public Boolean isAdminUser(Integer userId) {
		logger.debug("Checking if user " + userId + " is admin");
		JsonNode userJson = cacheService.getUserJson(userId);
		String strRoles = userJson.findPath("roles").asText();
		String[] roleNames = strRoles.split(Constants.COMMA_SEPARATOR);
		if (roleNames != null && roleNames.length > 0) {
			for (String role : roleNames) {
				if (RoleName.Admin.name().equalsIgnoreCase((role))) {
					logger.debug("User " + userId + " is admin");
					return true;
				}
			}
		}
		logger.debug("User " + userId + " is not admin");
		return false;
	}


	@Override
	public Boolean isBroadcaster(Integer userId) {
		logger.info("Checking if user " + userId + " is admin");
		JsonNode userJson = cacheService.getUserJson(userId);
		logger.info("userJson " + userJson);
		String strRoles = userJson.findPath("roles").asText();
		String[] roleNames = strRoles.split(Constants.COMMA_SEPARATOR);

		if (roleNames != null && roleNames.length > 0) {
			for (String role : roleNames) {
				logger.info("role " + role);
				if (RoleName.Broadcaster.name().equalsIgnoreCase(role)) {
					logger.info("User " + userId + " is Broadcaster");
					return true;
				}
			}
		}
		logger.info("User " + userId + " is not braodcaster");
		return false;
	}

	/**
	 * Returns true if tracked user's journey map can be seen by current user
	 *
	 * @param user
	 * @param user2
	 * @return
	 */
	private Boolean isJourneyMapShared(Integer currentUserId, Contact contact) {
		logger.debug("Checking if User " + currentUserId + " can see JourneyMap of user  " + contact.getId());
		JsonNode user = cacheUserDao.find(currentUserId);
		Integer currentUserOrgId = user.findPath("orgId").asInt();
		if (!(hasAdminEnabledLocationFeature(contact.getOrgId()) && hasAdminEnabledLocationFeature(currentUserOrgId))) {
			logger.debug("User " + currentUserId + " can't see JourneyMap of user  " + contact.getId());
			return false;
		}
		if (currentUserId == contact.getId().intValue()) {// user can see his own journey map
			logger.debug("User " + currentUserId + " can see JourneyMap of user  " + contact.getId());
			return true;
		}
		Boolean hasUserSharedJM = false;
		List<Map<String, String>> prefs = contact.getUserPreferences();
		if (prefs != null && !prefs.isEmpty()) {
			for (Map<String, String> map : prefs) {
				if ("ShareJourneyMap".equalsIgnoreCase(map.get("name"))) {
					hasUserSharedJM = Boolean.valueOf(map.get("value"));
					break;
				}
			}
		}
		if (hasUserSharedJM) {
			logger.debug("User " + currentUserId + " can see JourneyMap of user  " + contact.getId());
			return true;
		}

		if (isInReportingHierarchy(currentUserId, contact.getId())) {
			logger.debug("User " + currentUserId + " can see JourneyMap of user  " + contact.getId());
			return true;
		}
		logger.debug("User " + currentUserId + " can't see JourneyMap of user  " + contact.getId());
		return false;
	}

	private Boolean isAttendanceRegisterShared(Integer currentUserId, Contact contact) {
		logger.debug("Checking if User " + currentUserId + " can see AttendanceRegister of user  " + contact.getId());
		JsonNode user = cacheUserDao.find(currentUserId);
		Integer currentUserOrgId = user.findPath("orgId").asInt();
		if (!(hasAdminEnabledLocationFeature(contact.getOrgId()) && hasAdminEnabledLocationFeature(currentUserOrgId))) {
			logger.debug("User " + currentUserId + " can't see AttendanceRegister of user  " + contact.getId());
			return false;
		}
		if (currentUserId == contact.getId().intValue()) {// user can see his own attendance register
			logger.debug("User " + currentUserId + " can see AttendanceRegister of user  " + contact.getId());
			return true;
		}
		Boolean hasUserSharedAR = false;
		List<Map<String, String>> prefs = contact.getUserPreferences();
		if (prefs != null && !prefs.isEmpty()) {
			for (Map<String, String> map : prefs) {
				if (map.get("name").equalsIgnoreCase("ShareAttendanceRegister")) {
					hasUserSharedAR = Boolean.valueOf(map.get("value"));
					break;
				}
			}
		}
		if (hasUserSharedAR) {
			logger.debug("User " + currentUserId + " can see AttendanceRegister of user  " + contact.getId());
			return true;
		}

		if (isInReportingHierarchy(currentUserId, contact.getId())) {
			logger.debug("User " + currentUserId + " can see AttendanceRegister of user  " + contact.getId());
			return true;
		}
		logger.debug("User " + currentUserId + " can't see AttendanceRegister of user  " + contact.getId());
		return false;
	}

	private boolean isInReportingHierarchy(Integer currentUserId, Integer userId) {
		logger.debug("Checking if User " + userId + " is in ReportingHierarchy of user  " + currentUserId);
		int cnt = 25;// used to overcome the cyclic reporting manager issue
		try {
			JsonNode reportee = cacheUserDao.find(userId);
			Integer managerId = reportee.findPath("reportingManagerId").asInt();
			while ((managerId != null && managerId > 0) || cnt == 0) {
				if (currentUserId.equals(managerId)) {
					logger.debug("User " + userId + " is in ReportingHierarchy of user  " + currentUserId);
					return true;
				}
				reportee = cacheUserDao.find(managerId);
				managerId = reportee.findPath("reportingManagerId").asInt();
				cnt--;
			}
		} catch (Throwable e) {// if user is not logged in, may happen in case of
			// getUserInfoByEmail-mainsite:signup page
			logger.error("current user is not logged in, returning false");
			return false;
		}
		logger.debug("User " + userId + " is not in ReportingHierarchy of user  " + currentUserId);
		return false;
	}

	private boolean hasAdminEnabledLocationFeature(Integer orgId) {
		logger.debug("Checking if Admin has EnabledLocationFeature for org  " + orgId);
		JsonNode orgNode = cacheOrgDao.find(orgId);
		JsonNode prefs = orgNode.findPath("settings");
		boolean hasAccess = false;
		if (prefs != null && prefs.isArray()) {
			for (JsonNode pref : prefs) {
				if (Constants.ENABLE_DEVICE_LOCATION_FEATURE.equalsIgnoreCase(pref.findPath("preference").asText())) {
					if ("TRUE".equalsIgnoreCase(pref.findPath("value").asText())) {
						hasAccess = true;
						break;
					}
				}
			}
		}
		logger.debug("EnabledLocationFeature flag for org  " + orgId + " is set to " + hasAccess);
		return hasAccess;
	}

	private void createChatContact(UserContext userContext, ChatContact contact) {
		logger.info("create Chat Contact for contact Id " + contact.getId() + " of chattype " + contact.getChatType());
		if (ChatType.GroupChat.getId().equals(contact.getChatType())) {
			logger.debug("Setting Group Members for Group Chat Contact, Id " + contact.getId());
			if (contact.getGroupMembersStr() != null && !"".equals(contact.getGroupMembersStr().trim())) {
				JsonNode memberNodes = Json.parse(contact.getGroupMembersStr());
				Set<Contact> members = new LinkedHashSet<Contact>();
				for (int count = 0; count < memberNodes.size(); count++) {
					memberNodes.get(count);
					// Fix for iOS
					Integer contactType = memberNodes.get(count).get("contactType").asInt();
					contactType = commonUtil.overRideContactType(userContext, contactType);

					Contact groupMember = new Contact(memberNodes.get(count).get("name").asText(),
							memberNodes.get(count).get("Id").asInt(),
							memberNodes.get(count).has("email") ? memberNodes.get(count).get("email").asText() : null,
							Byte.parseByte(memberNodes.get(count).get("memberStatus").asText()),
							memberNodes.get(count).has("photoUrl") ? memberNodes.get(count).get("photoUrl").asText()
									: null,
									memberNodes.get(count).has("designation")
									? memberNodes.get(count).get("designation").asText()
											: null,
											memberNodes.get(count).has("departmentName")
											? memberNodes.get(count).get("departmentName").asText()
													: null,
													contactType,
													memberNodes.get(count).has("memberRole")
													? Byte.parseByte(memberNodes.get(count).get("memberRole").asText())
															: null);
					groupMember.setUserCategory(Byte.parseByte(memberNodes.get(count).get("userCategory").asText()));
					groupMember.setUnreadMsg(null);
					logger.info("email: {}"+groupMember.getEmail());
					
                 
					// Populate the device of the guest user in the group
					if (UserCategory.Guest.getId().equals(memberNodes.get(count).get("userCategory").asInt())) {
						int guestId = memberNodes.get(count).get("Id").asInt();
						List<Integer> userIds = new ArrayList<Integer>();
						userIds.add(guestId);
						List<Device> devices = deviceDao.getDevices(userIds, NotificationType.Chat);
						List<Device> webBrowserDevices = CommonUtil.getDevicesByType(devices, DeviceType.WebBrowser);
						logger.debug("Number of Devices registered from WebBrowser: " + webBrowserDevices.size()
						+ " for contact : " + guestId);
						contact.setDevices(webBrowserDevices);
						groupMember.setEmail(null);
					}
					members.add(groupMember);
				}
			
				contact.setGroupMembers(members);
				logger.debug("Set " + members.size() + " Group Members for Group Chat Contact, Id " + contact.getId());

				JsonNode prefs = null;
				if (contact.getGroupPreferencesStr() != null
						&& !"".equalsIgnoreCase(contact.getGroupPreferencesStr().trim())) {
					prefs = Json.parse(contact.getGroupPreferencesStr());
				}
				logger.debug("Set preferences for Group " + contact.getId() + " with preferences " + prefs);
				if (prefs != null) {
					List<Map<String, String>> preferences = new ArrayList<Map<String, String>>();
					for (int count = 0; count < prefs.size(); count++) {
						JsonNode pref = prefs.get(count);
						String name = pref.findPath("preference").asText();
						if (name != null && name.equalsIgnoreCase("EnableOpenChat")) {
							Map<String, String> mapCustStatus = new HashMap<String, String>();
							mapCustStatus.put("name", "EnableOpenChat");
							mapCustStatus.put("value", pref.findPath("value").asText());
							preferences.add(mapCustStatus);
						}
						if (name != null && name.equalsIgnoreCase("EnableRoundRobin")) {
							Map<String, String> mapCustStatus = new HashMap<String, String>();
							mapCustStatus.put("name", "EnableRoundRobin");
							mapCustStatus.put("value", pref.findPath("value").asText());
							preferences.add(mapCustStatus);
						}
						if (name != null && name.equalsIgnoreCase("EnableOneWayGroupChat")) {
							Map<String, String> mapCustStatus = new HashMap<String, String>();
							mapCustStatus.put("name", "EnableOneWayGroupChat");
							mapCustStatus.put("value", pref.findPath("value").asText());
							preferences.add(mapCustStatus);
						}
					}
					if (!preferences.isEmpty()) {
						contact.setGroupPreferences(preferences);
						logger.debug("Set preferences " + preferences.toString());
					}
				}
			}
		} else if (ChatType.One2One.getId().equals(contact.getChatType())) {
			if (contact.getPhotoURLStr() != null && !"".equals(contact.getPhotoURLStr().trim())) {
				contact.setPhotoURL(Json.fromJson(Json.parse(contact.getPhotoURLStr()), UserPhoto.class));
			}
			logger.debug("Set photo URL for One2One Contact " + contact.getId() + " with PhotoURL "
					+ contact.getPhotoURLStr());
			JsonNode prefs = null;
			if (contact.getUserPreferencesStr() != null
					&& !"".equalsIgnoreCase(contact.getUserPreferencesStr().trim())) {
				prefs = Json.parse(contact.getUserPreferencesStr());
			}
			logger.debug("Set preferences for One2One Contact " + contact.getId() + " with preferences " + prefs);
			if (prefs != null) {
				List<Map<String, String>> preferences = new ArrayList<Map<String, String>>();
				for (int count = 0; count < prefs.size(); count++) {
					JsonNode pref = prefs.get(count);
					String name = pref.findPath("name").asText();
					if (name != null && name.equalsIgnoreCase("CustomStatus")) {
						Map<String, String> mapCustStatus = new HashMap<String, String>();
						mapCustStatus.put("name", "CustomStatus");
						mapCustStatus.put("value", pref.findPath("value").asText());
						preferences.add(mapCustStatus);
					}
					if (name != null && name.equalsIgnoreCase("ShareDeviceLocation")) {
						Map<String, String> mapTrackDev = new HashMap<String, String>();
						mapTrackDev.put("name", "ShareDeviceLocation");
						mapTrackDev.put("value", pref.findPath("value").asText());
						preferences.add(mapTrackDev);
					}
					if (name != null && name.equalsIgnoreCase("ShareJourneyMap")) {
						Map<String, String> mapShareJourney = new HashMap<String, String>();
						mapShareJourney.put("name", "ShareJourneyMap");
						mapShareJourney.put("value", pref.findPath("value").asText());
						preferences.add(mapShareJourney);
					}
					if (name != null && name.equalsIgnoreCase("ShareAttendanceRegister")) {
						Map<String, String> mapShareAttendance = new HashMap<String, String>();
						mapShareAttendance.put("name", "ShareAttendanceRegister");
						mapShareAttendance.put("value", pref.findPath("value").asText());
						preferences.add(mapShareAttendance);
					}
					if (name != null && name.equalsIgnoreCase("AllowVideoChat")) {
						Map<String, String> mapShareAttendance = new HashMap<String, String>();
						mapShareAttendance.put("name", "AllowVideoChat");
						mapShareAttendance.put("value", pref.findPath("value").asText());
						preferences.add(mapShareAttendance);
					}
				}
				if (!preferences.isEmpty()) {
					contact.setUserPreferences(preferences);
					logger.debug("Set preferences " + preferences.toString());
				}
			}
			Integer loggedInUserId = userContext.getUser().getId();
			Contact contact1 = new Contact();
			contact1.setId(contact.getId());
			contact1.setOrgId(contact.getOrgId());
			contact1.setUserPreferences(contact.getUserPreferences());
			contact.setIsJourneyMapAccessible(isJourneyMapShared(loggedInUserId, contact1));
			contact.setIsUserAttendanceAccessible(isAttendanceRegisterShared(loggedInUserId, contact1));
			contact.setUserStatus(null);

			// Populate the device of the user
			List<Integer> userIds = new ArrayList<Integer>();
			userIds.add(contact.getId());
			List<Device> devices = deviceDao.getDevices(userIds, NotificationType.Chat);
			List<Device> webBrowserDevices = CommonUtil.getDevicesByType(devices, DeviceType.WebBrowser);
			logger.debug("Number of Devices registered from WebBrowser: " + webBrowserDevices.size() + " for contact : "
					+ contact.getId());
			contact.setDevices(webBrowserDevices);

			JsonNode user = cacheUserDao.find(contact.getId());
			if (user != null) {
				if (!ChatContactType.Customer.getId().equals(contact.getContactType())) {
					commonUtil.setUserLocation(contact1, user);
					if (contact1.getDeviceLocation() != null) {
						if (checkDateDiff(contact1.getDeviceLocation())) {
							contact.setDeviceLocation(contact1.getDeviceLocation());
						}
					}
				}
				logger.debug("Set DeviceLocation " + contact1.getDeviceLocation());
			}
			if (contact.getActive()) {
				Presence presence = presenceService.getPresence(contact.getId(), true);
				contact.setPresence(presence);
				logger.debug("Set Presence " + presence);
			}
		}

	}


	public Boolean checkDateDiff(DeviceLocation deviceLocation) {
		if (deviceLocation.getDayDiff() != null && deviceLocation.getDayDiff() > -1) {
			return true;
		}
		return false;
	}

	@Override
	public Optional<UnreadCountSummary> unreadCountSummary(Integer orgId, String userName) {
		return chatSummaryDao.unreadCountSummary(orgId, userName);
	}

}
