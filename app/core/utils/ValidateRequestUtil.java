package core.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import controllers.dto.MemoDto;
import core.entities.Memo;
import core.entities.User;
import core.exceptions.BadRequestException;
import core.exceptions.ForbiddenException;
import core.exceptions.InternalServerErrorException;
import core.services.CacheService;
import core.services.MemoService;
import core.services.UserService;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.Enums.ChannelType;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.OrganizationFlavour;
import core.utils.Enums.UserCategory;
import core.utils.PropertyUtil;
import core.utils.ThreadContext;
import core.validator.ValidatorsUtil;

@Component
public class ValidateRequestUtil {
	final static Logger logger = LoggerFactory.getLogger(ValidateRequestUtil.class);
	
	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;
	@Autowired
	private UserService userService;
	@Autowired
	private MemoService memoService;

	private static Matcher matcher = null;
	private static final Pattern CONTAINS_HTML_XML_CODE = Pattern.compile(".*?<.*?>.*?</.*?>|.*?<.*?/>.*?|.*?<.*?>.*?");

	public ValidateRequestUtil() {
		logger.info("ValidateRequestUtil Bean created");
	}

	private Boolean isMemoCreationAllowed(JsonNode orgNode) {
		logger.info("check if user allowed to create memo:");
		Boolean isAllowed = true;
		if(orgNode != null) {
			JsonNode orgPrefs = orgNode.findPath("settings");
			if (orgPrefs != null && orgPrefs.isArray()) {
				for (JsonNode pref : orgPrefs) {
					if ("EnableMemo".equalsIgnoreCase(pref.findPath("preference").asText())) {
						String flag = pref.findPath("value").asText();
						if(flag != null && !flag.isEmpty()) {
							isAllowed = Boolean.valueOf(flag);
						}
						break;
					}
				}
			}			
		}
		logger.info("is user allowed to create memo:"+isAllowed);
		return isAllowed;
	}
	
	public void validateCreateMemo(MemoDto memoDto) {
		/*
		 * MEMO 2.0 changes 1. memo can be sent by any user 2. User with Broadcast role
		 * can send memos to all employees or to users falling in selected criteria of
		 * office/group/citydepartment/designation etc 3. User Role or Admin role can't
		 * send memo to all employee, no criteria selection. 4. User Role or Admin role
		 * can send memo to limited number of employees (count provided in org_pref) 5.
		 * Memo will have HTML content 6. Memo will have attachments 7. Memo can be sent
		 * to employees and to contacts(co-workers) also
		 */
		
	   Integer currentUserId = ThreadContext.getUserContext().getUser().getId();
	   Integer orgId = ThreadContext.getUserContext().getUser().getOrganizationId();
		
		JsonNode orgNode = cacheService.getOrgJson(orgId);
		logger.debug("Validating memo creation, currentUserId = " + currentUserId);
		
		if(!isMemoCreationAllowed(orgNode)) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "currentOrganization does not authorise access to this API");
		}
		
		if(memoDto.getSendToAllPartner() && memoDto.getSendToAll()) {
			throw new BadRequestException(ErrorCode.Invalid_Data, "Send to all partner & send to all employee flag should not enable in same api");		
		}
		
	/*	if(memoDto.getShowUserDetailOnSCP() && !memoDto.getIsPublic) {
			throw new BadRequestException(ErrorCode.Invalid_Data, "memo is not public so, show user details on scp cant mark true");		
		}*/
		
		if(memoDto.getSendToAllPartner() || memoDto.getSendToAllEmployeeAndPartner()) {
			logger.info("sned to all Partners validation started for" + currentUserId);
			if(!userService.isBroadcaster(currentUserId)) {
				logger.info("user is not broadcaster, id is" + currentUserId);
				throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "currentUser is not authorised to access API");
			}	
			Boolean isPartnerEmployeeAssociationznNnll = orgNode.findPath("partnerEmployeeAssociationId").isNull();
			logger.info("isPartnerEmployeeAssociationznNnll : " + isPartnerEmployeeAssociationznNnll);
			if(isPartnerEmployeeAssociationznNnll)				
				throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "currentUser is not authorised to access API, Partner org mapping is missing");
			}		
		  
		if (!userService.isBroadcaster(currentUserId)) {
			JsonNode userNode = cacheService.getUserJson(currentUserId);
			String feature = getProductFeature(orgNode, userNode);
			if (memoDto.getIsPublic() || memoDto.getSendToAll() || !"ChatOnly".equalsIgnoreCase(feature)) {
				throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "currentUser is not authorised to access API");
			}
			int limit = getMemoRecipientLimit(ThreadContext.getUserContext().getUser().getOrganizationId(), orgNode);
			int recipientCount = 0;
			if (memoDto.getRecipientIds() == null || memoDto.getRecipientIds().size() == 0) {
				recipientCount = 1;
			} else if (memoDto.getRecipientIds().contains(currentUserId)) {
				recipientCount = memoDto.getRecipientIds().size() - 1;
			}
			
			if ((memoDto.getAdGroupIds() != null && memoDto.getAdGroupIds().size() > 0)
					|| ((limit > 0) && (recipientCount + 1) > limit)) {
				throw new BadRequestException(ErrorCode.Invalid_Memo_Receipient, "Invalid Memo Recipients");
			}
		}

		if (memoDto.getSubject() == null || memoDto.getSubject().isEmpty() || memoDto.getText() == null
				|| memoDto.getText().isEmpty()) {
			throw new BadRequestException(ErrorCode.Invalid_Memo_Subject_Text);
		}

		// //trim before checking length
		memoDto.setSubject(memoDto.getSubject().trim());
		if (memoDto.getSubject().length() > 128) {
			throw new BadRequestException(ErrorCode.Invalid_Memo_Subject_Max_Length);
		}

		// legth : medium text allows 16MB, so no check added
		memoDto.setText(memoDto.getText().trim());

		// Allow memo to send subject with special characters
		/*
		 * if (containsHtmlXmlCode(memoDto.getSubject()) ||
		 * !isAsciiPrintable(memoDto.getSubject())) { throw new
		 * BadRequestException(ErrorCode.Invalid_Memo_Subject_Content); }
		 */

		// Checking of other details like, offices,departments, groups will be handled
		// by procedure
		if(memoDto.getMemoDumpAttachmentId() != null) {
		if (memoDto.getRecipientIds() != null && !memoDto.getSendToAll() && !memoDto.getSendToAllPartner()) {
			validateMemoRecipients(memoDto.getRecipientIds(), currentUserId, orgNode);
		}
		}
		logger.debug("Validated memo creation, currentUserId = " + currentUserId);
	}

	
	public void validateCreateUserMemo(MemoDto memoDto) {
		if(memoDto.getMemoDumpAttachmentId() == null) {
			throw new BadRequestException(ErrorCode.Invalid_Memo_Dump_AttachmentId, "MemoDump attachment Id should not be null");
		}				
	       validateBulkMemoCreateAccess();

		if (memoDto.getSubject() == null || memoDto.getSubject().isEmpty() || memoDto.getText() == null
				|| memoDto.getText().isEmpty()) {
			throw new BadRequestException(ErrorCode.Invalid_Memo_Subject_Text);
		}

		// //trim before checking length
		memoDto.setSubject(memoDto.getSubject().trim());
		if (memoDto.getSubject().length() > 128) {
			throw new BadRequestException(ErrorCode.Invalid_Memo_Subject_Max_Length);
		}
		// legth : medium text allows 16MB, so no check added
		memoDto.setText(memoDto.getText().trim());	
		logger.debug("Validated userMemo creation");
	}

	public void validateBulkMemoCreateAccess() {
		Integer currentUserId = ThreadContext.getUserContext().getUser().getId();
		Integer orgId = ThreadContext.getUserContext().getUser().getOrganizationId();
		JsonNode orgNode = cacheService.getOrgJson(orgId);
		logger.debug("Validating memo creation, currentUserId = " + currentUserId);
		
		if(!isMemoCreationAllowed(orgNode)) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "currentOrganization does not authorise access to this API");
		}		
		if (!userService.isBroadcaster(currentUserId)) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "current User role does not authorise access to this API");						
		}
		
	}
	
	public void validateChannelMessage(MemoDto memoDto, Integer channelId) {
		logger.info("validating ChannelMessage for channelId " + channelId);
		Integer currentUserOrgId = ThreadContext.getUserContext().getUser().getOrganizationId();
		JsonNode orgNode = cacheService.getOrgJson(currentUserOrgId);
		if(!isMemoCreationAllowed(orgNode)) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "currentOrganization does not authorise access to this API");
		}

		memoDto.setChannelId(channelId);
		ValidatorsUtil.validate("channelMessage", memoDto);
		
		Integer currentUserId = ThreadContext.getUserContext().getUser().getId();
		validateRoleForSendingChannelMessage(channelId, currentUserOrgId, currentUserId);
		validateChannelMessageSendingOptions(memoDto);
		validateChannelMessageChatUsers(memoDto, currentUserOrgId);
		memoDto.setText(memoDto.getText().trim());
		
		logger.info("Validated memo creation with currentUserId = " + currentUserId);
	}
	

	public void validateGetMemosByOrgId(Integer orgId) {
		logger.debug("Validating get memo by orgId");
		User currentUser = ThreadContext.getUserContext().getUser();
		if (orgId != null && currentUser.getOrganizationId().intValue() != orgId) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "Cusrrent User is not authorized to get Memo");
		}
		if (!userService.isAdminUser(currentUser.getId())) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "Cusrrent User is not authorized to access API");
		}
		logger.debug("Validated get memo by orgId");
	}

	public void validateChatUsersByMemoId(Integer memoId) {
		// TODO::make first and last logger info...keep rest as debug
		logger.debug("Validating get memo by orgId");
		User currentUser = ThreadContext.getUserContext().getUser();
		Memo memo = memoService.getMemoDetailsV2(memoId, false);
		JsonNode channelNode = cacheService.getChannelJson(memo.getChannelId());
		logger.info("channelType is:" + channelNode);
		Integer channelType = channelNode.findPath("channelType").asInt();
		logger.info("channelType is:" + channelType);

		logger.debug("Validating is consumer is Authorized to access chat users.");
		if (currentUser.getUserCategory().intValue() == UserCategory.Employee.getId()) {
			if (!userService.isAdminUser(currentUser.getId())) {
				throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "Current user is not authorized to access Api");
			} else if (currentUser.getOrganizationId().intValue() != memo.getOrganizationId()) {
				throw new ForbiddenException(ErrorCode.User_Org_Mismatch, "User & memo's organization are mismatched");
			}
		}
		logger.debug("Validate is consumer is Authorized to access chat users.");
		if (currentUser.getUserCategory().intValue() == UserCategory.Customer.getId()) {
			//TODO::use ChannelType.Private instead ChannelType.Private == 2
			if (memoService.isReceipient(memo.getId(), currentUser.getId()) == 0 && channelType == 2) {
				throw new ForbiddenException(ErrorCode.Invalid_Memo_Receipient, "Invalid memo receipient");
			}
		}
		logger.debug("Validating is Chat enable.");

		if (memo.getIsChatEnabled() != true) {
			throw new ForbiddenException(ErrorCode.Invalid_Channel_IsChatEnable, "Chat with channel is not allowed");
		}
		logger.debug("Validation done successfully");
	}

	public void validateGetMemosByUserId(Integer userId,Integer channelId,Integer offset,Integer limit) {
		logger.info("Validating get memo by userId and channelId");
		ValidatorsUtil.assertNonNegativeInteger(channelId);
		ValidatorsUtil.assertNonNegativeInteger(offset);
		ValidatorsUtil.assertPositiveInteger(limit);
		User currentUser = ThreadContext.getUserContext().getUser();
		if (currentUser.getId().intValue() != userId) {
			throw new ForbiddenException(ErrorCode.UserMismatch, "Logged in userId & userId provided input is mismatch ");
		}
		logger.info("Validated get memo by userId");
	}

	public void validateGetMemoDetails(Memo memo, Boolean needSummary) {
		logger.debug("Validating get memo details");
		User currentUser = ThreadContext.getUserContext().getUser();
		if (currentUser.getOrganizationId().intValue() != memo.getOrganizationId()) {
			throw new ForbiddenException(ErrorCode.User_Org_Mismatch, "User & memo's organization are mismatched");
		}
		if (memoService.isReceipient(memo.getId(), currentUser.getId()) == 0) {
			throw new ForbiddenException(ErrorCode.Invalid_Memo_Receipient, "Invalid memo receipient");
		}

		if (needSummary && !(currentUser.getId().equals(memo.getCreatedById()))) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
					"logged in user is not creator of the memo ");
		}
		logger.debug("Validated get memo details");
	}

	public void validateGetMemoDetailsV2(Memo memo, Boolean needSummary) {

		logger.info("Validating get ChannelMessage details");
		User currentUser = ThreadContext.getUserContext().getUser();
		JsonNode channelNode = cacheService.getChannelJson(memo.getChannelId());
		Integer channelType = channelNode.findPath("channelType").asInt();

		logger.debug("channelType is:" + channelType);

		if (currentUser.getUserCategory().intValue() == UserCategory.Employee.getId()) {
			logger.debug("user is an employee");
			if (memoService.isReceipient(memo.getId(), currentUser.getId()) == 0) {
				throw new ForbiddenException(ErrorCode.Invalid_Memo_Receipient, "Invalid memo receipient");
			}
			logger.debug("employee is a valid receiptent");

			if (needSummary && !(currentUser.getId().equals(memo.getCreatedById()))) {
				throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
						"logged in user is not creator of the memo ");
			}
			logger.debug("employee has admin role");

		}
		else if (currentUser.getUserCategory().intValue() == UserCategory.Consumer.getId()) {
			logger.debug("user is a consumer");
			if (memoService.isReceipient(memo.getId(), currentUser.getId()) == 0
					&& channelType == ChannelType.Private.getId()) {
				throw new ForbiddenException(ErrorCode.Invalid_Memo_Receipient, "Invalid memo receipient");
			}
			logger.debug("consumer is a valid receiptent");
		}
		logger.info("Validated get memo details");
	}

	public void validateChangeReadStatus(Integer memoId, Integer userId) {
		logger.debug("Validating memo read status change");
		User currentUser = ThreadContext.getUserContext().getUser();
		if (currentUser.getId().intValue() != userId) {
			throw new ForbiddenException(ErrorCode.UserMismatch, userId, currentUser.getId());
		}
		if (memoService.isReceipient(memoId, currentUser.getId()) == 0) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
					"user is not a recipient of the memo ");
		}
		logger.debug("Validated memo read status change");
	}

	public void validateUpdateMemoPublicState(Memo memo) {
		logger.debug("Validating memo public state change");
		User currentUser = ThreadContext.getUserContext().getUser();
		if (!userService.isAdminUser(currentUser.getId())) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "currentUser is not Admin");
		}
		if (currentUser.getId().intValue() != memo.getCreatedById()) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
					"current user is not creator of the memo ");
		}
		logger.debug("Validated memo public state change");
	}

	public void validateGetMemoDetailsDownloaded(Memo memo) {
		logger.debug("Validating memo detail download");
		User currentUser = ThreadContext.getUserContext().getUser();
		if (!(currentUser.getId().equals(memo.getCreatedById()))) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
					"current user is not creator of the memo ");
		}
		logger.debug("Validated memo detail download");
	}

	/**
	 * @param userId
	 */
	public void validateGetMemoCountByStatus(Integer userId) {
		logger.debug("Validating get memo count");
		User currentUser = ThreadContext.getUserContext().getUser();
		if (currentUser.getId().intValue() != userId) {
			throw new ForbiddenException(ErrorCode.UserMismatch, "logged in user & userId provided in input are different");
		}
		logger.debug("Validated get memo count");
	}

	public void validateGetChatContact(Integer userId, Integer contactId, Integer inputChatType) {
		logger.debug("Validating GetChatContact: for id " + userId + "&contcatId: " + contactId
				+ "&chatType:" + inputChatType);
		if (Enums.ChatType.One2One.getId().byteValue() == inputChatType.byteValue()) {
			User currentUser = ThreadContext.getUserContext().getUser();
			User contact = cacheService.getUser(contactId, false);
			if (((UserCategory.Customer.getId().byteValue() == currentUser.getUserCategory().byteValue())
					&& (UserCategory.Customer.getId().byteValue() == contact.getUserCategory().byteValue()))
					|| ((UserCategory.Guest.getId().byteValue() == currentUser.getUserCategory().byteValue())
							&& (UserCategory.Guest.getId().byteValue() == contact.getUserCategory().byteValue()))) {
				throw new ForbiddenException(ErrorCode.NotInContact, "userId is not in contact with contactId");
			}
		}
		logger.debug("Validated GetChatContact:  for id " + userId + "&contcatId: " + contactId
				+ "&chatType:" + inputChatType);
	}

	public boolean containsHtmlXmlCode(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			matcher = CONTAINS_HTML_XML_CODE.matcher(inputText);
			return matcher.matches();
		} else {
			return false;
		}
	}

	private boolean isAsciiPrintable(final String inputText) {
		if (inputText != null && !inputText.trim().isEmpty()) {
			return StringUtils.isAsciiPrintable(inputText);
		} else {
			return false;
		}
	}

	// TODO: This might give performance hit in case of Customer Organization and
	// sending memo to all Org Contacts
	private void validateMemoRecipients(List<Long> recipients, Integer currentUserId, JsonNode orgNode) {
		List<Integer> recipientIds = new ArrayList<Integer>();
		for (Long id : recipients) {
			recipientIds.add(id.intValue());
		}
		if (OrganizationFlavour.Employee.getId().byteValue() == Byte
				.valueOf(orgNode.findPath("organizationFlavour").asText()).byteValue()) {
			if (!cacheService.isInContact(currentUserId, recipientIds)) {
				throw new BadRequestException(ErrorCode.NotInContact, "recipient is not in contact with current user");
			}
		} else {
			List<Integer> nonRecipientIds = new ArrayList<Integer>();
			for (Integer recipientId : recipientIds) {
				User recipient = cacheService.getUser(recipientId, false);
				if (!(recipient.getActive() && ThreadContext.getUserContext().getUser().getOrganizationId()
						.equals(recipient.getOrganizationId()))) {
					nonRecipientIds.add(recipientId);
				}
			}
			if (!nonRecipientIds.isEmpty()) {
				throw new BadRequestException(ErrorCode.NotInContact, "recipient is not in contact with current user");
			}
		}
	}

	private int getMemoRecipientLimit(Integer orgId, JsonNode orgNode) {
		logger.debug("Get MemoRecipientLimit for org  " + orgId);
		JsonNode prefs = orgNode.findPath("settings");
		int limit = -1;
		if (prefs != null && prefs.isArray()) {
			for (JsonNode pref : prefs) {
				if (Constants.MEMO_RECIPIENT_LIMIT.equalsIgnoreCase(pref.findPath("preference").asText())) {
					limit = pref.findPath("value").asInt();
					break;
				}
			}
		}
		logger.debug("Got MemoRecipientLimit " + limit + " for org  " + orgId);
		return limit;
	}

	private String getProductFeature(JsonNode orgNode, JsonNode userNode) {
		String feature = "All";
		JsonNode userPrefs = userNode.findPath("userPreferences");
		if (userPrefs != null && userPrefs.isArray()) {
			for (JsonNode pref : userPrefs) {
				if ("Features".equalsIgnoreCase(pref.findPath("name").asText())) {
					feature = pref.findPath("value").asText();
					return feature;
				}
			}
		}
		JsonNode orgPrefs = orgNode.findPath("settings");
		if (orgPrefs != null && orgPrefs.isArray()) {
			for (JsonNode pref : orgPrefs) {
				if ("Features".equalsIgnoreCase(pref.findPath("preference").asText())) {
					feature = pref.findPath("value").asText();
					break;
				}
			}
		}
		return feature;
	}
	
	private void validateRoleForSendingChannelMessage(Integer channelId, Integer currentUserOrgId,
			Integer currentUserId) {
		logger.debug("validating userRole for sending ChannelMessage");
		logger.info("Finding channelDetails for channelId " + channelId + " from cache");
		JsonNode channelNode = cacheService.getChannelJson(channelId);
		if (Objects.isNull(channelNode)) {
			throw new InternalServerErrorException(ErrorCode.Resource_Not_Found, "CouldNot Find the ChannelDetails");
		}
		logger.debug("Found ChannelDetails for ChannelId " + channelId + " from cache");
		Integer channelOrgId = channelNode.findPath("organizationId").asInt();
		logger.info("currentUserId:" + currentUserId + " with currentUserOrgId:" + currentUserOrgId
				+ "and channelOrgId:" + channelOrgId + ", channelId:" + channelId);

		Boolean isAdmin = userService.isAdminUser(currentUserId);
		logger.debug("User has Admin Role?:" + isAdmin);
		if (!isAdmin || !currentUserOrgId.equals(channelOrgId)) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
					"you can't send message");
		}
		logger.debug("User is a Admin of channel with channelId:" + channelId + " and can create/send channelMessage");
	}

	private void validateChannelMessageSendingOptions(MemoDto memoDto) {
		Integer uploadId = memoDto.getUploadId();
		Boolean isUploadIdPresent = Objects.nonNull(uploadId);
		Boolean isReceiptentIdsPresent = Objects.nonNull(memoDto.getRecipientIds());
		Boolean sendToAll = memoDto.getSendToAll();
		if ((sendToAll && isUploadIdPresent && isReceiptentIdsPresent) ||
				!sendToAll && !isUploadIdPresent && !isReceiptentIdsPresent ||
				sendToAll && isUploadIdPresent) {
			throw new BadRequestException(ErrorCode.Invalid_Options, "both sendToAll and upload not accepted");
		}
		logger.info("validated ChannelMessage Sending options");
	}

	private void validateChannelMessageChatUsers(MemoDto memoDto, Integer currentUserOrgId) {
		logger.info("validating ChannelMessage ChatUsers");
		List<Integer> chatUserIds = memoDto.getOne2OneChatIds();
		if (Objects.nonNull(chatUserIds) && chatUserIds.size() > 0) {
			for (Integer id : chatUserIds) {
				logger.debug("MessageCreatorOrgId:" + currentUserOrgId + " ,chatUserId:" + id);
				Boolean isInSameOrg = cacheService.isInOrgContact(currentUserOrgId, id);
				logger.debug("isInSameOrg=" + isInSameOrg);
				if (!isInSameOrg) {
					throw new BadRequestException(ErrorCode.Invalid_Memo_ChatList, "Id in ChatList is not valid");
				}
			}
		}

		List<Integer> chatGroupIds = memoDto.getGroupChatIds();
		if (Objects.nonNull(chatGroupIds) && chatGroupIds.size() > 0) {
			for (Integer id : chatGroupIds) {
				logger.debug("Get groupCreatorId for group " + id + " from cache");
				Integer groupCreatorId = cacheService.getGroupDetails(id).getCreatedById();
				logger.debug("MessageCreatorOrgId:" + currentUserOrgId + " ,groupCreatorId:" + groupCreatorId
						+ "for groupId:" + id);
				Boolean isInSameOrg = cacheService.isInOrgContact(currentUserOrgId, groupCreatorId);
				logger.debug("isInSameOrg=" + isInSameOrg);
				if (!isInSameOrg) {
					throw new BadRequestException(ErrorCode.Invalid_Memo_ChatList,
							"Group's OrganizationId & Users organizationId mismatch");
				}
			}
		}
		logger.info("validated ChannelMessage ChatUsers done!!!");
	}

	
	
	
}
