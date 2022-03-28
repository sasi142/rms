package controllers;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.stream.javadsl.Flow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.workapps.common.core.services.DataEncryptionService;
import controllers.actions.ApiAuthAction;
import controllers.actions.UserAuthAction;
import core.akka.actors.RmsActorSystem;
import core.akka.actors.UserConnectionActor;
import core.entities.*;
import core.exceptions.ApplicationException;
import core.exceptions.ForbiddenException;
import core.exceptions.UnAuthorizedException;
import core.services.*;
import core.utils.*;
import core.utils.Enums.ChatMessageType;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.GroupType;
import core.utils.Enums.UserCategory;
import messages.ConnId;
import messages.UserConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import play.libs.F.Either;
import play.libs.Json;
import play.mvc.*;
import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;
import play.mvc.Http.Status;
import utils.RmsApplicationContext;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class ChatController extends Controller {

	private static Logger logger = LoggerFactory.getLogger(ChatController.class);

	private AuthUtil					authUtil;

	private ChatHistoryService			chatHistoryService;

	private UserConnectionService			userConnectionService;

	private Environment					env;

	private CommonUtil					commonUtil;

	private CacheService				cacheService;

	private ChatMessageService			chatMessageService;

	private GroupChatMessageService groupChatMessageService;	

	private AccessCheckUtil accessCheckUtil;

	private String						methods;
	private String						maxAge;
	private String						headers;
	private Boolean						enableCorsHeaders;
	private Boolean enableXsrfCheck;
	private List<String> originList = new ArrayList<>();

	private final Duration time = Duration.of(10, ChronoUnit.SECONDS);

	private DataEncryptionService dataEncryptionService;



	public ChatController() {
		logger.info("Before Context: ");
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		logger.info("Application Context: " + ctx);
		commonUtil = (CommonUtil) ctx.getBean(Constants.COMMON_UTIL_SPRING_BEAN);
		logger.info("commonUtil: " + commonUtil);
		env = ctx.getBean(Environment.class);
		authUtil = (AuthUtil) ctx.getBean(Constants.AUTH_UTIL_SPRING_BEAN);
		chatHistoryService = (ChatHistoryService) ctx.getBean(Constants.CHAT_HISTORY_SPRING_BEAN);
		userConnectionService = (UserConnectionService) ctx.getBean(Constants.USER_CONNECTION_SERVICE_SPRING_BEAN);
		chatMessageService = (ChatMessageService) ctx.getBean(Constants.CHAT_MESSAGE_SERVICE_SPRING_BEAN);
		groupChatMessageService = (GroupChatMessageService) ctx.getBean(Constants.GROUP_CHAT_MESSAGE_SERVICE_SPRING_BEAN);
		accessCheckUtil = ctx.getBean(AccessCheckUtil.class);
		cacheService = (CacheService) ctx.getBean(Constants.CACHE_SERVICE_SPRING_BEAN);
		dataEncryptionService = (DataEncryptionService) ctx.getBean(Constants.DATA_ENCRYPTION_SERVICE_BEAN);
		init();
	}

	//@Override
	public void init() {
		String originText = env.getProperty(Constants.ACCESS_CONTROL_ALLOW_ORIGIN);

		if (originText != null) {
			String str[] = originText.split(",");
			if (str != null && str.length > 0) {
				for (String origin: str) {
					originList.add(origin);
				}
			}
		}

		methods = env.getProperty(Constants.ACCESS_CONTROL_ALLOW_METHODS);
		maxAge = env.getProperty(Constants.ACCESS_CONTROL_MAX_AGE);
		headers = env.getProperty(Constants.ACCESS_CONTROL_ALLOW_HEADERS);
		enableCorsHeaders = Boolean.valueOf(env.getProperty(Constants.ENABLE_SET_CORS_HEADERS));
		logger.info("allow cors origin: " + originList);
		logger.info("allow cords methods: " + methods);
		logger.info("allow cors max age: " + maxAge);
		logger.info("allow.cors.headers: " + headers);
		logger.info("enable.cors.headers: " + enableCorsHeaders);		

		enableXsrfCheck = Boolean.valueOf((PropertyUtil.getProperty(Constants.ENABLE_XSRF_CHECK)).trim());
	}

	@With(UserAuthAction.class)
	public Result getOne2OneChatHistory(Integer to, Boolean mergeChats, final Integer offset, Integer limit) {
		logger.info("Get One2One chat history - to: " + to + ",offset: " + offset + ",limit: " + limit);
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		List<One2OneChat> one2OneHistory = chatHistoryService.getOne2OneChatHistory(userContext, to, 9999999999999L, offset, limit);
		logger.info("getOne2OneChatHistory returned " + (one2OneHistory == null ? null : one2OneHistory.size()) + " records");

		JsonNode one2OneJson = null;
		if (mergeChats) {// message Grouping needed
			List<MergedOne2OneChat> mergedChatHistory = processChatHistory(userContext, one2OneHistory);
			logger
			.debug("processedChatHistory to get MergedOne2OneChat" + (mergedChatHistory == null ? null : mergedChatHistory.size()) + " records");
			one2OneJson = Json.toJson(mergedChatHistory);
		} else {
			logger.debug("No one2one chat mering needed ");
			one2OneJson = Json.toJson(one2OneHistory);
		}
		logger.info("returned One2One chat history ");
		return ok(one2OneJson);
	}

	/**
	 * API used by Mobile Applications
	 * 
	 * @param to
	 * @param lastMsgDate
	 * @param offset
	 * @param limit
	 * @return
	 */
	@With(UserAuthAction.class)
	public Result getOne2OneChatHistoryV2(Integer to, Long lastMsgDate, final Integer offset, Integer limit) {
		logger.info("Get One2One chat history v2 - to: " + to + ",offset: " + offset + ",limit: " + limit);
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		List<One2OneChat> one2OneHistory = chatHistoryService.getOne2OneChatHistory(userContext, to, lastMsgDate, offset, limit);
		logger.info("getOne2OneChatHistory returned " + (one2OneHistory == null ? null : one2OneHistory.size()) + " records");
		List<MergedOne2OneChat> mergedChatHistory = processChatHistoryBySender(one2OneHistory);
		logger
		.debug("processChatHistoryBySender to get MergedOne2OneChat" + (mergedChatHistory == null ? null : mergedChatHistory.size()) + " records");
		JsonNode one2OneJson = Json.toJson(mergedChatHistory);
		logger.info("returned One2One chat history v2");
		return ok(one2OneJson);
	}

	/**
	 * API used by WEB Application
	 * 
	 * @param to
	 * @param mergeChats
	 * @param offset
	 * @param limit
	 * @return
	 */
	@With(UserAuthAction.class)
	public Result getGroupChatHistory(Integer to, Boolean mergeChats, final Integer offset, Integer limit) {
		logger.info("Get Group chat history - to: " + to + ",offset: " + offset + ",limit: " + limit);
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		List<GroupChat> groupChats = chatHistoryService.getGroupChatHistory(userContext, to, 9999999999999L, offset, limit);
		logger.info("getGroupChatHistory returned " + (groupChats == null ? null : groupChats.size()) + " records");

		decryptName(userContext, groupChats, to);

		JsonNode output = null;
		if (mergeChats) {// message Grouping needed
			Group group = processGroupChatHistory(to, userContext, groupChats);
			logger.debug("processedGroupChatHistory to get Group" + group);
			output = Json.toJson(group);
		} else {// message Grouping not needed
			logger.debug("No group chat mering needed ");
			output = Json.toJson(groupChats);
		}
		logger.debug("returning Group chat history");
		return ok(output);
	}

	@With(UserAuthAction.class)
	public Result updateDocType(Integer messageId, Integer attachmentId, Integer docType, String docTypeText, Integer documentPurpose, String documentPurposeText) {
		logger.info("doc update");
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		GroupChat groupChat = groupChatMessageService.updateDocType(messageId, attachmentId, docType, docTypeText, documentPurpose, documentPurposeText, userContext.getUser().getId());
		return ok(Json.toJson(groupChat));
	}

	/**
	 * API used by Mobile Applications
	 * 
	 * @param to
	 * @param lastMsgDate
	 * @param offset
	 * @param limit
	 * @return
	 */
	@With(UserAuthAction.class)
	public Result getGroupChatHistoryV2(Integer to, Long lastMsgDate, final Integer offset, Integer limit) {
		logger.info("Get Group chat history - to: " + to + ",offset: " + offset + ",limit: " + limit);
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		List<GroupChat> groupChats = chatHistoryService.getGroupChatHistory(userContext, to, lastMsgDate, offset, limit);
		logger.info("getGroupChatHistory returned " + (groupChats == null ? null : groupChats.size()) + " records");
		//logger.info("getGroupChatHistory returned:  "+groupChats);
		Group group = processGroupChatHistoryBySender(to, userContext, groupChats);
		logger.debug("processGroupChatHistoryBySender  to get Group" + group);
		JsonNode output = Json.toJson(group);
		logger.info("returned Group chat history");
		return ok(output);
	}

	@With(UserAuthAction.class)
	public Result updatePresence() {
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		logger.info("update presence for user " + userContext.getUser().getId());
		// presenceService.addUpdateClosedConnection(userContext);
		logger.info("returned chat history");
		return ok();
	}

	@With(UserAuthAction.class)
	public Result sendMessage(Request request) {
		logger.debug("sending Message request().body().asJson() = " + request.body().asJson());
		ChatMessage message = Json.fromJson(request.body().asJson(), ChatMessage.class);
		//UserConnection connection = new UserConnection((UserContext) ctx().args.get("usercontext"), null);
		UserConnection connection = new UserConnection(ThreadContext.getUserContext(), null);
		chatMessageService.sendMessage(connection, message);
		JsonNode result = Json.toJson(message);
		logger.info("sent Message  = " + message);
		return ok(result);
	}

	@With(ApiAuthAction.class)
	public Result systemSendMessage(Request request) {
		logger.debug("sending system Message request().body().asJson() = " + request.body().asJson());
		//	ChatMessage message = Json.fromJson(request().body().asJson(), ChatMessage.class);
		ChatMessage message = Json.fromJson(request.body().asJson(), ChatMessage.class);
		Integer from = message.getFrom();

		User user = cacheService.getUser(from, false);
		UserContext context = ThreadContext.getUserContext();
		String apiKey = context.getApiKey();

		accessCheckUtil.checkPrivateKey(user.getOrganizationId(), apiKey);
		context.setUser(user);

		UserConnection connection = new UserConnection(context, null);
		chatMessageService.sendMessage(connection, message);
		JsonNode result = Json.toJson(message);

		logger.info("sent Message  = " + message);
		return ok(result);
	}

	private List<MergedOne2OneChat> processChatHistory(UserContext userContext, List<One2OneChat> one2OneHistory) {
		logger.info("merge one2one chat history as per time received " + (one2OneHistory == null ? null : one2OneHistory.size()) + " records");
		List<MergedOne2OneChat> mergedChatList = new ArrayList<MergedOne2OneChat>();
		MergedOne2OneChat mergedChat = null;
		MergedOne2OneChat lastMergedChat = null;
		Integer chatTextCreatorId = null;
		if (one2OneHistory != null) {
			for (One2OneChat chat : one2OneHistory) {
				if (chatTextCreatorId == null || !chat.getFrom().equals(chatTextCreatorId) || isDelayedComment(chat, mergedChat)) {
					if (chatTextCreatorId != null) {
						lastMergedChat = mergedChatList.get(mergedChatList.size() - 1);
						Integer dateDiff = commonUtil.getDateDifferenceInDays(
								((One2OneChat) lastMergedChat.getChatHistory().get(lastMergedChat.getChatHistory().size() - 1)).getCreatedDate(), userContext
								.getUser().getTimezone());
						lastMergedChat.setDateDiff(dateDiff);
						logger.info("Got the lastMergedChat as " + lastMergedChat);
					}
					mergedChat = new MergedOne2OneChat(chat.getFrom(), new ArrayList<One2OneChat>());
					mergedChat.getChatHistory().add(chat);
					mergedChatList.add(mergedChat);
					chatTextCreatorId = chat.getFrom();
					logger.debug("created mergedChat as " + mergedChat + " added chat " + chat.getId());
				} else {
					mergedChat.getChatHistory().add(chat);
					logger.debug("added chat " + chat.getId() + " to mergechat " + mergedChat);
				}
			}
			if (chatTextCreatorId != null && mergedChatList.get(mergedChatList.size() - 1).getDateDiff() == null) {
				lastMergedChat = mergedChatList.get(mergedChatList.size() - 1);
				Integer dateDiff = commonUtil.getDateDifferenceInDays(((One2OneChat) lastMergedChat.getChatHistory()
						.get(lastMergedChat.getChatHistory().size() - 1)).getCreatedDate(), userContext.getUser().getTimezone());
				lastMergedChat.setDateDiff(dateDiff);
				logger.info("Got the lastMergedChat as " + lastMergedChat);
			}
		}
		logger.info("merged chat history as per time");
		return mergedChatList;
	}

	private Group processGroupChatHistory(Integer groupId, UserContext userContext, List<GroupChat> groupChats) {
		logger.info("merge group chat history as per time received " + (groupChats == null ? null : groupChats.size()) + " records");
		List<MergedGroupChat> mergedChatList = new ArrayList<MergedGroupChat>();
		MergedGroupChat mergedChat = null;
		MergedGroupChat lastMergedChat = null;
		Integer chatTextCreatorId = null;
		Integer dateDiff = 0;
		for (GroupChat chat : groupChats) {
			dateDiff = commonUtil.getDateDifferenceInDays(chat.getCreatedDate(), userContext.getUser().getTimezone());
			if (ChatMessageType.SystemMessage.getId().byteValue() == chat.getChatMessageType().byteValue()) {// System notification
				//MergedGroupChat mergedChat = null;
				chat.setChatMessageType(null);
				mergedChat = new MergedGroupChat(chat.getSenderId(), new ArrayList<GroupChat>());
				mergedChat.getChatHistory().add(chat);
				mergedChat.setChatMessageType(ChatMessageType.SystemMessage.getId());
			//	mergedChatList.add(mergedChat);
				chatTextCreatorId = null;
				logger.debug("Created group mergedChat " + mergedChat + " using system chat message " + chat.getId());
			} else {
				chat.setChatMessageType(null);
				if (chatTextCreatorId == null || !chat.getSenderId().equals(chatTextCreatorId) || isDelayedComment(chat, mergedChat)) {
					if (chatTextCreatorId != null) {
						lastMergedChat = mergedChatList.get(mergedChatList.size() - 1);
						dateDiff = commonUtil.getDateDifferenceInDays(
								((GroupChat) lastMergedChat.getChatHistory().get(lastMergedChat.getChatHistory().size() - 1)).getCreatedDate(), userContext
								.getUser().getTimezone());
						lastMergedChat.setDateDiff(dateDiff);
						logger.info("Got group lastMergedChat as " + lastMergedChat);
					}
					mergedChat = new MergedGroupChat(chat.getSenderId(), new ArrayList<GroupChat>());
					if (commonUtil.isMobileChatClient(userContext.getClientId())) {
						setSenderInfo(mergedChat);
					}
					mergedChat.getChatHistory().add(chat);
				//	mergedChatList.add(mergedChat);
					chatTextCreatorId = chat.getSenderId();
					logger.debug("Created group mergedChat " + mergedChat + " added chat message " + chat.getId());
				} else {
					mergedChat.getChatHistory().add(chat);
					logger.debug("added group chat " + chat.getId() + " to mergechat " + mergedChat);
				}
			}
			mergedChat.setDateDiff(dateDiff);

		}
		mergedChatList.add(mergedChat);
		if (chatTextCreatorId != null && mergedChatList.get(mergedChatList.size() - 1).getDateDiff() == null) {
			lastMergedChat = mergedChatList.get(mergedChatList.size() - 1);
			dateDiff = commonUtil.getDateDifferenceInDays(((GroupChat) lastMergedChat.getChatHistory().get(lastMergedChat.getChatHistory().size() - 1))
					.getCreatedDate(), userContext.getUser().getTimezone());
			lastMergedChat.setDateDiff(dateDiff);
			logger.info("Got group lastMergedChat as " + lastMergedChat);
		}
		logger.debug("merged chat history as per time");
		Group group = new Group(groupId, mergedChatList);
		return group;
	}

	private void setSenderInfo(MergedGroupChat mergedGroupChat) {
		logger.info("setSenderInfo to merged group chat " + mergedGroupChat);
		JsonNode userJson = cacheService.getUserJson(mergedGroupChat.getUserId());
		User user = new User();
		user.setId(mergedGroupChat.getUserId());
		user.setName(userJson.findPath("firstName").asText());
		JsonNode photoURL = userJson.findPath("photoURL");
		if (photoURL != null && !photoURL.isMissingNode()) {
			String photoStr = photoURL.asText();
			if (photoStr != null && !"".equalsIgnoreCase(photoStr)) {
				photoStr = photoStr.replaceAll("//", "");
				ObjectMapper mapper = new ObjectMapper();
				try {
					JsonNode photoNode = (JsonNode) mapper.readTree(photoStr);
					if (photoNode != null && !photoNode.isMissingNode()) {
						JsonNode thumbnail = photoNode.findPath("thumbnail");
						user.setThumbnailUrl(thumbnail.asText());
					}
				} catch (Exception ex) {
					logger.error("failed to parse photo url", ex);
				}
			}
		}
		logger.info("set Sender to merged group chat " + mergedGroupChat + " as user " + user.getId());
		mergedGroupChat.setSenderInfo(user);
	}

	private boolean isDelayedComment(One2OneChat chat, MergedOne2OneChat mergedChat) {
		boolean delayedComment = false;
		logger.info("Check if One2OneChat delayed comment for chat " + chat.getId());
		Long permitedDelay = Long.valueOf(PropertyUtil.getProperty(Constants.CHAT_GROUP_DELAY));
		if (mergedChat.getChatHistory() != null && !mergedChat.getChatHistory().isEmpty()) {
			Long lastCommentDate = ((One2OneChat) mergedChat.getChatHistory().get(mergedChat.getChatHistory().size() - 1)).getCreatedDate();
			Long newCommentDate = chat.getCreatedDate();
			Long delayDiff = Math.abs(newCommentDate - lastCommentDate);
			if (delayDiff >= permitedDelay) {
				delayedComment = true;
			}
		}
		delayedComment = false;
		logger.info("Checked One2OneChat delayed comment : " + delayedComment + " for chat " + chat.getId());
		return delayedComment;
	}

	private boolean isDelayedComment(GroupChat chat, MergedGroupChat mergedChat) {
		boolean delayedComment = false;
		logger.info("Check if GroupChat delayed comment for chat " + chat.getId());
		Long permitedDelay = Long.valueOf(PropertyUtil.getProperty(Constants.CHAT_GROUP_DELAY));
		if (mergedChat.getChatHistory() != null && !mergedChat.getChatHistory().isEmpty()) {
			Long lastCommentDate = ((GroupChat) mergedChat.getChatHistory().get(mergedChat.getChatHistory().size() - 1)).getCreatedDate();
			Long newCommentDate = chat.getCreatedDate();
			Long delayDiff = Math.abs(newCommentDate - lastCommentDate);
			if (delayDiff >= permitedDelay) {
				delayedComment = true;
			}
		}
		delayedComment = false;
		logger.info("Checked GroupChat delayed comment : " + delayedComment + " for chat " + chat.getId());
		return delayedComment;
	}

	@With(UserAuthAction.class)
	public Result getUnReadMessagesUserCount() {
		UserContext userContext = ThreadContext.getUserContext();//(UserContext) ctx().args.get("usercontext");
		logger.info("get unread messages user count for user " + userContext.getUser().getId());
		List<ChatSummary> contactUnreadDetails = chatHistoryService.getUnReadMsgsPerContact(userContext.getUser().getId());
		logger.info("return unread message count for user " + userContext.getUser().getId() + " is " + contactUnreadDetails.size());
		return ok(Json.toJson(Integer.valueOf(contactUnreadDetails.size())));
	}

	@With(ApiAuthAction.class)	
	public Result getUnreadContactcount(String strUserIds, Integer offset, Integer limit) {
		logger.info("get unread contact count for user " + strUserIds);
		List<Integer> userIds = new ArrayList<Integer>();
		if (strUserIds != null && !strUserIds.isEmpty()) {
			String[] strUserIdArray = strUserIds.split(",");
			for (String strUserId : strUserIdArray) {
				userIds.add(Integer.valueOf(strUserId));
			}
		}
		List<ChatSummary> chatUnreadCounts = chatHistoryService.getUnReadMsgContact(userIds, offset, limit);
		logger.info("unread contact count resultset size is " + chatUnreadCounts.size());
		return ok(Json.toJson(chatUnreadCounts));
	}

	public Result chatOptions() {
		logger.info("setting cors headers ");
		if (enableCorsHeaders) {
			//setCorsHeaders(response());
		}
		return ok();
	}

	private void checkOriginHeader(RequestHeader request) {		
		Optional<String> originH = request.header(Constants.Origin);
		String originStr = null;
		if (originH != null && originH.isPresent()) {
			originStr = originH.get();
		}

		logger.info("origin in request: "+originStr);

		Boolean validOrigin = false;
		if (originStr != null) {
			for (String origin: originList) {
				logger.debug("origin from DB: "+origin);
				if (originStr.toLowerCase().contains(origin.toLowerCase())) {
					validOrigin = true;
					break;
				}
			}			
		}
		if (!validOrigin) {
			logger.error("Origin is not matched");
			throw new UnAuthorizedException(ErrorCode.Invalid_Origin, "Invalid_Origin");
		}
	}

	public WebSocket chat() {
		return WebSocket.Json.acceptOrResult(request -> {
			Optional<String> requestId = request.header(Constants.X_REQUEST_ID);
			UserContext userContext = null;
			Integer userId = null;
			String clientId = null;
			Integer status = Status.OK;
			long t1 = System.currentTimeMillis();
			try {
				if (requestId.isEmpty()) {
					requestId = Optional.of(UUID.randomUUID().toString());
				}
				final String uuid = UUID.randomUUID().toString();
				UserConnection userConnection;
				try {
					userContext = authUtil.checkUserAuth(request, requestId.get());			
					userId = userContext.getUser().getId();
					clientId = userContext.getClientId();
					if (!commonUtil.isMobileChatClient(userContext.getClientId())) {
						logger.info("client id is : "+userContext.getClientId());
						checkOriginHeader(request);
					}
					
					if (enableXsrfCheck && !commonUtil.isMobileChatClient(userContext.getClientId())) {
						CommonUtil.checkCSRFToken(request);
					}
					userConnection = new UserConnection(userContext, uuid);
				}
				catch (ApplicationException ex) {
					logger.info("exception occured: ", ex);
					userConnection = new UserConnection(uuid, authUtil.getClientId(request), ex);
				}

				UserConnectionActor.Create create = new UserConnectionActor.Create(uuid);
				Props props = Props.create(UserConnectionActor.class, userConnection, RmsActorSystem.getMaterializer());	
				ActorRef ref = RmsActorSystem.get().actorOf(props, uuid);		
				userConnection.setActorRef(ref);

				userConnectionService.initialiseUserConnection(userConnection);
				logger.debug("created and initialised userConnection with uuid " + uuid);

				final CompletionStage<Flow<JsonNode, JsonNode, NotUsed>> future = wsFutureFlow(request, ref, create);
				final CompletionStage<Either<Result, Flow<JsonNode, JsonNode, ?>>> stage = future.thenApply(Either::Right);
				return stage.exceptionally(this::logException);
			}
			catch (Exception e) {
				logger.error("failed to create socket :", e);
				if (e instanceof UnAuthorizedException) {
					WebSocketErrorCode code = new WebSocketErrorCode(ErrorCode.Unauthorized_Api_Access.getId(), "UnAuthorizedException");
					JsonNode jsonCode = Json.toJson(code);
					logger.error("UnAuthorizedException request");
					status = Status.UNAUTHORIZED;
					return unauthenticatedResult(jsonCode);
				}
				else if (e instanceof ForbiddenException) {
					WebSocketErrorCode code = new WebSocketErrorCode(ErrorCode.Forbidden.getId(), "ForbiddenException");
					JsonNode jsonCode = Json.toJson(code);
					logger.error("ForbiddenException request");
					status = Status.FORBIDDEN;
					return forbiddenResult(jsonCode);
				} 
				else {
					WebSocketErrorCode code = new WebSocketErrorCode(ErrorCode.Internal_Server_Error.getId(), "UnAuthorizedException");
					JsonNode jsonCode = Json.toJson(code);
					status = Status.INTERNAL_SERVER_ERROR;
					logger.error("Internal server error request");
					return  internalServerErrorResult(jsonCode);
				}
			}
			finally {
				Integer diff = (int) (System.currentTimeMillis() - t1);
				authUtil.printRequest(request, diff, userId, status, clientId, requestId.get());
			}
		});
	}


	@SuppressWarnings("unchecked")
	private CompletionStage<Flow<JsonNode, JsonNode, NotUsed>> wsFutureFlow(Http.RequestHeader request, ActorRef ref, 
			UserConnectionActor.Create  create) {		
		return Patterns.ask(ref, create, time).thenApply((Object flow) -> {
			logger.info(("inside ask"));
			final Flow<JsonNode, JsonNode, NotUsed> f = (Flow<JsonNode, JsonNode, NotUsed>) flow;			
			return f.named("websocket");
		});
	}


	private void setCorsHeaders(Http.Response response) {
		logger.info("set cors headers");
		//	response.setHeader("Access-Control-Allow-Origin", origin);
		response.setHeader("Access-Control-Allow-Methods", methods);
		response.setHeader("Access-Control-Max-Age", maxAge);
		response.setHeader("Access-Control-Allow-Headers", headers);
		logger.info("cors headers set");
	}

	private List<MergedOne2OneChat> processChatHistoryBySender(List<One2OneChat> one2OneHistory) {
		logger.info("merge One2OneChat chat history as per sender. received size " + one2OneHistory.size());
		List<MergedOne2OneChat> mergedChatList = new ArrayList<MergedOne2OneChat>();
		MergedOne2OneChat mergedChat = null;
		Integer chatTextCreatorId = null;
		//UserContext userContext = (UserContext) ctx().args.get("usercontext");
		for (One2OneChat chat : one2OneHistory) {
			if (chatTextCreatorId == null || !chat.getFrom().equals(chatTextCreatorId)) {
				mergedChat = new MergedOne2OneChat(chat.getFrom(), new ArrayList<One2OneChat>());
				mergedChat.getChatHistory().add(chat);
				mergedChatList.add(mergedChat);
				chatTextCreatorId = chat.getFrom();
			} else {
				mergedChat.getChatHistory().add(chat);
			}
		}
		logger.info("merged One2OneChat chat history as per sender with size " + mergedChatList.size());
		return mergedChatList;
	}

	private Group processGroupChatHistoryBySender(Integer groupId, UserContext userContext, List<GroupChat> groupChats) {
		logger.info("merge GroupChat chat history as per sender. received size " + groupChats.size());
		List<MergedGroupChat> mergedChatList = new ArrayList<MergedGroupChat>();
		MergedGroupChat mergedChat = null;
		Integer chatTextCreatorId = null;
		for (GroupChat chat : groupChats) {
			if(chat.getChatMessageType()!= null && chat.getChatMessageType().byteValue() == ChatMessageType.ScreenShot.getId().byteValue()) {
				logger.info("Chat MessageType is Screenshot: "+ chat.getChatMessageType()+ "& MessageId is : "+ chat.getId()+"& text is: "+chat.getText());
			}
			if (ChatMessageType.SystemMessage.getId().byteValue() == chat.getChatMessageType().byteValue()) {// System notification
				chat.setChatMessageType(null);
				mergedChat = new MergedGroupChat(chat.getSenderId(), new ArrayList<GroupChat>());
				mergedChat.getChatHistory().add(chat);
				mergedChat.setChatMessageType(ChatMessageType.SystemMessage.getId());
				mergedChatList.add(mergedChat);
				chatTextCreatorId = null;
			} else {
				//chat.setChatMessageType(null);  		
				
				if (chatTextCreatorId == null || !chat.getSenderId().equals(chatTextCreatorId)) {
					mergedChat = new MergedGroupChat(chat.getSenderId(), new ArrayList<GroupChat>());
					logger.info("Merged Chat: "+mergedChat);
					if (commonUtil.isMobileChatClient(userContext.getClientId())) {
						setSenderInfo(mergedChat);
					}
					mergedChat.getChatHistory().add(chat);
					mergedChat.setChatMessageType(chat.getChatMessageType());
				    mergedChatList.add(mergedChat);
					chatTextCreatorId = chat.getSenderId();
				} else {
					mergedChat.getChatHistory().add(chat);
					//TODO: need to check impact of below 2 lines
					mergedChat.setChatMessageType(chat.getChatMessageType());
					
				}
			}
			if(chat.getChatMessageType()!= null && chat.getChatMessageType().byteValue() == ChatMessageType.ScreenShot.getId().byteValue()) {
				logger.info("Chat MessageType is Screenshot: "+ chat.getChatMessageType()+ "& MessageId is : "+ chat.getId()+"& text is: "+chat.getText());
				logger.info("GMerged GroupChat MessageType is : "+ mergedChat.getChatMessageType());
				if(mergedChat.getChatMessageType() == null) {
					mergedChat.setChatMessageType(chat.getChatMessageType());
				}
			}			
		}
		//mergedChatList.add(mergedChat);
		logger.info("merged GroupChat chat history as per sender with size " + mergedChatList.size());
		Group group = new Group(groupId, mergedChatList);
		return group;
	}


	private void decryptName(UserContext userContext, List<GroupChat> groupChats, Integer groupId) {
		if (groupId != null) {
			Group group = cacheService.getGroupDetails(groupId);
			logger.info("Group Type: " + group.getGroupType());
			if (group.getDataEncryptionKeyId() != null) {
				for (GroupChat chat : groupChats) {
					Integer userId = chat.getSenderId();
					User user = cacheService.getUser(userId, false);
					if (UserCategory.Guest.getId().equals(user.getUserCategory().intValue())) {
						logger.info("Decrypt value required for given organization");
						String updatedName = dataEncryptionService.decrypt(group.getDataEncryptionKeyId(), chat.getFromName());
						logger.info("Decrypt value is done");
						chat.setFromName(updatedName);
					}
				}
			}
		}
	}
	private CompletionStage<Either<Result, Flow<JsonNode, JsonNode, ?>>> forbiddenResult(JsonNode node) {
		final Result forbidden = Results.forbidden(node.asText());
		final Either<Result, Flow<JsonNode, JsonNode, ?>> left = Either.Left(forbidden);
		return CompletableFuture.completedFuture(left);
	}

	private CompletionStage<Either<Result, Flow<JsonNode, JsonNode, ?>>> unauthenticatedResult(JsonNode node) {
		final Result unauthorized = Results.unauthorized(node.asText());
		final Either<Result, Flow<JsonNode, JsonNode, ?>> left = Either.Left(unauthorized);
		return CompletableFuture.completedFuture(left);
	}

	private CompletionStage<Either<Result, Flow<JsonNode, JsonNode, ?>>> internalServerErrorResult(JsonNode node) {
		final Result internalError = Results.internalServerError(node.asText());
		final Either<Result, Flow<JsonNode, JsonNode, ?>> left = Either.Left(internalError);
		return CompletableFuture.completedFuture(left);
	}
	
	private CompletionStage<Either<Result, Flow<JsonNode, JsonNode, ?>>> badRequestExceptionResult(JsonNode node) {
		final Result badRequest1 = Results.badRequest(node.asText());
		final Either<Result, Flow<JsonNode, JsonNode, ?>> left = Either.Left(badRequest1);
		return CompletableFuture.completedFuture(left);
	}

	private Either<Result, Flow<JsonNode, JsonNode, ?>> logException(Throwable throwable) {
		logger.error("Cannot create websocket", throwable);
		Result result = Results.internalServerError("error");
		return Either.Left(result);
	}
}
class WebSocketErrorCode {
	private int code;
	private String msg;
	public WebSocketErrorCode(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
}
