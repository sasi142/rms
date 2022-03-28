package core.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.daos.BulkMemoDumpDao;
import core.daos.MemoDao;
import core.daos.MemoRecipientDao;
import core.entities.BulkMemoDump;
import core.entities.Memo;
import core.entities.MemoChatUser;
import core.entities.MemoRecipient;
import core.entities.User;
import core.exceptions.ForbiddenException;
import core.exceptions.InternalServerErrorException;
import core.utils.Constants;
import core.utils.Enums.ChannelMessageSendingOptions;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.FileUploadType;
import core.utils.Enums.MemoStatus;
import core.utils.Enums.MemoType;
import core.utils.ThreadContext;

@Service

public class MemoServiceImpl implements MemoService, InitializingBean  {
	final static Logger logger = LoggerFactory.getLogger(MemoServiceImpl.class);

	@Autowired
	private MemoDao memoDao;

	@Autowired
	private BulkMemoDumpDao bulkMemoDumpDao;
	
	@Autowired
	private BulkMemoProcessService bulkMemoProcessService;

	@Autowired
	private MemoRecipientDao memoRecipientDao;

	@Autowired
	private UserConnectionService userConnectionService;

	@Autowired
	private Environment env;

	@Autowired
	private NotificationService notificationService;


	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;	

	private Integer numberOfFileProcessedInJob = 10;
	private Integer badgeCount = 10;

	public Memo getMemoDetails(Integer memoId, Boolean needSummary) {
		Integer currentUserId = ThreadContext.getUserContext().getUser().getId();		
		Memo memo = memoDao.getMemoDetailsById(memoId, needSummary, currentUserId);
		return memo;
	}

	public Memo getMemoDetailsV2(Integer memoId, Boolean needSummary) {
		logger.info(" in getMemoDetailsV2 ");
		Integer currentUserId = ThreadContext.getUserContext().getUser().getId();
		Memo memo = memoDao.getMemoDetailsV2(memoId, needSummary, currentUserId);
		logger.debug("memo details:" + memo.toString());
		logger.info("getMemoDetailsV2 ends");
		return memo;
	}

	@SuppressWarnings("unlikely-arg-type")
	@Transactional
	public Memo createMemo(Memo inMemo) {
		Integer userId = ThreadContext.getUserContext().getUser().getId();
		//Integer userId = 440;
		logger.info("Create ChannelMessage for user " + userId );
		logger.debug("Get detils for user " + userId+" from cache");
		User user = cacheService.getUser(userId, false);
		String memoText = inMemo.getMessage();
		inMemo.setCreatedById(userId);
		inMemo.setOrganizationId(user.getOrganizationId());
		// TODO: take length of random string from property and look for better random
		// string generation logic for uniqueness.
		String publicURL = RandomStringUtils.randomAlphanumeric(32);
		inMemo.setPublicURL(publicURL);
		logger.debug("inMemo.getMessage().size:"+inMemo.getMessage().length());
		logger.debug("inMemo.getMessage() " + inMemo.getMessage());
		String snippet = getSnippet(inMemo);
		logger.debug("Got Snippet Length:" + snippet.length());
		inMemo.setSnippet(snippet);
		inMemo.setMemoType(MemoType.RegulerMemoUserSelection.getId().byteValue());
		if (inMemo.getSendToAllEmployeeAndPartner()) {
			logger.info("Get partner userIds");
			Integer orgId = user.getOrganizationId();
			JsonNode orgNode = cacheService.getOrgJson(orgId);
			Integer partnerOrgId =orgNode.findPath("partnerEmployeeAssociationId").intValue();			
			List<Long> partnerIds1 = getRecipientList(partnerOrgId);
			logger.info("Got partnerUsers of size " + (partnerIds1 == null ? null : partnerIds1.size()));
			List<Long> orgIds1 = getRecipientList(orgId);
			logger.info("Got OrgUserIds of size " + (orgIds1 == null ? null : orgIds1.size()));
			partnerIds1.addAll(orgIds1);
			logger.info("Got total partner & employees of size " + (partnerIds1 == null ? null : partnerIds1.size()));
			inMemo.setRecipientIds(partnerIds1);
		} else if (inMemo.getSendToAllPartner()) {
			logger.info("Get partner userIds");
			Integer orgId = user.getOrganizationId();
			JsonNode orgNode = cacheService.getOrgJson(orgId);
			Integer partnerOrgId =orgNode.findPath("partnerEmployeeAssociationId").intValue();			
			List<Long> partnerIds1 = getRecipientList(partnerOrgId);
			logger.info("Got OrgUserIds of size " + (partnerIds1 == null ? null : partnerIds1.size()));
			partnerIds1.add(Long.valueOf(user.getId()));
			inMemo.setRecipientIds(partnerIds1);
		} else if (inMemo.getSendToAll()) {
			logger.info("Get OrgUserIds");
		    List<Long> orgUserIds = getRecipientList(user.getOrganizationId());
		    logger.info("Got OrgUserIds of size " + (orgUserIds == null ? null : orgUserIds.size()));
			inMemo.setRecipientIds(orgUserIds);
		} else if (inMemo.getRecipientIds() == null || inMemo.getRecipientIds().isEmpty()) {
			List<Long> orgUserIds = new ArrayList<Long>();
			orgUserIds.add(userId.longValue());
			inMemo.setRecipientIds(orgUserIds);
		} else if (inMemo.getRecipientIds() != null && !inMemo.getRecipientIds().contains(userId)) {
			inMemo.getRecipientIds().add(userId.longValue());
		}
		inMemo.setSendAs((byte) 0);
		logger.debug("Calling Create memo for user " + userId);
		Memo memo = memoDao.createMemo(inMemo);

		logger.info("Created memo for user " + userId + " with memo " + memo.getId());
		return memo;
	}
	
	private List<Long> getRecipientList(Integer orgId) {
		List<Integer> orgUserIds = cacheService.getAllOrgUserIds(orgId);
		logger.info("list size  is " + (orgUserIds == null ? null : orgUserIds.size()));
		List<Long> orgUserIds1 = orgUserIds.stream().map(s -> Long.valueOf(s)).collect(Collectors.toList());
		logger.info("list size  is " + (orgUserIds1 == null ? null : orgUserIds1.size()));
		return orgUserIds1;
	}

	@Transactional
	public Memo createChannelMessage(Memo inMemo, Integer channelId) {
		logger.info("in createChannelMessage service layer ");
		Memo createdMemo = null;
		Integer userId = ThreadContext.getUserContext().getUser().getId();
		logger.info("Create ChannelMessage for user " + userId);
		logger.debug("get userDetails from cache for userId:"+userId);
		User user = cacheService.getUser(userId, false);
		inMemo.setCreatedById(userId);
		inMemo.setOrganizationId(user.getOrganizationId());
		inMemo.setChannelId(channelId);
		// TODO: take length of random string from property and look for better random
		// string generation logic for uniqueness.
		String publicURL = RandomStringUtils.randomAlphanumeric(8);
		inMemo.setPublicURL(publicURL);
		logger.debug("inMemo.getMessage():" + inMemo.getMessage());
		String snippet = getSnippet(inMemo);
		logger.debug("Got Snippet Length:" + snippet.length());
		inMemo.setSnippet(snippet);
		Integer uploadId = inMemo.getUploadId();
		if (inMemo.getSendToAll()) {
			logger.debug("send to all followers");
			inMemo.setSendAs((byte) ChannelMessageSendingOptions.SendToAll.getId());

		} else if (!Objects.isNull(inMemo.getRecipientIds())) {
			inMemo.setSendAs((byte) ChannelMessageSendingOptions.SendToSpecificIds.getId());
			logger.debug("Creating Messages for ReceiptentList populated by UI");
		} else if (!Objects.isNull(uploadId) && uploadId > 0) {
			logger.debug("sending message to excel uploaded ids");
			inMemo.setSendAs((byte) ChannelMessageSendingOptions.SendToSpecificIds.getId());
		}

		createdMemo = memoDao.createMemo(inMemo);
		logger.info("Created channelMessage for user " + userId + " with ChannelMessageId " + createdMemo.getId());
		return createdMemo;
	}


	public List<Memo> getMemosByOrgId(Integer offset, Integer limit) {
		Integer userId = ThreadContext.getUserContext().getUser().getId();
		User user = cacheService.getUser(userId, false);
		logger.info("Get Memos for user " + user.getId() + " and org " + user.getOrganizationId());
		List<Memo> memos = memoDao.getMemosByOrgId(userId, user.getOrganizationId(), offset, limit);
		logger.info("Get Memos for user " + user.getId() + " and org " + user.getOrganizationId()
		+ " returned memo list of size " + (memos == null ? null : memos.size()));
		return memos;
	}

	public List<Memo> getMemosByUserIdV2(Integer inUserId, Integer channelId, Integer offset, Integer limit) {
		Integer userId = ThreadContext.getUserContext().getUser().getId();
		logger.debug("getting user details from cache for userId:"+userId);
		User user = cacheService.getUser(userId, false);
		logger.info("Get ChannelMessages for user " + user.getId() + " and input user " + inUserId + "from DB");
		List<Memo> memos = memoDao.getMemosByUserIdV2(user.getId(), channelId, offset, limit);
		logger.debug("total ChannelMessages returned from DB are :" + memos.size());
		return memos;
	}

	public List<Memo> getMemosByUserId(Integer inUserId, Integer offset, Integer limit) {

		Integer userId = ThreadContext.getUserContext().getUser().getId();
		User user = cacheService.getUser(userId, false);
		logger.info("Get Memos for user " + user.getId() + " and input user " + inUserId);
		// ChannelId is set to 0...means channelId is not required
		List<Memo> memos = memoDao.getMemosByUserIdV2(user.getId(), 0, offset, limit);
		logger.info("Get Memos for user " + user.getId() + " and input user " + inUserId
				+ " returned memo list of size " + (memos == null ? null : memos.size()));
		return memos;
	}

	@Transactional
	public void changeReadStatus(Integer memoId, Integer userId, Boolean readStatus) {
		logger.info("User " + userId + " changing Read status of memo " + memoId + " to " + readStatus);
		memoRecipientDao.changeReadStatus(memoId, userId, readStatus);
		logger.info("User " + userId + " changed Read status of memo " + memoId + " to " + readStatus);
	}

	@Transactional
	public void updateMemoPublicState(Integer memoId, Boolean isPublic) {
		logger.info("changing public state of memo " + memoId + " to " + isPublic);
		memoDao.updateMemoPublicState(memoId, isPublic);
		logger.info("changed public state of memo " + memoId + " to " + isPublic);
	}

	
	public Long getMemoCountByStatus(Integer userId, Boolean readStatus) {
		logger.info("User " + userId + " Getting Memo Count whose Read status is " + readStatus);
		Long count = memoRecipientDao.getMemoCountByStatus(userId, readStatus);
		logger.info("User " + userId + " Got Memo Count as " + count + "whose Read status is " + readStatus);
		return count;
	}

	public List<MemoChatUser> getMemoChatUsers(Integer memoId, Integer offset, Integer limit) {
		logger.info("Get Chat users for memo " + memoId);
		List<MemoChatUser> chatUsers = memoDao.getMemoChatUsers(memoId, offset, limit);
		logger.info("Get Chat users for memo " + memoId);
		return chatUsers;
	}


	@Override	
	public void processBulkMemo() {		
		logger.info("Get ready to process memo from database.");
		for(int index=1; index <= numberOfFileProcessedInJob; index ++) {
			BulkMemoDump memoDump = bulkMemoProcessService.getMemoDump();
			if(memoDump.getId() != null) {
				logger.info("createorById: "+memoDump.getCreatedById());	 
				
				logger.info("Process started for memoDumpId:  "+memoDump.getId());
				if(MemoType.RegulerMemoUserSelection.getId().intValue() == memoDump.getUploadType().intValue() &&
						memoDump.getTotalMemoSent() == 1) {
					logger.info("memo already processed, mark status in db: ", memoDump.getCreatedById());
					bulkMemoProcessService.updateBulkMemoDumpStatus(memoDump);
				}
				
				LocalDateTime startDate = LocalDateTime.now();		
				logger.info("Process memo started at time: " + startDate);
				
				List<ObjectNode> memoList = bulkMemoProcessService.getMemoListForProcess(memoDump);
				logger.info("memoList: "+memoList.toString());
				logger.info("List of memo returned from excel:  "+memoList.size());
				Integer memoCount = memoList.size();
			    Integer innitialCount= memoDump.getTotalMemoSent();			
			    processMemo(memoDump, memoList, memoCount, innitialCount);	
				bulkMemoProcessService.updateBulkMemoDumpStatus(memoDump);
				LocalDateTime endDate = LocalDateTime.now();		
				logger.info("Process memo end at time: " + endDate);
			} else {
				logger.info("No any ready to process, entry found in database ");
				break;
			}
		}
		logger.info("Memo process done.");
	}

	public void processMemo(BulkMemoDump memoDump, List<ObjectNode> memoList, Integer memoCount, Integer innitialCount) {
		List<ObjectNode> subList= new ArrayList<ObjectNode>();
		Integer count=memoDump.getTotalMemoSent();
		for(int index1= innitialCount; index1<= memoCount; index1=index1++) {			//count = count + badgeCount;
	
			Integer remainCount = memoCount - count;			
			if(remainCount > badgeCount) {
				logger.info("get inntial index: "+count);
				Integer innitialIndex = count;
				count = count+badgeCount;
				logger.info("get start index: "+innitialIndex+" end Index: "+count);
				subList = new ArrayList<>(memoList.subList(innitialIndex, count));	
				logger.info("get end index: "+count);
				List<BulkMemoDump> updatedMemoDump = bulkMemoProcessService.createMemoFromExcel(memoDump, subList);
				bulkMemoProcessService.sendCreateMemoEventToRecipients(updatedMemoDump);
				
			} else {
				subList = new ArrayList<>(memoList.subList(count, memoCount));	
				logger.info("get start index: "+count+" end Index: "+memoCount);
				logger.info("********* Last Patch *********");
				List<BulkMemoDump> updatedMemoDump = bulkMemoProcessService.createMemoFromExcel(memoDump, subList);
				bulkMemoProcessService.sendCreateMemoEventToRecipients(updatedMemoDump);
				break;
			}
			logger.info("-----------loop is running--------");	
			}		
		 	
	}
	
	
	
	@Override
	@Transactional
	public Integer createMemoFileDetailsInDump(String filePath, String fileName, Byte uploadType ) {
		logger.info("store Upload memo details in memo dump");
	Integer currentUserId = ThreadContext.getUserContext().getUser().getId();
		Integer orgId = ThreadContext.getUserContext().getUser().getOrganizationId();		
		try {			 
			BulkMemoDump memoDump = new BulkMemoDump();	
			memoDump.setFileName(fileName);
			memoDump.setFilePath(filePath);
			memoDump.setUploadType(uploadType);
			if(FileUploadType.UserMemo.getId().byteValue() == uploadType) {
				memoDump.setStatus(MemoStatus.Initiated.getId().byteValue());
			} else {
				memoDump.setStatus(MemoStatus.ReadyToProcess.getId().byteValue());
			}
			logger.info("status: "+memoDump.getStatus());
			Long currentDate =System.currentTimeMillis();
			memoDump.setCreatedDate(currentDate);
			memoDump.setUpdatedDate(currentDate);
			memoDump.setCreatedById(currentUserId);
			memoDump.setOrganizationId(orgId);
			memoDump.setActive(true);
			memoDump.setIsPublic(false);
			memoDump.setTotalMemoSent(0);
			memoDump.setShowUserDetailOnSCP(false);
			
			bulkMemoDumpDao.create(memoDump);
			logger.info("memoDump Id: "+memoDump.getId());
			return memoDump.getId();
		} catch (Exception e) {
			logger.info("uploading file.."+e);
			throw new InternalServerErrorException(ErrorCode.Invalid_Data, "Error while create Memo Dump at DB");
		}
	}

	@Override
	@Transactional
	public void updateUserMemoDetailsInDump(Memo inMemo) {
		logger.info("store Upload memo details in memo dump");

		BulkMemoDump memoDump = bulkMemoDumpDao.findOne(inMemo.getMemoDumpAttachmentId());	
		if(FileUploadType.UserMemo.getId().byteValue() != memoDump.getUploadType().byteValue()) {
			throw new ForbiddenException(ErrorCode.Invalid_Memo_Dump_AttachmentId, "upload Type of givenEnity is not UserMemo");
		}
		
		if(!memoDump.getActive() || MemoStatus.Initiated.getId().byteValue() != memoDump.getStatus().byteValue()) {
			throw new ForbiddenException(ErrorCode.Invalid_Memo_Status, "memo status should be innitiated");
		}		
	
			Integer userId = ThreadContext.getUserContext().getUser().getId();
		//Integer userId=440;
			logger.info("logged in userId: "+userId +" memo dump created byId: "+memoDump.getCreatedById());
			if(!memoDump.getCreatedById().equals(userId)) {
				logger.info("logged in userId & memo dumpId are not similer");
				throw new ForbiddenException(ErrorCode.Invalid_Memo_Dump_AttachmentId, "update entry is not allowed for given user");
			}
		try {
			memoDump.setSubject(inMemo.getSubject());
			memoDump.setMessage(inMemo.getMessage());
			memoDump.setSnippet(getSnippet(inMemo));
			memoDump.setIsPublic(inMemo.getIsPublic());
			memoDump.setShowUserDetailOnSCP(inMemo.getShowUserDetailOnSCP());
			Long currentDate =System.currentTimeMillis();			
			memoDump.setUpdatedDate(currentDate);
			if(inMemo.getAttachmentIds()!= null) {
				String attachments = inMemo.getAttachmentIds().toString();
				memoDump.setAttachments(attachments);
			}
			memoDump.setStatus(MemoStatus.ReadyToProcess.getId().byteValue());
			bulkMemoDumpDao.update(memoDump);		
			logger.info("memoDump Id: "+memoDump.getId());			
		} catch (Exception e) {
			logger.info("Update Memo Dump Failed."+e);
			throw new InternalServerErrorException(ErrorCode.Invalid_Data, "Error while update Memo Dump at DB");

		}				
	}

	@Transactional
	public Memo getMemoByPublicURL(String url) {
		logger.info("get memo with public url : " + url);
		Memo memo = memoDao.getMemoByPublicURL(url);
		memoDao.updateMemoPageViewCount(memo.getId());
		logger.info("get memo with public url : " + url + " and details : " + memo);
		return memo;
	}

	@Transactional
	public Memo getMessagePublicPage(String url) {
		logger.info("in MemoService getMessagePublicPage for url" + url);
		Memo memo = memoDao.getMessagePublicPage(url);
		logger.debug("message Public Details:" + memo.toString());
		return memo;
	}

	private String getSnippet(Memo inMemo) {
		logger.info("getting snippet");
		String memoText = inMemo.getMessage();
		return getSnippet(memoText);
	}

	private String getSnippet(String memoText) {
		String snippet = "";
		String snippetDelimiter = "...";
		Elements pTags = Jsoup.parse(memoText).select("p");
		for (Element pTag : pTags) {
			if (pTag != null && pTag.hasText()) {
				snippet = snippet + pTag.text();
				if (snippet.length() > 150) {
					snippet = snippet.substring(0, 150);
					break;
				}
			}
		}
		snippet = snippet + snippetDelimiter;
		logger.info("returning snippet");
		return snippet;
	}

	@Override
	public Integer isReceipient(Integer memoId, Integer userId) {
		Integer count = memoRecipientDao.isReceipient(memoId,userId);
		logger.debug("returning count="+count);
		return count;
	}
	
	@Override
	public String getCustomMemoReportById(Integer memoId) {		
		return memoDao.getMemoReportById(memoId);
	}
	
	
	@Override
	public String getRegularMemoReportById(Integer memoId) {
		return memoDao.getRegularMemoReportById(memoId);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		numberOfFileProcessedInJob = Integer.valueOf(env.getProperty(Constants.NUMBER_OF_FILE_PROCESSED_IN_JOB));
		logger.info("numberOfFileProcessedInJob= "+numberOfFileProcessedInJob);
		badgeCount= Integer.valueOf(env.getProperty(Constants.MEMO_PROCESS_BADGE_COUNT));
		logger.info("badgeCount= "+badgeCount);
	}

	

}


