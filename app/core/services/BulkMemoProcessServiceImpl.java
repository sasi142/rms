package core.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums.ChannelMessageSendingOptions;
import core.utils.Enums.ErrorCode;
import core.utils.Enums.EventType;
import core.utils.Enums.FileUploadType;
import core.utils.Enums.MemoStatus;
import core.utils.Enums.MemoType;
import core.utils.Enums.MessageType;
import core.utils.Enums.NotificationType;
import core.utils.ThreadContext;
import play.libs.Json;

@Service
@Transactional(rollbackFor = { Exception.class })
public class BulkMemoProcessServiceImpl implements BulkMemoProcessService {
	final static Logger logger = LoggerFactory.getLogger(MemoServiceImpl.class);

	@Autowired
	private BulkMemoDumpDao bulkMemoDumpDao;

	@Autowired
	private UserConnectionService userConnectionService;

	@Autowired
	private NotificationService notificationService;


	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;	  

	@Override
	public List<ObjectNode> getMemoListForProcess(BulkMemoDump memoDump) {
		logger.info("path is: "+memoDump.getFilePath());		
		List<ObjectNode> memoList =new ArrayList<ObjectNode>();
		String systemPath = FilenameUtils.separatorsToSystem(memoDump.getFilePath());
		File newFile = new File(systemPath);
		logger.info("File received from given path with name "+newFile.getName());
		try {	
			if(FileUploadType.UserMemo.getId().byteValue() == memoDump.getUploadType().byteValue()) {	
				logger.info("MemoDump Type is: "+memoDump.getUploadType());
				ObjectNode node = readUserMemoXLFile(newFile, memoDump);
				memoList.add(node);
			} else {
				logger.info("MemoDump Type is: "+memoDump.getUploadType());
				memoList = readCustomMemoXLFile(newFile, memoDump);
			}	
		} catch (Exception e) {
			logger.info("Exception occured while reading content From excel: "+e);
		}
		logger.info("Get Chat users for memo ");
		return memoList;
	}



	@Override
	public void updateBulkMemoDumpStatus(BulkMemoDump memoDump) {
		bulkMemoDumpDao.updateBulkMemoDumpStatus(memoDump.getId(), MemoStatus.Processed.getId().byteValue());
	}


	@Override
	public List<BulkMemoDump> createMemoFromExcel(BulkMemoDump memoDump, List<ObjectNode> memoList) {
		List<BulkMemoDump> updatedMemoDump = new ArrayList<BulkMemoDump>();
		try {
			updatedMemoDump = bulkMemoDumpDao.createMemoFromMemoDump(memoDump.getOrganizationId(), memoDump.getCreatedById(), memoList);
			//   Integer sendmemoCount = memoList.size() + 1;
			logger.info("sendmemoCount: "+memoList.size());
			bulkMemoDumpDao.updateSentMemoCount(memoDump.getId(), memoList.size());
		} catch(Exception e) {
			logger.error("Failed to create memeo : "+e);
		}
		return updatedMemoDump;
	}


	@Override
	public BulkMemoDump getMemoDump() {
		return bulkMemoDumpDao.getMemoDump();
	}

	@Override
	public void sendCreateMemoEventToRecipients(List<BulkMemoDump> updatyedMemoDump) {
		for(BulkMemoDump memodump: updatyedMemoDump) {		
			logger.info("send CreateMemo event started");
			ObjectNode node = Json.newObject();
			node.put("type", MessageType.Event.getId());
			node.put("subtype", EventType.CreateMemo.getId());

			ObjectNode data = Json.newObject();
			data.put("memoId", memodump.getId().toString());
			data.put("alert", "New Memo: " + memodump.getSubject());
			data.put("senderId", memodump.getCreatedById());
			logger.info("creator Name is : "+memodump.getFirstName());
			data.put("name", memodump.getFirstName());
			node.put("data", data);

			logger.info("memo Object is : "+node.toString());
			ObjectNode pushNotification = Json.newObject();
			data.put("type", MessageType.Notification.getId());
			data.put("subtype", NotificationType.Memo.getId());
			ObjectNode aps = Json.newObject();
			aps.put("alert", "New Memo: " + memodump.getSubject());
			aps.put("sound", "default");

			pushNotification.set("aps", aps);
			pushNotification.set("data", data);

			Set<Integer> recipents =memodump.getRecipientIds();															
			logger.info("Create memo event recipient list : "+recipents.toString());
			List<Integer> msgReceivedUserIds= userConnectionService.sendMessageToActorSet(recipents, node, null);	
			logger.info("Create memo push Notification recipient list : "+recipents.toString());
			notificationService.sendMobilePushNotification(msgReceivedUserIds, pushNotification, NotificationType.Memo);
		}
	}



	@SuppressWarnings("resource")
	public List<ObjectNode> readCustomMemoXLFile(File file, BulkMemoDump memoDump) {
		logger.info("read xlsx file started");

		List<ObjectNode> memoList =new ArrayList<ObjectNode>();
		createCustomMemoForSummary(memoList, memoDump);
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
			XSSFWorkbook wb = new XSSFWorkbook(inputStream);
			XSSFSheet sheet = wb.getSheetAt(0);
			logger.info("sheet last roww Number is: " + sheet.getLastRowNum());		
			for (int index = 1; index <= sheet.getLastRowNum(); index++) {				
				XSSFRow row = sheet.getRow(index);
				logger.info("row: " + row);	
                if(row == null) {
                	break;
                }
				ObjectNode node = Json.newObject();	
				ObjectMapper mapper = new ObjectMapper();
				ArrayNode recipients =mapper.createArrayNode();
				//	List<String> recipients = new ArrayList<String>();
				try {
					String value = getCellValue(row.getCell(0));
					if(!value.isBlank()) {
						recipients.add(getCellValue(row.getCell(0)));				
						logger.info("recipients: " + recipients);		
						node.put("recipients", recipients.toString());
						node.put("subject", getCellValue(row.getCell(1)));
						node.put("message", getCellValue(row.getCell(2)));
						String snippet = getSnippet(getCellValue(row.getCell(2)));
						node.put("snippet", snippet);		
						node.put("isPublic", ReturnIntegerValue(row.getCell(3)));
						node.put("id", memoDump.getId());	
						node.put("memoType", MemoType.CustomMemo.getId());	
						node.put("showUserDetailOnSCP ",false);
						logger.info("node: " + node);		
						memoList.add(node);
					} else {
						logger.info("username value is blank. ");
						break;
					}
				}  catch (Exception e) {			
					logger.info(" need to add this memo in failure list " + e);
					
				}
			}			
			logger.info("memo List: " + memoList);

		} catch (Exception e) {			
			logger.info("error is: " + e);
		}
		logger.debug(
				"file reading done. valid memoList size: " + memoList.size());
		return memoList;
	}

	
	private void createCustomMemoForSummary(List<ObjectNode> memoList, BulkMemoDump memoDump) {	
		 logger.info("create summary custom memo started : ");		 
	             logger.info("userId is: " + memoDump.getCreatedById());
	           
	            	ObjectNode node = Json.newObject();	
					ObjectMapper mapper = new ObjectMapper();
					ArrayNode recipients =mapper.createArrayNode();		
					 User user= cacheService.getUser(memoDump.getCreatedById(), true);
					recipients.add(user.getEmail());
					logger.info("recipients: " + recipients);		
					node.put("recipients", recipients.toString());				
					SimpleDateFormat formatter= new SimpleDateFormat("dd MMM YYYY");
					Date date1 = new Date(System.currentTimeMillis());
					String date= formatter.format(date1);
					logger.info("date: "+ date);
					node.put("subject", Constants.CUSTOM_MEMO_SUBJECT+ date);
					String message = Constants.CUSTOM_MEMO_CONTENT+ date;
					node.put("message", message);
					String snippet = getSnippet(message);
					node.put("snippet", snippet);						
					node.put("isPublic", 0);
					node.put("memoType", MemoType.CustomMemoSummary.getId());
					node.put("id", memoDump.getId());	
					node.put("showUserDetailOnSCP ", 0);
					logger.info("node: " + node);
					memoList.add(node);					
			}
	
	public ObjectNode readUserMemoXLFile(File file, BulkMemoDump memoDump) {
		logger.info("read xlsx file started");
		ObjectNode node = Json.newObject();	
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode recipients =mapper.createArrayNode();
		//	List<String> recipients = new ArrayList<String>();			
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(file);
			XSSFWorkbook wb = new XSSFWorkbook(inputStream);
			XSSFSheet sheet = wb.getSheetAt(0);				
			for (Row row : sheet) {
				XSSFCell cell =  (XSSFCell) row.getCell(0);
				if(getCellValue(cell) != null) {
					recipients.add(getCellValue(cell));
				} else {
					logger.info("username value is blank. ");
					break;
				}				 
			}	
			//Sender should be bydefault added in recipient list
			 User user= cacheService.getUser(memoDump.getCreatedById(), true);
			recipients.add(user.getEmail());
			node.put("recipients", recipients.toString());
			node.put("subject", memoDump.getSubject());
			node.put("message", memoDump.getMessage());			
			node.put("snippet", memoDump.getSnippet());				
			node.put("attachmentIds", memoDump.getAttachments());
			node.put("memoType", MemoType.RegulerMemoExcelSelction.getId());
			Integer showUserDetailOnScp = (memoDump.getShowUserDetailOnSCP()) ? 1 :0;		
			node.put("showUserDetailOnSCP", showUserDetailOnScp);
			Integer isPublic = (memoDump.getIsPublic()) ? 1 :0;
			node.put("isPublic", isPublic);
			node.put("id", memoDump.getId());	
			logger.info("node: " + node);	

		} catch (Exception e) {			
			logger.info("error is: " + e);
		}
		logger.debug("file reading done. valid usernameList: "+recipients.size());
		return node;
	}


	public String getSnippet(String memoText) {
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

	public String getCellValue(XSSFCell cell) {			

		String value ="";
		if (cell != null && cell.getCellTypeEnum() != CellType.BLANK ) {
			DataFormatter dataFormatter = new DataFormatter();
			value = dataFormatter.formatCellValue(cell).trim();
			logger.info("cell Value: " + value);		 
			return value;
		}
		logger.info("cell Value: " + value);	
		return value;
	}

	private Integer ReturnIntegerValue(XSSFCell cell) {
		Integer isPublic= 0;
		String sharingEnable = getCellValue(cell);
		logger.info("sharingEnable: "+sharingEnable);
		if (sharingEnable != null ) {
			if(sharingEnable.equalsIgnoreCase("On")) {
				isPublic = 1;
			}		
		}
		return isPublic;
	}



}


