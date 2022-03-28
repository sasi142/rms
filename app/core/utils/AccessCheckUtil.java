package core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import core.daos.CacheGroupDao;
import core.daos.CacheOrgDao;
import core.entities.Group;
import core.entities.IqMessage;
import core.exceptions.BadRequestException;
import core.exceptions.ForbiddenException;
import core.exceptions.ResourceNotFoundException;
import core.services.CacheService;
import core.utils.Enums.ChatType;
import core.utils.Enums.ErrorCode;

@Component
public class AccessCheckUtil {
	final static Logger logger = LoggerFactory.getLogger(AccessCheckUtil.class);

	@Autowired
	private CacheOrgDao cacheOrgDao;

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private CacheGroupDao cacheGroupDao;

	public Boolean isVideoChatAllowed(JsonNode user) {
		Boolean isVideoChatAllowed = false;
		try {
			Integer orgId = user.findPath("orgId").asInt();
			JsonNode organization = cacheOrgDao.find(orgId);
			JsonNode prefs = organization.findPath("settings");
			if (prefs != null && prefs.isArray()) {
				for (JsonNode pref : prefs) {
					if (Constants.ALLOW_VIDEO_CHAT_PREFERENCE.equalsIgnoreCase(pref.findPath("preference").asText())) {
						isVideoChatAllowed = pref.findPath("value").asBoolean();
						break;
					}
				}
			}
		} catch (Exception ex) {
			logger.info("Exception occurred while fetching AllowVideoChat preference from cache", ex);
		}
		return isVideoChatAllowed;
	}

	public void checkPrivateKey(Integer orgId, String requestApiKey) {
		Boolean valid = false;
		try {
			JsonNode organization = cacheOrgDao.find(orgId);
			JsonNode prefs = organization.findPath("settings");
			if (prefs != null && prefs.isArray()) {
				for (JsonNode pref : prefs) {
					if (Constants.API_KEY.equalsIgnoreCase(pref.findPath("preference").asText())) {
						String apiKey = pref.findPath("value").asText();
						if (requestApiKey.contentEquals(apiKey)) {
							valid = true;
						}
					}
				}
			}

		} catch (Exception ex) {
			logger.info("Exception occurred while getting ApiKey", ex);
		}
		if (!valid) {
			throw new ForbiddenException(ErrorCode.InvalidApiKey, "InvalidApiKey");
		}
	}

	public void isVideoRecordingEventAllowed(Integer fromUserId, IqMessage iqMessage) {
		Integer toUserId = Integer.valueOf(iqMessage.getParams().get("to").toString());

		if (fromUserId == null) {
			logger.error("caller not found in cache " + fromUserId);
			throw new ResourceNotFoundException(ErrorCode.User_not_found, "caller not found in cache " + fromUserId);
		}

		// Check if caller has the permission to make video calls
		JsonNode fromUserJson = cacheService.getUserJson(fromUserId);
		if (!isVideoChatAllowed(fromUserJson)) {
			logger.error("video chat not allowed for caller " + fromUserId);
			throw new ForbiddenException(ErrorCode.Action_Not_Supported,
					"video chat not allowed for caller " + fromUserId);
		}

		if (Integer.valueOf(iqMessage.getParams().get("chatType")).equals(ChatType.One2One.getId().intValue())) {
			// Check if sender and receiver are contacts
			Boolean isInContact = cacheService.isInContact(fromUserId, toUserId);
			if (!isInContact) {
				logger.info("caller and receiver not in contact");
				throw new ForbiddenException(ErrorCode.NotInContact, toUserId, fromUserId);
			}

			// Check if receiver exists
			JsonNode toUserJson = cacheService.getUserJson(toUserId);
			if (toUserId == null) {
				logger.info("receiver not found in cache " + toUserId);
				throw new ResourceNotFoundException(ErrorCode.User_not_found,
						"receiver not found in cache " + toUserId);
			}

			// Check if receiver has the permission to receive video calls - only required
			// for One2One calls.
			if (!isVideoChatAllowed(toUserJson)) {
				logger.error("video chat not allowed for receiver " + toUserId);
				throw new ForbiddenException(ErrorCode.Action_Not_Supported,
						"video chat not allowed for receiver " + toUserId);
			}
		} else if (Integer.valueOf(iqMessage.getParams().get("chatType"))
				.equals(ChatType.GroupChat.getId().intValue())) {
			Group group = cacheGroupDao.getGroup(toUserId);
			if (group == null) {
				logger.error("Receiver group not found in cache " + toUserId);
				throw new BadRequestException(ErrorCode.InvalidGroup, toUserId);
			}
		} else {
			logger.error("Chat type not support " + iqMessage.getParams().get("chatType"));
			throw new BadRequestException(ErrorCode.Action_Not_Supported);
		}
	}

}
