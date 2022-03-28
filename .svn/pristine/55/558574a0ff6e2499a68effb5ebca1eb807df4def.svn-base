package controllers.aspects;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import controllers.dto.MeetingDto;
import core.daos.CacheGroupDao;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;


import controllers.dto.MemoDto;
import controllers.dto.TrackingEventDto;
import core.entities.Group;
import core.entities.Memo;
import core.entities.User;
import core.exceptions.BadRequestException;
import core.exceptions.ForbiddenException;
import core.exceptions.InternalServerErrorException;
import core.services.CacheService;
import core.services.EventTrackingServiceImpl;
import core.services.MemoService;
import core.services.UserService;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.Enums.ChannelType;
import core.utils.Enums.CustomerPriority;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.FileUploadType;
import core.utils.Enums.OrganizationFlavour;
import core.utils.Enums.TrackingEvent;
import core.utils.Enums.UserCategory;
import core.utils.PropertyUtil;
import core.utils.Enums.MeetingEventType;
import core.utils.Enums.MemoType;
import core.utils.ThreadContext;

@Service
public class ValidatorAspect implements InitializingBean {
	private static final Logger			logger	= LoggerFactory.getLogger(ValidatorAspect.class);

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;
	@Autowired
	private UserService userService;
	@Autowired
	private MemoService memoService;
	@Autowired
	private CacheGroupDao cacheGroupDao;

	@Autowired
	public Environment env;

	private static Matcher matcher = null;

	private static List<String> coloumnNameList = new ArrayList<String>();
	private static final Pattern CONTAINS_HTML_XML_CODE = Pattern.compile(".*?<.*?>.*?</.*?>|.*?<.*?/>.*?|.*?<.*?>.*?");

	public ValidatorAspect() {
		logger.info("ValidatorAspect Bean created");
	}


	@Override
	public void afterPropertiesSet() {
		logger.info("afterPropertiesSet in validation aspect");
		String columnString = env.getProperty(Constants.CUSTOM_MEMO_COLUMN_LIST);
		coloumnNameList = Arrays.asList(columnString.split("\\s*,\\s*"));			
		logger.info("custom memo Coloumn List: "+coloumnNameList);

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
		if (!userService.isAdminUser(currentUserId)) {
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
		if (memoDto.getRecipientIds() != null && !memoDto.getSendToAll()) {
			validateMemoRecipients(memoDto.getRecipientIds(), currentUserId, orgNode);
		}
		logger.debug("Validated memo creation, currentUserId = " + currentUserId);
	}

	public void validateChannelMessage(MemoDto memoDto, Integer channelId) {
		logger.info("validating ChannelMessage for channelId "+channelId);
		Boolean isChannelIdAbsent = Objects.isNull(channelId);
		if (!isChannelIdAbsent && channelId > 0 ) {
			logger.debug(channelId + " is a valid channelId");
		}
		else {
			throw new BadRequestException(ErrorCode.Invalid_Channel_Id,"Invalid channelId:" + (isChannelIdAbsent ? "null" : "ChannelId  should be a Positive Integer"));
		}	
		logger.info("Finding channelDetails for channelId "+channelId +" from cache");
		JsonNode channelNode = cacheService.getChannelJson(channelId);
		if ( Objects.isNull(channelNode)) {
			throw new InternalServerErrorException(ErrorCode.Resource_Not_Found,
					"CouldNot Find the ChannelDetails");
		}
		logger.debug("Found ChannelDetails for ChannelId "+channelId+" from cache");
		Integer channelOrgId = channelNode.findPath("organizationId").asInt();
		Integer currentUserId = ThreadContext.getUserContext().getUser().getId();
		Integer currentUserOrgId = ThreadContext.getUserContext().getUser().getOrganizationId();

		logger.info("Validating Role for User:"+currentUserId+" with currentUserOrgId:" + currentUserOrgId + "and channelOrgId:"
				+ channelOrgId + ", channelId:" + channelId);

		Boolean isAdmin = userService.isAdminUser(currentUserId);
		logger.debug("User has Admin Role?:"+isAdmin);

		if (!isAdmin || !currentUserOrgId.equals(channelOrgId)) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,"Only Admin can use this Api,to send Messages to Channel in his Organization. Current user doesnot have required authorization");
		}
		logger.debug("User is a Admin of channel "+channelId);

		Integer uploadId = memoDto.getUploadId();
		logger.info("Validating uploadId:"+uploadId+" for uploaded ExcelFile:");
		Boolean isUploadIdAbsent = Objects.isNull(uploadId);
		if (!isUploadIdAbsent && uploadId > 0) {
			logger.debug("valid uploadId");
		}
		else {
			throw new BadRequestException(ErrorCode.Invalid_UploadId,"UploadId need to be a positive number,Invalid uploadId");
		}

		logger.info("validating ChannelMessage Sending options");

		Boolean isReceiptentIdsAbsent = Objects.isNull(memoDto.getRecipientIds());
		Boolean sendToAll = memoDto.getSendToAll();
		if (!isUploadIdAbsent) {
			if (isReceiptentIdsAbsent && sendToAll == false) {
				logger.debug("sending ChannelMessage using ExcelFile");
			}
			else {
				throw new BadRequestException(ErrorCode.Invalid_Options, "Invalid Sending Options");
			}
		}
		else if (sendToAll == true) {
			if(isUploadIdAbsent && isReceiptentIdsAbsent) {
				logger.debug("sending ChannelMessage for AllFollowers,sendToAll is true");
			}
			else {
				throw new BadRequestException(ErrorCode.Invalid_Options, "Invalid Sending Options");
			}
		}
		else if(!isReceiptentIdsAbsent) {
			if(isUploadIdAbsent && sendToAll == false) {
				logger.debug("sending ChannelMessage using FollowerIds");
			}
			else {
				throw new BadRequestException(ErrorCode.Invalid_Options, "Invalid Sending Options");
			}
		}
		else {
			logger.debug("Receiptents of the message is not empty");
			throw new BadRequestException(ErrorCode.Invalid_Memo_No_Recipients,"Receiptents of the message cannot be empty");
		}

		logger.debug("ChannelMessage Sending Options Validated");

		logger.info("Validating Length of ChannelMessage Subject and Content");

		Boolean isMessageSubjectAbsent = Objects.isNull(memoDto.getSubject());
		Boolean isMessageContentAbsent = Objects.isNull(memoDto.getText());

		if(isMessageSubjectAbsent || isMessageContentAbsent) {
			throw new BadRequestException(ErrorCode.Invalid_Memo_Subject_Text);
		}
		logger.debug("ChannelMessage subject and Content is present");

		// //trim before checking length
		memoDto.setSubject(memoDto.getSubject().trim());
		Integer maxLen = Integer.parseInt(PropertyUtil.getProperty(Constants.MAX_MESSAGE_SUBJECT_LEN));
		Integer minLen = Integer.parseInt(PropertyUtil.getProperty(Constants.MIN_MESSAGE_SUBJECT_LEN));
		if (memoDto.getSubject().length() <= maxLen && memoDto.getSubject().length() >= minLen) {
			logger.debug("ChannelMessage  subjectLength is in proper limit");
		}
		else {
			throw new BadRequestException(ErrorCode.Invalid_Memo_Subject_Length);
		}

		logger.info("Validating chatUserIds and chatGroupIds");

		List<Integer> chatUserIds = memoDto.getOne2OneChatIds();	
		if (Objects.nonNull(chatUserIds)) {
			for (Integer id : chatUserIds) {
				logger.debug("Get chatUserId "+ id+" OrgId from cache");
				Integer chatOrgId = cacheService.getUserJson(id).get("orgId").asInt();
				logger.debug("ChatUser OrgId:"+chatOrgId);
				if (chatOrgId.equals(currentUserOrgId)) {
					logger.debug("ChatId " + id + " is in same organization as that of Message Creator");
				}
				else {
					throw new BadRequestException(ErrorCode.Invalid_Memo_ChatList, "Id in ChatList is not valid");
				}
			}
		}

		List<Integer> chatGroupIds = memoDto.getGroupChatIds();
		Boolean chatGroupIdsAbsent = Objects.isNull(chatGroupIds);
		if (!chatGroupIdsAbsent) {
			for (Integer id : chatGroupIds) {
				logger.debug("Get groupCreatorId for group "+id+" from cache");
				Integer groupCreatorId = cacheService.getGroupDetails(id).getCreatedById();
				logger.debug("Get groupCreator OrgId,creatorId:"+groupCreatorId+" from cache");
				Integer chatOrgId = cacheService.getUserJson(groupCreatorId).get("orgId").asInt();
				logger.debug("groupCreator OrgId:"+chatOrgId);
				if (chatOrgId.equals(currentUserOrgId)) {
					logger.debug("GroupId " + id + " is in same organization as that of Message Creator");
				}
				else {
					throw new BadRequestException(ErrorCode.Invalid_Memo_ChatList, "Group's OrganizationId & Users organizationId mismatch");
				}
			}
		}		
		// legth : medium text allows 16MB, so no check added
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

	public void validateGetGroupCallWaitTime(Integer groupId, Integer priority) {
		logger.debug("Validating get call wait time by groupId");
		Group group = cacheService.getGroupDetails(groupId);
		if(group == null) {
			throw new BadRequestException(ErrorCode.InvalidGroup, "Group with Id does not exist" );
		}
		CustomerPriority customerPriority = CustomerPriority.getEnum(priority.byteValue());
		if(customerPriority == null) {
			throw new BadRequestException(ErrorCode.Invalid_Enum_Type, "Priority with Id is not valid" );
		}

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

		if (channelId < 0 || channelId == null) {
			throw new BadRequestException(ErrorCode.Invalid_Channel_Id,
					"channel Id should be NotNull and Positive Integer.....Found with requested channelId:" );
		}

		if (offset == null || offset < 0) {
			throw new BadRequestException(ErrorCode.Invalid_Offset,
					"Offset is NotNull and Positive Number");
		}

		if (limit == null || limit < 1) {
			throw new BadRequestException(ErrorCode.Invalid_Limit,
					"Limit is NotNull and Positive Number" );
		}

		User currentUser = ThreadContext.getUserContext().getUser();
		if (currentUser.getId().intValue() != userId) {
			throw new ForbiddenException(ErrorCode.UserMismatch, "Logged in userId & userId provided input is mismatch ");
		}
		logger.info("Validated get memo by userId");
	}

	public void validateGetMemoDetails(Memo memo, Boolean needSummary) {
		logger.debug("Validating get memo details");
		User currentUser = ThreadContext.getUserContext().getUser();

		if (memoService.isReceipient(memo.getId(), currentUser.getId()) == 0) {
			throw new ForbiddenException(ErrorCode.Invalid_Memo_Receipient, "Invalid memo receipient");
		}

		if (needSummary && !(currentUser.getId().equals(memo.getCreatedById()))) {
			throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access,
					"logged in user is not creator of the memo ");
		}
		logger.debug("Validated get memo details");
	}

	public void validateGetMemoDetailsv2(Memo memo, Boolean needSummary) {

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
		// Usercategory should be Consumer
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
		if (!userService.isBroadcaster(currentUser.getId())) {
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
		if(MemoType.CustomMemo.getId().byteValue() == memo.getMemoType()) {
			throw new BadRequestException(ErrorCode.Invalid_Memo_Id,
					"Report download is not allowed for memo type "+MemoType.CustomMemo.name());
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



	private boolean containsHtmlXmlCode(final String inputText) {
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

	public void validateMemoUploadExcelFile(File file, Byte uploadType) {
		logger.info("validate input file started.");
		try {
			InputStream inputStream = new FileInputStream(file.getAbsoluteFile());
			XSSFWorkbook wb = new XSSFWorkbook(inputStream);		
			XSSFSheet sheet = wb.getSheetAt(0);
			XSSFRow row = sheet.getRow(0);
			if(row == null) {
				throw new BadRequestException(ErrorCode.Empty_File, "File should not be empty");
			}
			if(CommonUtil.getCellValue(row.getCell(0)) == null) {
				throw new BadRequestException(ErrorCode.Empty_File, "File should not be empty");
			} 
			if(FileUploadType.CustomMemo.getId().byteValue() == uploadType) {				
				if(!row.getCell(0).getStringCellValue().equalsIgnoreCase(coloumnNameList.get(0)) ||
						!row.getCell(1).getStringCellValue().equalsIgnoreCase(coloumnNameList.get(1)) ||	
						!row.getCell(2).getStringCellValue().equalsIgnoreCase(coloumnNameList.get(2)) ||
						!row.getCell(3).getStringCellValue().equalsIgnoreCase(coloumnNameList.get(3))){
					throw new BadRequestException(ErrorCode.Invalid_Coloumn_Name, "Coloumn name is invalid");
				}
			}
			inputStream.close();
			logger.info("validation done successfuly.");
		} catch (IOException e) {
			throw new BadRequestException(ErrorCode.Parse_Excel_Failed, "read excel failed", e );
		}
	}




	public void validateTrackingEventInput(TrackingEventDto event) {

		TrackingEvent.getEnum(event.getType().byteValue());
		if(event.getGroupId() != null) {
			assertPositiveInteger(event.getGroupId());
		}
		if(event.getUserId() != null) {
			assertPositiveInteger(event.getUserId());
		}
		if(event.getMeetingId() != null) {
			assertPositiveInteger(event.getMeetingId());
		}
		logger.info("Event tracking input validated");
	}


	/**
	 * Checks whether the input Integer/Long/String value is either zero or negative
	 * if it is throws BadRequestException.
	 * 
	 * @param id
	 */
	public static void assertPositiveInteger(Object obj) {
		if (obj == null) {
			throw new BadRequestException(ErrorCode.Invalid_Id, "Null obj.");
		}

		Long id = null;
		if (obj instanceof Integer) {
			id = new Long(((Integer) obj).intValue());
		} else if (obj instanceof Long) {
			id = ((Long) obj).longValue();
		} else {
			try {
				id = Long.parseLong(obj.toString());
			} catch (NumberFormatException ex) {
				throw new BadRequestException(ErrorCode.Invalid_Id, "Input value can not be parsed into an long.");
			}
		}

		if (id == null) {
			throw new BadRequestException(ErrorCode.Invalid_Id, "Null input value.");
		}

		if (id.intValue() < 1) {
			throw new BadRequestException(ErrorCode.Invalid_Id,
					"Entered value has to be a positive Integer/Long value.");
		}
	}
	public void validateCreateMeetingRecording(Integer groupId, Integer currentUserId, Integer toUserId, Boolean isAlwaysCreateNewMeeting) {
		logger.debug("Validating create meeting request");
		Group group = cacheService.getGroupDetails(groupId);
		if(group == null) {
			throw new BadRequestException(ErrorCode.InvalidGroup, "Group with Id does not exist" );
		}
		//Group type validation not added yet
		Boolean isMember = cacheGroupDao.isMemberInGroup(group,currentUserId);
		if(!isMember){
			throw new BadRequestException(ErrorCode.Forbidden, "Loggedin user Id not valid" );
		}
		if(toUserId != null) {
			//Boolean isToUserMember = cacheGroupDao.isMemberInGroup(group, toUserId);
			/*if (!isToUserMember) {
				throw new BadRequestException(ErrorCode.Forbidden, "Loggedin user Id not valid");
			}*/
		}
		logger.debug("Validating is consumer is Authorized to access chat users.");
	/*if (isAlwaysCreateNewMeeting && ThreadContext.getUserContext().getUser().getUserCategory().intValue() != UserCategory.Employee.getId()) {
			
			//	throw new ForbiddenException(ErrorCode.Unauthorized_Api_Access, "Current user is not authorized to access Api");
			
		}*/
	}


		public void validateCreateRecording(MeetingDto meetingDto) {
			if(meetingDto.getRecordingType() == null){
				throw new BadRequestException(Enums.ErrorCode.BadRequest,
						"RecordingType should be present in input.");
			}
			if(meetingDto.getRecordingMethod() == null){
				throw new BadRequestException(Enums.ErrorCode.BadRequest,
						"Recording Method should be present in input.");
			}
			logger.info("recording Type: {}, recording Method: {}",meetingDto.getRecordingType(), meetingDto.getRecordingMethod());
			Enums.RecordingType recordingType = Enums.RecordingType.recordingType(meetingDto.getRecordingType().intValue());
			Enums.RecordingMethod method = Enums.RecordingMethod.getRecordingMethod(meetingDto.getRecordingMethod().byteValue());
		}


}
