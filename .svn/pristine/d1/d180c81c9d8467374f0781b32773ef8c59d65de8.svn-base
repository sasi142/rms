package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.JsonNode;

import com.google.inject.Singleton;

import controllers.actions.PublicApiAction;
import controllers.actions.UserAuthAction;
import controllers.aspects.ValidatorAspect;
import controllers.dto.MemoDto;
import core.akka.actors.RmsActorSystem;
import core.entities.Event;
import core.entities.Memo;
import core.entities.MemoChatUser;
import core.entities.MemoRecipient;
import core.entities.User;
import core.entities.UserContext;
import core.entities.UserPhoto;
import core.exceptions.BadRequestException;
import core.exceptions.InternalServerErrorException;
import core.services.CacheService;
import core.services.MemoService;
import core.services.UserService;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.Enums.ClientType;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.EventType;
import core.utils.Enums.FileUploadType;
import core.utils.Enums.MemoType;
import core.utils.Enums.UserCategory;
import core.utils.PropertyUtil;
import core.utils.SeoUtil;
import core.utils.ThreadContext;
import core.utils.ValidateRequestUtil;
import play.libs.Files;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Status;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.With;
import utils.RmsApplicationContext;



@Singleton
public class MemoController extends Controller implements InitializingBean  {
	final static Logger logger = LoggerFactory.getLogger(MemoController.class);

	private CommonUtil commonUtil;
	private MemoService memoService;

	@Qualifier("RmsCacheService")
	private CacheService cacheService;
	private UserService userService;
	private VelocityEngine velocityEngine;
	private ValidateRequestUtil validateRequestUtil;
	private ValidatorAspect validatorAspect ;
	private SeoUtil seoUtil;

	private Environment env;

	private String errorHtmlPagePath;
	private String followMsgTemplatePath;
	private String hostName;
	private String basePath;
	List<String> extensionList= new ArrayList<String>();
	List<String> alphabets= new ArrayList<String>(Arrays.asList("abcdefghij".split("")));


	public MemoController() {
		ApplicationContext ctx = RmsApplicationContext.getInstance().getSpringContext();
		commonUtil = (CommonUtil) ctx.getBean(Constants.COMMON_UTIL_SPRING_BEAN);
		env = ctx.getBean(Environment.class);
		memoService = (MemoService) ctx.getBean(Constants.MEMO_SERVICE_BEAN);
		validatorAspect = (ValidatorAspect) ctx.getBean(Constants.VALIDATOR_ASPECT_SPRING_BEAN);
		validateRequestUtil = (ValidateRequestUtil) ctx.getBean(Constants.VALIDATE_REQUEST_UTIL_SPRING_BEAN);
		userService = (UserService) ctx.getBean(Constants.USER_SERVICE_BEAN);
		velocityEngine = ctx.getBean(VelocityEngine.class); // velocityEngine is thread safe. Velocity context is not
		cacheService = (CacheService) ctx.getBean(Constants.CACHE_SERVICE_SPRING_BEAN);
		seoUtil = (SeoUtil) ctx.getBean(Constants.SEO_UTIL_SPRING_BEAN);
		init();
	}

	@With(UserAuthAction.class)
	public Result createMemo(Request request) {// Input memoDto to be parsed from httpReq
		logger.debug("request().body().asJson() = " + request.body().asJson());
		MemoDto memoDto = null;
		MemoDto inMemoDto = Json.fromJson(request.body().asJson(), MemoDto.class);
		validateRequestUtil.validateCreateMemo(inMemoDto);
		logger.debug("validated memo");
		Memo inMemo = inMemoDto.toMemo();
		Memo memo = memoService.createMemo(inMemo);
		logger.debug("created memo");
		memoDto = new MemoDto(memo);
		setMemoDto(memoDto, memo);
		JsonNode result = Json.toJson(memoDto);
		logger.debug("return memo result reday as " + result + " sending CreateMemo event");

		Event event = new Event();
		event.setType(EventType.CreateMemo.getId());
		Map<String, String> data = new HashMap<String, String>();
		data.put("Id", memo.getId().toString());
		data.put("CreatedById", memo.getCreatedById().toString());
		data.put("RecipientIds", StringUtils.join(memo.getRecipientIds(), ","));
		data.put("Subject", memo.getSubject());
		event.setData(data);
		logger.debug("sending CreateMemo event as " + event.toString());
		RmsActorSystem.getEventRouterActorRef().tell(event, null);
		logger.info("sent CreateMemo event ");
		logger.info("return memo with id " + memoDto.getId());
		return ok(result);
	}

	@With(UserAuthAction.class)
	public Result createUserMemo(Request request) {// Input memoDto to be parsed from httpReq
		logger.info("request().body().asJson() = " + request.body().asJson());		
		MemoDto inMemoDto = Json.fromJson(request.body().asJson(), MemoDto.class);
		validateRequestUtil.validateCreateUserMemo(inMemoDto);
		logger.debug("validated memo");
		Memo inMemo = inMemoDto.toMemo();
		memoService.updateUserMemoDetailsInDump(inMemo);		
		logger.debug(" memo details updated");	
		return  Results.status(Status.ACCEPTED);
	}

	@With(UserAuthAction.class)
	public Result createChannelMessage(Integer channelId, Request request) {
		logger.info("Creating ChannelMessage for channelId:.." + channelId);
		logger.debug("request().body().asJson() = " + request.body().asJson());
		MemoDto inMemoDto = Json.fromJson(request.body().asJson(), MemoDto.class);
		validateRequestUtil.validateChannelMessage(inMemoDto, channelId);
		logger.debug("validated memo");
		Memo inMemo = inMemoDto.toMemo();
		Memo memo = memoService.createChannelMessage(inMemo, channelId);
		logger.debug("created memo with CreatorId:" + memo.getCreatedById() + " and memoId:" + memo.getId());
		MemoDto memoDto = new MemoDto(memo);
		setMemoDto(memoDto, memo);
		sendCreateChannelMessageEvent(memo);
		JsonNode result = Json.toJson(memoDto);
		logger.debug("CreateChannelMessage result: " + result);
		return ok(result);
	}

	@With(UserAuthAction.class)
	public Result getMemosByOrgId(Integer orgId, Integer offset, Integer limit) {
		logger.info("get List of memos created in org " + orgId);
		validatorAspect.validateGetMemosByOrgId(orgId);
		MemoDto memoDto = null;
		List<MemoDto> memoList = new ArrayList<MemoDto>();

		if (offset == null || offset < 0) {
			offset = 0;
		}
		if (limit == null || limit < 1) {
			limit = 20;
		}
		List<Memo> memos = memoService.getMemosByOrgId(offset, limit);
		logger.info("got List of memos created in org " + orgId + " of size " + memos.size());
		for (Memo memo : memos) {
			memoDto = new MemoDto(memo);
			setMemoDto(memoDto, memo);
			unescapeMemoText(memoDto);
			memoList.add(memoDto);
		}
		JsonNode result = Json.toJson(memoList);
		logger.info("return memo list for org-id " + orgId);
		return ok(result);
	}

	@With(UserAuthAction.class)
	public Result getMemosByUserId(Integer userId, Integer offset, Integer limit) {
		logger.info("get List of memos received by user " + userId);
		Integer defaultChannelId = 0;
		validateRequestUtil.validateGetMemosByUserId(userId, defaultChannelId, offset, limit);
		MemoDto memoDto = null;
		List<MemoDto> memoList = new ArrayList<MemoDto>();
		List<Memo> memos = memoService.getMemosByUserId(userId, offset, limit);
		logger.info("got List of memos created for user " + userId + " of size " + memos.size());
		for (Memo memo : memos) {
			memoDto = new MemoDto(memo);
			setMemoDto(memoDto, memo);
			unescapeMemoText(memoDto);
			memoList.add(memoDto);
		}

		JsonNode result = Json.toJson(memoList);
		logger.info("return memo list for user-id " + userId);
		return ok(result);
	}

	@With(UserAuthAction.class)
	public Result getMemosByUserIdV2(Integer userId, Integer channelId, Integer offset, Integer limit) {
		logger.info("get List of memos received by user " + userId);
		validateRequestUtil.validateGetMemosByUserId(userId, channelId, offset, limit);
		MemoDto memoDto = null;
		List<MemoDto> memoList = new ArrayList<MemoDto>();
		logger.info("userId:" + userId + "channelId:" + channelId + "offset:" + offset + "limit:" + limit);
		List<Memo> memos = memoService.getMemosByUserIdV2(userId, channelId, offset, limit);
		logger.debug("got List of memos created for user " + userId + " of size " + memos.size());
		for (Memo memo : memos) {
			memoDto = new MemoDto(memo);
			logger.debug("memoDto with memoDetails:" + memoDto);
			setMemoDto(memoDto, memo);
			logger.debug("memoDto after setMemoDto:" + memoDto);
			unescapeMemoText(memoDto);
			memoList.add(memoDto);
		}

		JsonNode result = Json.toJson(memoList);
		logger.info("return memo list for user-id " + userId);
		return ok(result);
	}

	@With(UserAuthAction.class)
	public Result getMemoCountByStatus(Integer userId, Boolean readStatus) {
		if (readStatus == null) {
			readStatus = Boolean.FALSE;
		}
		logger.info("get count of " + readStatus + " memos received by user " + userId);
		validatorAspect.validateGetMemoCountByStatus(userId);
		Long count = memoService.getMemoCountByStatus(userId, readStatus);
		logger.info("Got count of " + readStatus + " memos received by user " + userId + " as count " + count);
		return ok(count.toString());
	}

	@With(UserAuthAction.class)
	public Result getMemoDetails(Integer memoId, Boolean needSummary) {
		logger.info("Getting details of memo " + memoId);

		Memo memo = memoService.getMemoDetails(memoId, needSummary);
		if (memo == null) {
			throw new BadRequestException(ErrorCode.Invalid_Memo, memoId);
		}
		validatorAspect.validateGetMemoDetails(memo, needSummary);
		logger.info("size Before memoText: " + memo.getMessage().length());
		MemoDto memoDto = new MemoDto(memo);
		logger.info("size After memoText: " + memoDto.getText().length());
		setMemoDto(memoDto, memo);
		unescapeMemoText(memoDto);
		User currentUser = ThreadContext.getUserContext().getUser();
		if(memo.getShowUserDetailOnSCP() && memo.getIsPublic() && (memoService.isReceipient(memo.getId(), currentUser.getId()) == 1)) {
			setSCPShareByUserId(memoDto, currentUser.getId());
		}	
		logger.info("size After UnmemoText: " + memoDto.getText().length());
		JsonNode result = Json.toJson(memoDto);
		logger.info("return memo with id " + memoDto.getId());
		return ok(result);
	}

	@With(UserAuthAction.class)
	public Result getMemoDetailsV2(Integer memoId, Boolean needSummary) {
		logger.info("Getting details of ChannelMessage with Id:" + memoId);
		Memo memo = memoService.getMemoDetailsV2(memoId, needSummary);
		validateRequestUtil.validateGetMemoDetailsV2(memo, needSummary);
		logger.info("Length Before memoText: " + memo.getMessage().length());
		MemoDto memoDto = new MemoDto(memo);
		logger.info("Length After memoText: " + memoDto.getText().length());
		setMemoDto(memoDto, memo);
		unescapeMemoText(memoDto);
		logger.info("Length After unescapeMemoText: " + memoDto.getText().length());
		logger.debug("memoText: " + memo.getMessage());
		logger.debug("Getting ChannelMessage publicUrl");
		String publicUrl = CommonUtil.getMessagePublicUrl(memo);
		memoDto.setPublicURL(publicUrl);
		JsonNode result = Json.toJson(memoDto);
		logger.info("return memo with id " + memoDto.getId());
		return ok(result);
	}

	@With(UserAuthAction.class)
	public Result changeReadStatus(Integer memoId, Integer userId, Boolean readStatus) {
		logger.info("user " + userId + " changing read status to " + readStatus + " of memo " + memoId);
		validatorAspect.validateChangeReadStatus(memoId, userId);
		memoService.changeReadStatus(memoId, userId, readStatus);
		logger.info("user " + userId + " changed read status to " + readStatus + " of memo " + memoId);
		return ok();
	}

	@With(UserAuthAction.class)
	public Result getMemoDetailsDownloaded(Integer memoId, Integer offset, Integer limit) {
		logger.info("getting memo details for memoId " + memoId);
		Memo memo = memoService.getMemoDetails(memoId, false);
		if (memo == null) {
			throw new BadRequestException(ErrorCode.Invalid_Memo, memoId);
		}
		validatorAspect.validateGetMemoDetailsDownloaded(memo);
		logger.debug("got memo details for memoId " + memoId + " as memo " + memo);

		if (offset == null || offset < 0) {
			offset = 0;
		}
		if (limit == null || limit < 1) {
			limit = 999999999;
		}		
		String fileName= reportFileName(memo.getSubject());			
		File newFile = new File(fileName);
		String resultSet = null;
		try {
			if(MemoType.RegulerMemoExcelSelction.getId().byteValue() == memo.getMemoType() ||
					MemoType.RegulerMemoUserSelection.getId().byteValue() == memo.getMemoType()	) {				
				resultSet = memoService.getRegularMemoReportById(memoId);
			} else if(MemoType.CustomMemoSummary.getId().byteValue() == memo.getMemoType()) {
				logger.info(" memoType is" + memo.getMemoType());			
			resultSet = memoService.getCustomMemoReportById(memoId);
			}
			byte[] contents = resultSet.getBytes();		
			FileUtils.writeStringToFile(newFile, resultSet);
			response().setHeader("Content-Disposition", "attachment; filename=\"" + newFile.getName() + "\"");
		} catch(Exception e){
			logger.error("Failed creating file output Stream for file " + fileName, e);
		}
		return ok(newFile);	
	}

	@With(UserAuthAction.class)
	public Result updateMemoPublicState(Integer memoId, Boolean isPublic) {
		logger.info("changing memo public state to " + isPublic + " of memo " + memoId);
		Memo memo = memoService.getMemoDetails(memoId, false);
		validatorAspect.validateUpdateMemoPublicState(memo);
		if (memo.getIsPublic().equals(isPublic)) {
			logger.info("Memo with id : " + memoId + " already in same state : " + isPublic);
		} else {
			memoService.updateMemoPublicState(memoId, isPublic);
		}
		logger.info("changed memo public state to " + isPublic + " of memo " + memoId);
		return ok();
	}


	@With(PublicApiAction.class)
	public Result getMemoByPublicURL(String url) {
		MemoDto memoDto = getMemoByPublicUrl(url);
		JsonNode result = Json.toJson(memoDto);
		logger.info("return memo with id " + memoDto.getId());
		return ok(result);
	}

	private MemoDto getMemoByPublicUrl(String url) {
		logger.info("Getting details of memo by public url" );
		UserContext context = new UserContext();
		context.setClientId(ClientType.Web.getClientId());
		ThreadContext.setUsercontext(context);
		Memo memo = memoService.getMemoByPublicURL(url);

		// TODO: Hacky fix
		String memoText = memo.getMessage();
		logger.info("size Before memoText: " + memoText.length());
		logger.debug("Before memoText: " + memoText);
		MemoDto memoDto = new MemoDto(memo);
		logger.info("size After memoText: " + memoDto.getText().length());
		logger.debug("After memoText: " + memoText);
		setMemoDto(memoDto, memo);
		return memoDto;
	}


	@With(PublicApiAction.class)
	public Result getMemoByPublicURLWithSCPSharedUser(String url, String sharedById) {
		logger.info("Getting details of memo by public url" + url);		
		logger.info("Getting details of memo by public url" + sharedById);

		MemoDto memoDto = getMemoByPublicUrl(url);
		if(!memoDto.getShowUserDetailOnSCP()) {
			throw new BadRequestException(ErrorCode.Invalid_Memo_Public_Url, "show shared user details is not allowed for given api call");
		}
		setScpSharedUser(memoDto, sharedById);
		JsonNode result = Json.toJson(memoDto);
		logger.info("return memo with id " + memoDto.getId());
		return ok(result);
	}


	@With(PublicApiAction.class)
	public Result getMessagePublicPage(String memoOpenUrl, String name) {
		logger.info("Getting memo publicPage details for Publicurl" + memoOpenUrl);		

		String pageHtml = "<html><body><h4> Page not found </h4></body></html>";

		try {
			String publicUrl = memoOpenUrl.substring(memoOpenUrl.lastIndexOf('-') + 1);
			logger.debug("extracted publicUrl from memoOpenUrl is:" + publicUrl
					+ " calling MemoService for getting Details");
			Memo memo = memoService.getMessagePublicPage(publicUrl);
			logger.debug("got memoDetails for url:" + publicUrl);

			String memoText = memo.getMessage();
			logger.info("Before memoText Length: " + memoText.length());
			logger.debug("After memoText: " + memoText);
			MemoDto memoDto = new MemoDto(memo);
			logger.info("After memoText Length: " + memoDto.getText().length());
			logger.debug("After memoText: " + memoText);
			Map<String, Object> data = new HashMap<String, Object>();
			String channelName = memoDto.getChannelName();
			logger.debug("ChannelName:" + channelName);
			data.put("ChannelName", channelName);

			seoUtil.setSeoTagsForTitleDescriptionMsgHeading(memo, data, channelName) ;

			seoUtil.setSeoTagsForChannelCategoryImageLogoPublicUrl(memo, data, channelName);

			seoUtil.setSeoTagsForFirstAttachment(memoDto,data);

			String msgdateTime = CommonUtil.getReadableCreationDate(memo.getCreatedDate());
			logger.debug("msgdateTime: " + msgdateTime);
			data.put("MessageDateTime", msgdateTime);
			// calling memoDto.getText() escapes HTML tag,so instead used memo.getMessage()
			String msgBody = memo.getMessage();
			logger.debug("msgBody: " + msgBody);
			data.put("messageBody", msgBody);
			// for non-meta tags assign
			String templatePath = followMsgTemplatePath;
			logger.info("templatePath: " + templatePath);

			velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
			velocityEngine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
			velocityEngine.init();

			Template template = velocityEngine.getTemplate(templatePath);
			StringWriter stringWriter = new StringWriter();
			template.merge(new VelocityContext(data), stringWriter);
			pageHtml = stringWriter.toString();
			logger.debug("pageHtml is of length:" + pageHtml.length());
		} catch (Exception ex) {
			logger.error("Failed to get the profile page ", ex);
			try {
				errorHtmlPagePath = hostName + errorHtmlPagePath;
				return movedPermanently(errorHtmlPagePath);
			} catch (Exception e) {
				throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, "internal server error", ex);
			}
		}
		logger.info("returned profile html page");

		Result result = Results.status(Status.OK, pageHtml);
		result.withHeader("Content-Type", "text/html; charset=utf-8");
		return result;
	}

	@With(UserAuthAction.class)
	public Result getMemoChatUsers(Integer memoId, Integer offset, Integer limit) {
		logger.info("get List of chatUser by memoId " + memoId);

		validatorAspect.validateChatUsersByMemoId(memoId);
		MemoDto memoDto = null;
		List<MemoDto> memoChatUserList = new ArrayList<MemoDto>();

		if (offset == null || offset < 0) {
			offset = 0;
		}
		if (limit == null || limit < 1) {
			limit = 20;
		}
		List<MemoChatUser> chatUsers = memoService.getMemoChatUsers(memoId, offset, limit);
		// TODO::make it debug and add some more loggers
		logger.info("got List of chat users in memo" + memoId);
		for (MemoChatUser chatUser : chatUsers) {
			memoDto = new MemoDto(chatUser);
			memoChatUserList.add(memoDto);
		}
		JsonNode result = Json.toJson(chatUsers);
		logger.info("return chat user list for memo id " + memoId);
		return ok(result);
	}

	@With(UserAuthAction.class)
	public Result bulkMemoUpload(Request request, String uploadType) {		
		validateRequestUtil.validateBulkMemoCreateAccess();
		Integer memoDumpId = null;
		logger.info("File Upload Type: "+uploadType);
		basePath = PropertyUtil.getProperty(Constants.CREATE_MEMO_UPLOAD_FILE_BASE_PATH);
		logger.info("base path: "+basePath);	
		String extension = PropertyUtil.getProperty(Constants.SUPPORTED_EXCEL_FILE_EXT);
		extensionList = Arrays.asList(extension.split("\\s*,\\s*"));	
		logger.info("extension List is: "+extensionList);
		FileUploadType fileUploadType = Enums.valueOf(FileUploadType.class, uploadType);		
		Http.MultipartFormData<File> files = request.body().asMultipartFormData();
		for(Http.MultipartFormData.FilePart<File> filePart: files.getFiles()) {
			logger.info("uploading file.." + filePart);
			File file = filePart.getFile();
			validatorAspect.validateMemoUploadExcelFile(file, fileUploadType.getId());
			String ext = FilenameUtils.getExtension(filePart.getFilename());
			if(!extensionList.stream().map(String::toLowerCase).anyMatch(ext::contentEquals)) {
				throw new BadRequestException(ErrorCode.Invalid_File_Extension, ext);
			}				
			String uploadFileName = UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis() + "." +ext;
			logger.info("uploading file text " + file);
			String path = basePath + uploadFileName;
			logger.info("uploading file path is " + path);
			Files.TemporaryFile ref = (Files.TemporaryFile) filePart.getRef();
			ref.copyTo(Paths.get(path), true);

			//			try {
			//				File newFile = new File(path);
			//				InputStream inputStream = new FileInputStream(file.getAbsoluteFile());
			//				byte[] byteFile = IOUtils.toByteArray(inputStream);
			//				FileUtils.writeByteArrayToFile(newFile, byteFile);
			//				inputStream.close();
			//				logger.info("store file to specified path is done, path: " + path);
			//			} catch (Exception e) {
			//				logger.info("uploading file.."+e);
			//				throw new BadRequestException(ErrorCode.Parse_Excel_Failed, "read excel failed", e );
			//			}
			memoDumpId =  memoService.createMemoFileDetailsInDump(path, uploadFileName, fileUploadType.getId());		
		}	
		if(FileUploadType.UserMemo.getId().byteValue() == fileUploadType.getId().byteValue()) {
			MemoDto memoDto = new MemoDto();
			memoDto.setMemoDumpAttachmentId(memoDumpId);
			JsonNode result = Json.toJson(memoDto);
			logger.info("return memoDto ");
			return ok(result);
		}	
		return  Results.status(Status.ACCEPTED);
	}	

	private void setSCPShareByUserId(MemoDto memoDto , Integer number) {
		String cipherText = "";
		while (number > 0) {
			int digit = number % 10;  // Store digit in a variable
			cipherText = alphabets.get(digit)+ cipherText ;
			number = number/10;		   
		}
		memoDto.setScpSharedByUserId(cipherText);
	}


	private void setScpSharedUser(MemoDto memoDto, String sharedById) {		
		String[] userIdString = sharedById.split("");
		Integer userId =0;
		for(String ch: userIdString) {			
			userId = (userId*10) + alphabets.indexOf(ch);				
		}
		logger.info("get user details from cahche:" + userId);
		User user = cacheService.getUser(userId, true);
		User scpShareduser = new User();
		scpShareduser.setName(user.getName());
		scpShareduser.setEmail(user.getEmail());
		scpShareduser.setMobile(user.getMobile());
		memoDto.setScpSharedUser(scpShareduser);
	}



	private String reportFileName(String subject) {
		String timeZone = null;
		try {
			timeZone = ThreadContext.getUserContext().getUser().getTimezone();
		} catch (Exception e) {
			// pass time zone as null, so time would be casted to UTC
		}
		String date = CommonUtil.formatDateWithTimeZone(System.currentTimeMillis(), "ddMMMyyyy", timeZone);
		String fileName = subject.trim() + "_" + date + ".xls";
		fileName = fileName.replaceAll("/", "_");
		fileName = fileName.replaceAll("[:|*|?|<|>|/|\"|\\\\|\\|]", "_");
		logger.debug("created file name as :" + fileName);
		return fileName;
	}

	private void setMemoDto(MemoDto memoDto, Memo memo) {

		logger.info("Seting cretor on memo for user " + memo.getCreatedById());
		UserContext userContext = (UserContext) ctx().args.get("usercontext");
		if (userContext != null && userContext.getUser() != null && userContext.getUser().getTimezone() != null) {
			logger.debug("userContext is not null");
			String creationDate = commonUtil.getDateTimeWithTimeZone(memo.getCreatedDate(),
					userContext.getUser().getTimezone());
			memoDto.setCreationDate(creationDate);
			logger.info("Set creationDate on memo for user " + memo.getCreatedById() + ", memo " + memo.getId());
		} else {
			logger.debug("userContext is null");
			if (memo.getCreatedDate() != null) {
				String creationDate = commonUtil.getDateTimeWithTimeZone(memo.getCreatedDate(), "Asia/Kolkata");
				memoDto.setCreationDate(creationDate);
				logger.info("Set creationDate on memo for user " + memo.getCreatedById() + ", memo " + memo.getId());
			}
		}

		User creator = null;
		if (memo.getCreator() == null) {
			logger.info("creator is null..getting from cache...with creatorId from memo:" + memo.getCreatedById());
			creator = cacheService.getUser(memo.getCreatedById(), true);
		} else {
			logger.info("creator is not null");
			creator = new User();
			creator.setEmail(memo.getCreator().getEmail());
			if (memo.getCreator().getPhotoURL() != null) {
				logger.debug("setting user PhotoURL");
				creator.setPhotoURL(memo.getCreator().getPhotoURL());
			}
			logger.debug("setting creator name and Id");
			creator.setName(memo.getCreator().getName());
			creator.setId(memo.getCreatedById());

		}
		// for not using PhotoUrl and use the profile and thumbnail variables in creator
		// object
		UserPhoto userphoto = creator.getPhotoURL();
		String profile = "";
		String thumbnail = "";
		if (userphoto != null) {
			thumbnail = (userphoto.getThumbnail() == null) ? "" : userphoto.getThumbnail();
			profile = (userphoto.getProfile() == null) ? "" : userphoto.getProfile();
		}

		logger.debug("setting user thumbnail photo:" + thumbnail + " and profile photo:" + profile);
		creator.setThumbnail(thumbnail);
		creator.setProfile(profile);
		// TODO::Need to uncomment below stmnt. when android update goes to Prod
		// creator.setPhotoURL(null);

		memoDto.setCreator(creator);
		logger.debug("Set cretor on memo for user " + memo.getCreatedById() + ", memo " + memo.getId());

	}

	private void unescapeMemoText(MemoDto memoDto) {
		if (memoDto.getText() != null && !memoDto.getText().isEmpty()) {
			logger.debug("Memo text unescaping html for mobile : " + memoDto.getText());
			memoDto.setText(CommonUtil.unEscapeHtmlForMobile(memoDto.getText()));
			logger.debug("Memo text unescaped html for mobile : " + memoDto.getText());
		}
	}

	private void sendCreateChannelMessageEvent(Memo memo) {
		logger.info("sending CreateChannelMessage event ");
		Event event = new Event();
		event.setType(EventType.CreateMemo.getId());
		Map<String, String> data = new HashMap<String, String>();
		data.put("Id", memo.getId().toString());
		data.put("CreatedById", memo.getCreatedById().toString());
		data.put("RecipientIds", StringUtils.join(memo.getRecipientIds(), ","));
		data.put("Subject", memo.getSubject());
		event.setData(data);
		logger.debug("CreateChannelMessageEventData:" + event.toString());
		RmsActorSystem.getEventRouterActorRef().tell(event, null);
		logger.info("CreateChannelMessage event sent");
	}

	public void init() {

		errorHtmlPagePath = env.getProperty(Constants.PAGE_NOT_PUBLIC_URL);
		logger.info("errorHtmlPagePath=" + errorHtmlPagePath);

		followMsgTemplatePath = env.getProperty(Constants.FOLLOW_MESSAGE_TEMPLATE_PATH);
		logger.info("followMsgTemplatePath=" + followMsgTemplatePath);

		hostName = env.getProperty(Constants.HOST_NAME);
		logger.info("hostname=" + hostName);
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}
}