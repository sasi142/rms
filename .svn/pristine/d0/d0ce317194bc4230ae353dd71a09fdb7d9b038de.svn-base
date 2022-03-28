package core.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.workapps.common.core.services.DataEncryptionService;
import core.daos.*;
import core.entities.*;
import core.exceptions.InternalServerErrorException;
import core.exceptions.ResourceNotFoundException;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.Enums.OrganizationFlavour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import play.libs.Json;

import java.util.*;
import java.util.stream.Collectors;

@Service("RmsCacheService")
public class CacheServiceImpl implements CacheService {
	final static Logger logger = LoggerFactory.getLogger(CacheServiceImpl.class);

	@Autowired
	@Qualifier("CacheImpl")
	private CacheConnectionInfoDao cacheConnectionInfoDao;

	@Autowired
	private CacheUserDao cacheUserDao;

	@Autowired
	private CacheContactsDao cacheContactDao;

	@Autowired
	private CacheClientCertificateDao cacheClientCertificateDao;

	@Autowired
	private CacheOrgDao cacheOrgDao;

	@Autowired
	private CacheChannelDao cacheChannelDao;
	
	@Autowired
	private CacheGroupDao cacheGroupDao;
	
	@Autowired
	private CacheUserVideoKycDao cacheUserVideoKycDao;

	@Autowired
	private CacheVideoKycDao cacheVideoKycDao;

	@Autowired
	private CacheApplicationSettingsDao cacheApplicationSettingsDao;

	@Autowired
	private DataEncryptionService dataEncryptionService;

	public CacheServiceImpl() {

	}
	
	@Override
	public Map<String, ArrayNode> getAll() {
		Map<String, ArrayNode> map = cacheConnectionInfoDao.getAll();
		return map;
	}

	@Override
	public void remove(Integer userId, String uuid) {
		cacheConnectionInfoDao.remove(userId, uuid);
	}

	@Override
	public JsonNode getUserJson(Integer id) {
		JsonNode userJson = cacheUserDao.find(id);
		//decrypt
		userJson = decryptUser(userJson);
		logger.debug("got User Json for userId " + id + " as " + userJson);
		return userJson;
	}

	private JsonNode decryptUser(JsonNode userJson) {
		Long id = userJson.findPath("id").asLong();
		String userName = userJson.findPath("firstName").asText();
		Integer dataEncryptionKeyId = userJson.has("dataEncryptionKeyId") ? userJson.findPath("dataEncryptionKeyId").asInt():null;
		if (dataEncryptionKeyId != null){
			logger.debug("Will decrypt user {}", id);
			userName = dataEncryptionService.decrypt(dataEncryptionKeyId, userName);
			JsonParser parser = new JsonParser();
			JsonObject jsonObject = parser.parse(userJson.toString()).getAsJsonObject();
			jsonObject.addProperty("firstName", userName);
			String updatedJson = jsonObject.toString();
			return Json.parse(updatedJson);
		}
		return userJson;
	}

	@Override
	public List<JsonNode> getUserJsons(List<Integer> userIds) {
		logger.info("get User Json list for userIds " + userIds);
		List<JsonNode> userJsons = cacheUserDao.findAll(userIds);
		List<JsonNode> decryptedUserJsons = userJsons.stream().map(this::decryptUser).collect(Collectors.toList());
		logger.info("got User Json for userId " + userIds);
		return userJsons;
	}

    @Override
    public void markKycAgentAssigned(Integer kycId, Integer agentId, Long groupId) {
		logger.info("Mark kyc agent assigned. Kyc {}, Agent {}, Group {}", kycId, agentId, groupId);
		ObjectNode videoKyc = cacheVideoKycDao.get(kycId);
		if (videoKyc == null){
			throw new ResourceNotFoundException(Enums.ErrorCode.Entity_Not_Found,
					String.format("Video KYC not found for id %d", kycId));
		}
		JsonNode status = videoKyc.get("status");
		if (status != null){
			//we have found invalid status
			if (status.asInt() == 1){
				//Open status
				//will be updated below
			}
			else if (status.asInt() == 3){
				//already agent assigned
				//we will update again
			}
			else {
				//TODO: what to do here??
				throw new InternalServerErrorException(Enums.ErrorCode.KYC_STATUS_INVALID,
						String.format("Invalid KYC Statys found %s", status));
			}
		}
		videoKyc.put("status", Enums.VideoKYCStatus.AgentAssigned.getId());
		videoKyc.put("agentId", agentId);
		cacheVideoKycDao.put(kycId, videoKyc);
		logger.info("Mark kyc agent assigned. Added to video kyc cache. Kyc {}, Agent {}, Group {}", kycId, agentId, groupId);
		Group group = cacheGroupDao.getGroup(groupId.intValue());
		boolean hasAgentAsMember = group.getMembers().stream().anyMatch(member -> member.getId().equals(agentId));
		if (hasAgentAsMember){
			//TODO: agent is already member of the group, what to do
			logger.warn("Agent {} already assigned to this KYC {}.", agentId, kycId);
		}

		GroupMember groupMember = new GroupMember();
		groupMember.setId(agentId);
		groupMember.setMemberRole(Enums.RoleName.VideoKYCAgent.getId().byteValue());
		groupMember.setMemberStatus((byte) 1);
		group.getMembers().add(groupMember);
		cacheGroupDao.put(groupId.intValue(), group);
		logger.info("Mark kyc agent assigned. Added to group cache. Kyc {}, Agent {}, Group {}", kycId, agentId, groupId);
	}

	@Override
	public void storeUserUnreadCount(Integer userId, String data) {
		cacheUserDao.storeUserUnreadCount(userId, data);
	}

	@Override
	public String getUserUnreadCount(Integer userId) {
		return cacheUserDao.getUserUnreadCount(userId);
	}

	@Override
	public JsonNode getOrgJson(Integer orgId) {
		JsonNode orgJson = cacheOrgDao.find(orgId);
		logger.debug("got Org Json for orgId " + orgId + " as " + orgJson);
		return orgJson;
	}
	
	@Override
	public ArrayNode getAppSettingsJson(Integer orgId) {
		ArrayNode appSettingsJson = cacheApplicationSettingsDao.getApplicationSettingByOrgId(orgId);
		logger.debug("got appSettingsJson Json for orgId " + orgId );
		return appSettingsJson;
	}
	
	@Override
	public String[] getUserRole(Integer userId) {
		logger.debug("get User Json for userId " + userId);
		JsonNode userJson = cacheUserDao.find(userId);
		String[] role = userJson.findPath("roles").asText().split(Constants.COMMA_SEPARATOR);
		logger.info("role: "+role);
		return role;
	}
	
	@Override
	public String getUserCustomStatus(Integer userId) {
		logger.debug("get User Json for userId " + userId);
		JsonNode userJson = cacheUserDao.find(userId);
		final JsonNode arrNode = userJson.get("userPreferences");
		String customStatus = null;
		if (arrNode != null && arrNode.isArray()) {
		    for (final JsonNode objNode : arrNode) {
		    	logger.debug("user preference: "+objNode.toString());
		    	String key = objNode.findPath("name").asText();
		    	if ("CustomStatus".equalsIgnoreCase(key)) {
		    		customStatus = objNode.findPath("value").asText();
		    		break;
		    	}		    	
		    }
		}
		return customStatus;
	}

	public User getUser(Integer userId, boolean all) {
		logger.debug("Load User Json for userId " + userId);
		JsonNode userJson = cacheUserDao.find(userId);
		logger.debug("User Json for userId {} as {}",userId, userJson);
		JsonNode decryptedUserJson = decryptUser(userJson);
		logger.debug("Decrypted User Json for userId {} as {}",userId, decryptedUserJson);
		User user = new User();
		user.setId(userId);
		user.setName(decryptedUserJson.findPath("firstName").asText());

		user.setRoles(decryptedUserJson.findPath("roles").asText());
		user.setActive(Boolean.valueOf(decryptedUserJson.findPath("active").asText()));
		user.setTimezone(decryptedUserJson.findPath("timezone").asText());
		user.setOrganizationId(decryptedUserJson.findPath("orgId").asInt());
		user.setUserCategory(Byte.valueOf(decryptedUserJson.findPath("userCategory").asText()));
		if (all) {
			// get desiganation,department,sub-department,office,mobile,email
			user.setDesignation(decryptedUserJson.findPath("designation").asText());
			user.setDepartment(decryptedUserJson.findPath("departmentName").asText());
			user.setSubDepartment(decryptedUserJson.findPath("subDepartmentName").asText());
			// office : TODO not in cache
			user.setMobile(decryptedUserJson.findPath("mobileNumber").asText());
			user.setEmail(decryptedUserJson.findPath("email").asText());
			user.setUserName(decryptedUserJson.findPath("userName").asText());
			
			//JsonNode userDetailJson = cacheUserDao.find(userId);
		}

		JsonNode photoURL = decryptedUserJson.findPath("photoURL");
		if (photoURL != null && !photoURL.isMissingNode()) {
			String photoStr = photoURL.asText();
			if (photoStr != null && !"".equalsIgnoreCase(photoStr)) {
				photoStr = photoStr.replaceAll("//", "");
				ObjectMapper mapper = new ObjectMapper();
				try {
					JsonNode photoNode = (JsonNode) mapper.readTree(photoStr);
					if (photoNode != null && !photoNode.isMissingNode()) {
						UserPhoto photo = new UserPhoto();
						JsonNode profile = photoNode.findPath("profile");
						photo.setProfile(profile.asText());
						JsonNode thumbnail = photoNode.findPath("thumbnail");
						photo.setThumbnail(thumbnail.asText());
						user.setPhotoURL(photo);

						// TODO: Remove if not needed
						user.setThumbnailUrl(thumbnail.asText());
					}
				} catch (Exception ex) {
					logger.error("failed to parse photo url", ex);
				}
			}
		}
		logger.debug("got User Object for userId " + userId);
		return user;
	}

	@Override
	public List<Integer> getAllContacts(Integer userId) {
		JsonNode user = cacheUserDao.find(userId);
		logger.debug("got User Object for userId " + userId);
		Integer orgId = user.findPath("orgId").asInt();
		return cacheContactDao.findAll(userId, orgId);
	}

	@Override
	public List<Integer> getUsersWithOpenChatWindow(Integer userId) {
		return cacheContactDao.getUsersWithOpenChatWindow(userId);
	}

	@Override
	public JsonNode getUserObjectWithOpenChatWindow(Integer userId) {
		logger.info("get user openchatWindowObj started for Id: "+userId);
		JsonNode openchatWindowObj = null;
		try {
			openchatWindowObj = cacheContactDao.getUserObjectWithOpenChatWindow(userId);
			logger.debug(" openchatWindowObj found: "+openchatWindowObj.size());
		} catch (Exception ex) {
			logger.error("No any result found : "+ openchatWindowObj );
		}
		return openchatWindowObj;
	}

	@Override
	public Set<Integer> getUsersWithOpenMapWindow(Integer userId){
		Set<Integer> contactIds = new HashSet<Integer>();
		JsonNode user = cacheUserDao.find(userId);
		Integer orgId = user.findPath("orgId").asInt();
		contactIds = cacheContactDao.getUsersWithOpenMapWindow(userId, orgId);
		return contactIds;
	}

	// This is used in Memo
	@Override
	public Boolean isInContact(Integer userId, List<Integer> contactIds) {
		Boolean isInContact = true;
		for (Integer contactId : contactIds) {
			isInContact = isInContact(userId, contactId);
			if (!isInContact) {
				return isInContact;
			}
		}
		logger.debug(
				"For userId " + userId + " and  contactIds " + contactIds + ", isInContact status = " + isInContact);
		return isInContact;
	}

	@Override
	public Boolean isInContact(Integer userId, Integer contactId) {
		if (userId.intValue() == contactId.intValue()) {
			return true;
		}
		JsonNode user = cacheUserDao.find(userId);
		Integer userOrgId = user.findPath("orgId").asInt();
		if (isInSameOrg(userOrgId, contactId)) {
			return true;
		}
		Boolean isInContact = cacheContactDao.isInContact(userId, contactId);
		logger.debug("For userId " + userId + " and  contactIds " + contactId + ", isInContact status = " + isInContact);
		return isInContact;
	}
	
	@Override
	public Boolean isInContact(Integer userOrgId, Integer userId, Integer contactId) {
		if (userId.intValue() == contactId.intValue()) {
			return true;
		}
		if (isInSameOrg(userOrgId, contactId)) {
			return true;
		}
		Boolean isInContact = cacheContactDao.isInContact(userId, contactId);
		logger.debug("For userId " + userId + " and  contactIds " + contactId + ", isInContact status = " + isInContact);
		return isInContact;
	}

	@Override
	public ClientCertificate getClientCertificate(String bundleKey) {
		return cacheClientCertificateDao.getClientCertificate(bundleKey);
	}

	@Override
	public List<Integer> getBrandChatOrgs(String clientId) {
		Set<String> certKeys = cacheClientCertificateDao.getClientCertificateKeys();
		List<Integer> orgIds = new ArrayList<Integer>();
		for (String key : certKeys) {
			if (key.contains("_" + clientId + "_")) {
				orgIds.add(Integer.parseInt(key.substring(0, key.indexOf('_'))));
			}
		}
		return orgIds;
	}

	@Override
	public Integer refreshClientCertificateCache() {
		return cacheClientCertificateDao.refresh();
	}

	private Boolean isInSameOrg(Integer userOrgId, Integer contactId) {
		JsonNode orgNode = cacheOrgDao.find(userOrgId);
		if (OrganizationFlavour.CustomerChat.getId().byteValue() == Byte
				.valueOf(orgNode.findPath("organizationFlavour").asText()).byteValue()
				|| OrganizationFlavour.OpenChat.getId().byteValue() == Byte
						.valueOf(orgNode.findPath("organizationFlavour").asText()).byteValue()) {
			return false;
		}
		Boolean isOrgContact = cacheContactDao.isInOrgContact(userOrgId, contactId);
		logger.debug(
				"For contactId " + contactId + " and  OrgId " + userOrgId + ", isInSameOrg status = " + isOrgContact);
		return isOrgContact;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see core.services.CacheService#getAllOrgUserIds(java.lang.Integer)
	 */
	public List<Integer> getAllOrgUserIds(Integer organizationId) {
		return cacheContactDao.findAllOrgContacts(organizationId);
	}

	// for follow
	public List<Integer> getAllChannelContacts(Integer channelId) {
		return cacheContactDao.findAllChannelContacts(channelId);
	}

	@Override
	public boolean isFollowing(Integer channelId, List<Integer> recipientIds) {
		Boolean isFollowing = true;
		for (Integer recipientId : recipientIds) {
			isFollowing = isFollowing(channelId, recipientId);
			if (!isFollowing) {
				return isFollowing;
			}
		}
		logger.debug("For channel " + channelId + " and  recipientIds " + recipientIds + ", isFollowing status = "
				+ isFollowing);
		return isFollowing;
	}

	@Override
	public Boolean isFollowing(Integer channelId, Integer recipientId) {
		Boolean isInContact = cacheContactDao.isFollowing(channelId, recipientId);
		logger.debug("For channelId " + channelId + " and  recipientId " + recipientId + ", isFollowing status = "
				+ isInContact);
		return isInContact;
	}

	@Override
	public JsonNode getChannelJson(Integer channelId) {
		JsonNode channelJson = cacheChannelDao.find(channelId);
		logger.debug("got channel Json for Id " + channelId );
		return channelJson;
	}
	
	@Override
	public Group getGroupDetails(Integer groupId) {
		Group group = cacheGroupDao.getGroup(groupId) ;
		if (group.getDataEncryptionKeyId() != null){
			group.setName(dataEncryptionService.decrypt(group.getDataEncryptionKeyId(), group.getName()));
		}
		return group;
	}
	
	@Override
	public Boolean isInOrgContact(Integer userOrgId, Integer contactId) {
		Boolean isOrgContact = cacheContactDao.isInOrgContact(userOrgId, contactId);
		return isOrgContact;
	}

	@Override
	public JsonNode getVideoKycInfo(Integer userId) {
		return cacheUserVideoKycDao.getVideoKycId(userId);
	}
}
