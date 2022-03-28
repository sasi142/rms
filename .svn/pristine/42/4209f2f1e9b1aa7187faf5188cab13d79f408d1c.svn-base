package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Singleton;
import com.workapps.common.core.services.DataEncryptionService;
import controllers.actions.UserAuthAction;
import controllers.aspects.ValidatorAspect;
import core.entities.*;
import core.exceptions.BadRequestException;
import core.exceptions.ForbiddenException;
import core.services.CacheService;
import core.services.RecordingService;
import core.services.UserService;
import core.utils.Constants;
import core.utils.Enums.ChatType;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.GroupType;
import core.utils.ThreadContext;
import core.utils.ValidateRequestUtil;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import utils.RmsApplicationContext;

import java.util.List;

@Singleton
public class UsersController extends Controller  {

	private static Logger logger = LoggerFactory.getLogger(UsersController.class);
	
	private Environment		env;

	
	private UserService		userService;

	private CacheService cacheService;

	private ValidatorAspect	validatorAspect;
	
	private ValidateRequestUtil	validateRequestUtil;
	
	private Integer			minSearchStrLength;

	private DataEncryptionService dataEncryptionService;

	private RecordingService recordingService;

	public UsersController() {
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		env = ctx.getBean(Environment.class);
		userService = (UserService) ctx.getBean(Constants.USER_SERVICE_BEAN);
		cacheService = (CacheService) ctx.getBean(Constants.CACHE_SERVICE_SPRING_BEAN);
		recordingService = (RecordingService) ctx.getBean(Constants.RECORDING_SERVICE_BEAN);
		validatorAspect = (ValidatorAspect) ctx.getBean(Constants.VALIDATOR_ASPECT_SPRING_BEAN);
		validateRequestUtil = (ValidateRequestUtil) ctx.getBean(Constants.VALIDATE_REQUEST_UTIL_SPRING_BEAN);
		dataEncryptionService = (DataEncryptionService) ctx.getBean(Constants.DATA_ENCRYPTION_SERVICE_BEAN);
		init();
	}
	//@Override
	public void init() {
		minSearchStrLength = Integer.valueOf(env.getProperty(Constants.MIN_SEARCH_STRING_LENGTH));
		logger.info("minSearchStrLength : " + minSearchStrLength);
	}

	@With(UserAuthAction.class)
	public Result getContacts(Integer Id, Integer offset, Integer limit, String search) {
		logger.info("get contact list for id " + Id + "&offset: " + offset + "&limit:" + limit + "&search:" + search);
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		if (!Id.equals(userContext.getUser().getId())) {
			logger.info("user id mismatch: " + Id + "&" + userContext.getUser().getId());
			throw new ForbiddenException(ErrorCode.UserMismatch, Id, userContext.getUser().getId());
		}

		List<Contact> contactList = null;
		if (search != null && !"".equalsIgnoreCase(search)) {
			if (search.length() < minSearchStrLength) {
				throw new BadRequestException(ErrorCode.MinLength_Validation_Failed, search, minSearchStrLength);
			}
		}
		contactList = userService.getContacts(userContext, offset, limit, search);
		logger.info("got contact list for id " + Id + ". Returned results " + (contactList == null ? null : contactList.size()));
		JsonNode contactsJson = Json.toJson(contactList);
		// logger.debug("contact list for id : " + Id + " as json : " + contactsJson);
		return ok(contactsJson);
	}

	@With(UserAuthAction.class)
	public Result getContactsV2(Integer Id, Integer offset, Integer limit, String search) {
		Long time1 = System.currentTimeMillis();
		logger.info("get contact list for id " + Id + "&offset: " + offset + "&limit:" + limit + "&search:" + search);
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		if (!Id.equals(userContext.getUser().getId())) {
			logger.info("user id mismatch: " + Id + "&" + userContext.getUser().getId());
			throw new ForbiddenException(ErrorCode.UserMismatch, Id, userContext.getUser().getId());
		}

		List<Contact> contactList = null;
		if (search != null && !"".equalsIgnoreCase(search)) {
			if (search.length() < minSearchStrLength ) {
				throw new BadRequestException(ErrorCode.MinLength_Validation_Failed, search, minSearchStrLength);
			}
			if (validateRequestUtil.containsHtmlXmlCode(search)) {
				throw new BadRequestException(ErrorCode.Invalid_Data, "html char not allowed");
			}

		} else {// if not searching then make the search value as empty string, do not set as null
			search = "";
		}
		contactList = userService.getContactsV2(userContext, offset, limit, search, null, null);
		logger.info("got contact list for id " + Id + ". Returned results " + (contactList == null ? null : contactList.size()));
		JsonNode contactsJson = Json.toJson(contactList);
		// logger.debug("contact list for id " + Id + " as json : " + contactsJson);
		logger.info("returning contact list for id " + Id + " in " + (System.currentTimeMillis() - time1) + " milliseconds");
		return ok(contactsJson);
	}

	@With(UserAuthAction.class)
	public Result getChatContact(Integer Id, Integer contactId, Integer inputChatType) {
		logger.info("get chat contact list for id " + Id + "&contcatId: " + contactId + "&chatType:" + inputChatType);
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		if (!Id.equals(userContext.getUser().getId())) {
			logger.info("user id mismatch: " + Id + "&" + userContext.getUser().getId());
			throw new ForbiddenException(ErrorCode.UserMismatch, Id, userContext.getUser().getId());
		}
		validatorAspect.validateGetChatContact(Id, contactId, inputChatType);
		List<Contact> contactList = userService.getContactsV2(userContext, null, null, null, contactId, inputChatType);
		logger.info("got chat contact list for id " + Id + ". Returned results " + (contactList == null ? null : contactList.size()));
		Contact contact = null;
		if (contactList != null && contactList.size() > 0) {
			contact = contactList.get(0);
		}
		JsonNode contactJson = Json.toJson(contact);
		// logger.info("returned chat contact for contactId " + contactId + " and chattype = " + inputChatType + " as " + contactJson);
		return ok(contactJson);
	}

	@With(UserAuthAction.class)
	public Result getChatContactV2(Integer Id, Integer contactId, Integer inputChatType) {
		logger.info("get chat contact v2 for id " + Id + "&contcatId: " + contactId + "&chatType:" + inputChatType);
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		if (!Id.equals(userContext.getUser().getId())) {
			logger.info("user id mismatch: " + Id + "&" + userContext.getUser().getId());
			throw new ForbiddenException(ErrorCode.UserMismatch, Id, userContext.getUser().getId());
		}
		validatorAspect.validateGetChatContact(Id, contactId, inputChatType);
		ChatContact contact = userService.getChatContactDetail(userContext, contactId, inputChatType);
		if (ChatType.GroupChat.getId() == inputChatType.byteValue() && GroupType.VideoKycGuestGroupChat.getId() == contact.getGroupType().intValue()) {
			Group group = cacheService.getGroupDetails(contactId);
			if (group.getDataEncryptionKeyId() != null) {
				logger.info("Contact name [{}] is encrypted using [{}] for Contact Id [{}]", contact.getName(), group.getDataEncryptionKeyId(), contactId);
				String updatedName = dataEncryptionService.decrypt(group.getDataEncryptionKeyId(), contact.getName());
				logger.info("Contact name decrypted as [{}] encrypted using [{}] for Contact Id [{}]", updatedName, group.getDataEncryptionKeyId(), contactId);
				contact.setName(updatedName);
			}
			//get the meeting and recording id and return with it.
			logger.info("Group contact Id {} is a VKYC GroupChat. Will get recording and meeting id details", contactId);
			final List<Recording> recordings = recordingService.getRecordingsByGroupId(contactId);
			logger.info("Recording count {} for group id {}", recordings.size(), contactId);
			if (!recordings.isEmpty()){
				final Recording firstRecording = recordings.get(0);
				contact.setMeetingId(firstRecording.getMeetingId());
				contact.setRecordingId(firstRecording.getId());
				logger.info("Meeting Id {}, Recoding Id {}", firstRecording.getMeetingId(), firstRecording.getId());
				if (recordings.size() > 1){
					logger.warn("There are multiple active recordings present for this group");
				}
			}
		}
		logger.debug("got getChatContactDetail for id " + Id + ". Returned results " + contact + ", contactType = " + contact.getContactType());
		JsonNode contactsJson = Json.toJson(contact);
		logger.debug("return contact for contactId " + contactId + " and chattype = " + inputChatType + " as " + contactsJson);
		return ok(contactsJson);
	}

	@With(UserAuthAction.class)
	public Result getUser(Integer Id) {
		logger.info("get user info for id " + Id);
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		Contact user = userService.getUserInfo(userContext.getUser().getId(), Id);
		logger.debug("got Contact (UserInfo) for id " + Id + ". Returned result " + user);
		JsonNode userJson = Json.toJson(user);
		logger.debug("user info for id " + Id + " is " + userJson.toString());
		return ok(userJson);
	}
}