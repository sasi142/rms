/**
 * 
 */
package core.daos.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import core.daos.GroupChatDao;
import core.entities.Attachment;
import core.entities.GroupChat;
import core.entities.MessageReadInfo;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums.ErrorCode;
import core.utils.PropertyUtil;
import core.utils.Constants;
import javax.persistence.*;



/**
 * @author Chandramohan.Murkute
 */
@Repository
public class GroupChatDaoImpl extends AbstractJpaDAO<GroupChat> implements GroupChatDao {
	
	final static Logger logger = LoggerFactory.getLogger(GroupChatDaoImpl.class);
	
    @PersistenceContext(unitName = "readEntityManagerFactory")
    protected EntityManager readEntityManager;

	public GroupChatDaoImpl() {
		super();
		setClazz(GroupChat.class);
	}

	@Override
	public GroupChat createGroupChatHistory(final GroupChat groupChat) {
		GroupChat insertedGroupChat = null;
		logger.debug("create groupChat entity withe input P_SenderId:" + groupChat.getSenderId()+ "P_GroupId:"+groupChat.getGroupId()
		+"P_MessageType:"+groupChat.getChatMessageType()+"P_MemberId:"+groupChat.getMemberId()+"P_EventType:"+groupChat.getEventNotificationType()+"P_GroupType:"+groupChat.getGroupType());

		
		try {
			StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("GroupChat.CreateGroupChat");
			spQuery.setParameter("P_SenderId", groupChat.getSenderId());
			spQuery.setParameter("P_GroupId", groupChat.getGroupId());
			spQuery.setParameter("P_Message", groupChat.getText());
			spQuery.setParameter("P_Data", groupChat.getData() == null ? "" : groupChat.getData());
			spQuery.setParameter("P_MessageType", groupChat.getChatMessageType());
			spQuery.setParameter("P_CreatedDate", groupChat.getCreatedDate());
			spQuery.setParameter("P_MemberId", groupChat.getMemberId());
			spQuery.setParameter("P_EventType", groupChat.getEventNotificationType());
			spQuery.setParameter("P_ParentMessageId", groupChat.getParentMsgId() == null ? 0 : groupChat.getParentMsgId());			
			spQuery.setParameter("P_ExcludeFromMsgRecipient", groupChat.getExcludedRecipients() == null? "" : groupChat.getExcludedRecipients());
			spQuery.setParameter("P_GroupType", groupChat.getGroupType() == null? "" : groupChat.getGroupType());

			insertedGroupChat = (GroupChat) spQuery.getSingleResult();
			logger.debug("created groupChat entity in db with id = " + insertedGroupChat.getId());
		} catch (Exception e) {
			logger.error("Failed to insert group chat ", e);
			throw e;
		}
		return insertedGroupChat;
	}

	@Override
	public GroupChat createVideoKycGroupChatHistory(final GroupChat groupChat) {
		GroupChat insertedGroupChat = null;
		logger.debug("create groupChat entity withe input P_SenderId:" + groupChat.getSenderId()+ "P_GroupId:"+groupChat.getGroupId()
		+"P_MessageType:"+groupChat.getChatMessageType()+"P_MemberId:"+groupChat.getMemberId()+"P_EventType:"+groupChat.getEventNotificationType()+"P_GroupType:"+groupChat.getGroupType());

		
		try {
			StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("GroupChat.CreateVideoKycGroupChat");
			spQuery.setParameter("P_SenderId", groupChat.getSenderId());
			spQuery.setParameter("P_GroupId", groupChat.getGroupId());
			spQuery.setParameter("P_Message", groupChat.getText());
			spQuery.setParameter("P_Data", groupChat.getData() == null ? "" : groupChat.getData());
			spQuery.setParameter("P_MessageType", groupChat.getChatMessageType());
			spQuery.setParameter("P_CreatedDate", groupChat.getCreatedDate());
			spQuery.setParameter("P_MemberId", groupChat.getMemberId());
			spQuery.setParameter("P_EventType", groupChat.getEventNotificationType());
			spQuery.setParameter("P_ParentMessageId", groupChat.getParentMsgId() == null ? 0 : groupChat.getParentMsgId());			
			spQuery.setParameter("P_ExcludeFromMsgRecipient", groupChat.getExcludedRecipients() == null? "" : groupChat.getExcludedRecipients());
			spQuery.setParameter("P_GroupType", groupChat.getGroupType() == null? "" : groupChat.getGroupType());

			insertedGroupChat = (GroupChat) spQuery.getSingleResult();
			logger.debug("created groupChat entity in db with id = " + insertedGroupChat.getId());
		} catch (Exception e) {
			logger.error("Failed to insert group chat ", e);
			throw e;
		}
		return insertedGroupChat;
	}
	/*
	 * (non-Javadoc)
	 * @see core.daos.GroupChatDao#getChatHistory(java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.Integer)
	 */
	public List<GroupChat> getChatHistory(Integer groupId, Integer currentUserId, Boolean isMemberActive, Long lastMsgDate, Integer offset, Integer limit) {
		logger.debug("get chat history for group " + groupId);
		List<GroupChat> groupChats = new ArrayList<>();
		try {
			String strQuery = "SELECT gc.Id, gc.SenderId, gc.GroupId, gc.Message, gc.Data, gc.MessageType, pgcmr.GroupChatId AS ParentMessageId, gc.CreatedDate, gc.Active, "
					+ "( CASE WHEN pgcmr.Id IS NOT NULL THEN JSON_OBJECT('to', pgc.GroupId, 'text', pgc.Message, 'from', pgc.SenderId ,'mid', pgc.Id, "
					+ "'chatType', 1, 'chatMessageType', pgc.MessageType ,'utcDate', pgc.CreatedDate, "
					+ "'data', pgc.Data) ELSE NULL END ) AS ParentMsg, gc.ReadDate, gcmr.ReadDate AS MemberReadDate "
					+ "FROM Group_Chat gc INNER JOIN group_chat_msg_recipient gcmr ON gcmr.GroupChatId = gc.Id "
					+ "AND gcmr.RecipientId = :currentUserId AND gcmr.Active = TRUE "
					+ "LEFT OUTER JOIN group_chat pgc ON pgc.Id = gc.ParentMessageId "
					+ "LEFT OUTER JOIN group_chat_msg_recipient pgcmr ON pgcmr.GroupChatId = pgc.Id AND pgcmr.RecipientId = :currentUserId "
					+ "AND pgcmr.Active = TRUE AND ( CASE WHEN pgcmr.DeletedDate IS NOT NULL THEN FALSE ELSE pgc.Active END ) = TRUE "
					+ " where ( CASE WHEN gcmr.DeletedDate IS NOT NULL THEN FALSE ELSE gc.Active END ) = TRUE AND gc.groupId=:groupId AND gc.createdDate < :lastMsgDate ";
			if (!isMemberActive) {
				strQuery += "AND EXISTS (SELECT 1 FROM chat_user_last_message cu WHERE cu.chatType = 1 AND cu.senderId = :groupId AND cu.recipientId = :currentUserId AND gc.createdDate <= cu.leftDate)";
			}
			strQuery += " ORDER BY gc.createdDate DESC";

			Query query = readEntityManager.createNativeQuery(strQuery, "GroupChat.InsertGroupChatResultMappings");
			query.setParameter("currentUserId", currentUserId);
			query.setParameter("groupId", groupId);
			query.setParameter("lastMsgDate", lastMsgDate);
			// query.setParameter("userId", currentUserId);
			query.setFirstResult(offset);
			query.setMaxResults(limit);
			groupChats = query.getResultList();
			logger.debug("get chat history depatments. total entities found " + (groupChats == null ? null : groupChats.size()));
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.debug("chat history not found " + groupChats);
		}
		return groupChats;
	}

	@Override
	public void createGroupChatMsgRecipient(Integer groupId, Integer excludeConsumerId) {
		logger.debug("insert into group chat msg recipient for groupId: " + groupId);
		String strSQL = "INSERT INTO group_chat_msg_recipient(GroupChatId, RecipientId, Active, DeletedDate) "
				+ " SELECT	gc.Id, c.RecipientId, TRUE, ( CASE WHEN c.RecipientId = :excludeConsumerId THEN :currentDate ELSE NULL END )"
				+ " FROM group_chat gc INNER JOIN chat_user_last_message c ON c.ChatType = 1 AND c.SenderId = gc.GroupId"
				+ " AND gc.CreatedDate <= IFNULL(c.LeftDate,gc.CreatedDate) AND c.Active = TRUE WHERE gc.groupId = :groupId AND gc.Active = TRUE"
				+ " AND NOT EXISTS ( SELECT 1 FROM group_chat_msg_recipient gcr WHERE gcr.GroupChatId = gc.Id"
				+ " AND gcr.RecipientId = c.RecipientId AND gcr.Active = TRUE)";				
		Query query = entityManager.createNativeQuery(strSQL);
		query.setParameter("groupId", groupId);
   	    query.setParameter("excludeConsumerId", excludeConsumerId);
   		final long currentDate = System.currentTimeMillis();
   	    query.setParameter("currentDate", currentDate);				    	
      	query.executeUpdate();
		logger.debug("inserted into group chat msg recipient for groupId: " + groupId);
	}

	@Override
	public List<MessageReadInfo> getGroupChatMsgReadInfo(Long msgId, Integer currentUserId) {
		logger.debug("getGroupChatMsgReadInfo for message : " + msgId);
		List<MessageReadInfo> msgReadInfoList = new ArrayList<MessageReadInfo>();
		String strQuery = "SELECT gcm.RecipientId, gcm.ReadDate, ( CASE WHEN cu.LeftDate IS NULL THEN 1 ELSE 2 END ) AS MemberStatus "
				+ " FROM group_chat gc  INNER JOIN group_chat_msg_recipient gcm  ON gcm.GroupChatId = gc.Id AND gcm.RecipientId <> gc.SenderId  "
				+ " INNER JOIN chat_user_last_message cu ON cu.ChatType = 1 AND cu.SenderId = gc.GroupId AND cu.RecipientId = gcm.RecipientId  "
				+ " WHERE gc.Id = :msgId AND gc.SenderId = :currentUserId order by gcm.ReadDate desc";
		// Query query = entityManager.createNativeQuery(strQuery);
		Query query = entityManager.createNativeQuery(strQuery);
		query.setParameter("msgId", msgId);
		query.setParameter("currentUserId", currentUserId);

		List<Object[]> resultSet = query.getResultList();
		if (resultSet != null) {
			for (Object[] row : resultSet) {
				MessageReadInfo msgInfo = new MessageReadInfo(Integer.valueOf(row[0].toString()), (row[1] == null) ? null : Long.valueOf(row[1].toString()), (row[2] == null) ? null : Byte
						.valueOf(row[2].toString()));
				msgReadInfoList.add(msgInfo);
			
			}

		}
		logger.debug("got GroupChat Msg ReadInfo for message : " + msgId + " as " + (msgReadInfoList == null ? null : msgReadInfoList.size()));

		return msgReadInfoList;
	}

	@Override
	public Long getLastMsgIdByGroup(Integer groupId, Integer recipientId) {
		logger.debug("get last massage Id for group " + groupId + " and recipient " + recipientId);
		Long msgId = null;
		try {
			String strQuery = "SELECT MAX(gc.Id) AS lastMsgId FROM group_chat gc INNER JOIN group_chat_msg_recipient gcm"
					+ " ON gcm.GroupChatId = gc.Id AND gcm.RecipientId = :recipientId AND gcm.ReadDate IS NULL AND gcm.Active = TRUE WHERE gc.GroupId = :groupId AND gc.Active = TRUE";
			Query query = entityManager.createNativeQuery(strQuery);
			query.setParameter("groupId", groupId);
			query.setParameter("recipientId", recipientId);
			BigInteger result = (BigInteger) query.getSingleResult();
			if (result != null) {
				msgId = result.longValue();
			}
			logger.debug("last massage Id " + msgId + " for group " + groupId + " and recipient " + recipientId);
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.info("No msgs found for recipient " + recipientId + " in group " + groupId);
		}
		return msgId;
	}

	@Override
	public List<Attachment> assignAttachmentRecipient(Long groupId, String attachmentId, String entityType ,Integer createdById) {
		logger.debug("insert into Attachment recipient for attachmentId: " + attachmentId);		
		List<Attachment> attachmentList = null;	
		List<Object[]> attachments =null;
	try {
		StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("GroupChat.AddAttachmentRecipient");
		spQuery.setParameter("P_AttachmentIds", attachmentId);
		spQuery.setParameter("P_GroupId", groupId);
		spQuery.setParameter("P_EntityType", entityType);
		spQuery.setParameter("P_CreatedById", createdById);	
		attachments = spQuery.getResultList();
		attachmentList =  getAttachmentList(attachments);
		logger.debug("created groupChat entity in db with ids = ");
	} catch (Exception e) {
		logger.error("Failed to insert group chat ", e);
		throw e;
	}
	return attachmentList;
 }

	@Override	
	public void updateWelcomeMessage(final GroupChat groupChat) {		
		logger.debug("create groupChat entity withe input " + groupChat);
		try {
			Query spQuery = entityManager.createNamedQuery("GroupChat.updateWelcomeMessage");
			//updateStatusQuery.setParameter("meetingStatus", meetingStatus);
			//StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("GroupChat.updateWelcomeMessage");

			spQuery.setParameter("senderId", groupChat.getSenderId());
			spQuery.setParameter("groupId", groupChat.getGroupId());
			spQuery.setParameter("chatMessageType", groupChat.getChatMessageType());
			spQuery.setParameter("createdDate", groupChat.getCreatedDate());
		    spQuery.executeUpdate();
			logger.debug("updated groupChat entity in db");
		} catch (Exception e) {
			logger.error("Failed to iupdategroup chat ", e);
			throw e;
		}
		
	}
	
	@Override
	public List<GroupChat> getChatHistoryByShortUri(Integer groupId, Integer offset, Integer limit) {
		logger.debug("get chat history for group using shartUri " + groupId);
		List<GroupChat> groupChats = new ArrayList<>();
		try {
			String strQuery = "SELECT gc.Id, gc.SenderId, gc.GroupId, gc.Message, gc.Data, gc.MessageType, pgc.Id AS ParentMessageId, gc.CreatedDate, gc.Active, "
					+"	( CASE WHEN pgc.Id IS NOT NULL THEN JSON_OBJECT('to', pgc.GroupId, 'text', pgc.Message, 'from', pgc.SenderId ,'mid', pgc.Id, " 
					+"	'chatType', 1, 'chatMessageType', pgc.MessageType ,'utcDate', pgc.CreatedDate, "
					+"	'data', pgc.Data) ELSE NULL END ) AS ParentMsg, gc.ReadDate, NULL AS MemberReadDate FROM Group_Chat gc "
				     +" LEFT OUTER JOIN group_chat pgc ON pgc.Id = gc.ParentMessageId AND pgc.DeletedForEveryone = 0 WHERE gc.GroupId = :groupId "
				 +" AND gc.DeletedForEveryone = 0 ORDER BY gc.createdDate DESC ";
			
			Query query = readEntityManager.createNativeQuery(strQuery, "GroupChat.InsertGroupChatResultMappings");			
			query.setParameter("groupId", groupId);			
			query.setFirstResult(offset);
			query.setMaxResults(limit);
			groupChats = query.getResultList();
			logger.debug("get chat history depatments. total entities found " + (groupChats == null ? null : groupChats.size()));
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.debug("chat history not found  by shortUri" + groupChats);
		}
		return groupChats;
	}

	
	private List<Attachment> getAttachmentList(List<Object[]> attachmentsObjList){
		List<Attachment> attachmentlist = new ArrayList<Attachment>();
		logger.debug("convert resultset into atatchment object");
		for(Object[] attachmentObj : attachmentsObjList) {
			Attachment attachment = new Attachment();
		if (attachmentObj != null) {
			logger.debug("received attachment data");		
			if(attachmentObj[0] != null) {
				attachment.setId(Long.valueOf(attachmentObj[0].toString()));
			}
			if(attachmentObj[1] != null) {
				attachment.setFileName(attachmentObj[1].toString());
			}
			if(attachmentObj[2] != null) {
				attachment.setFileSize(attachmentObj[2].toString());
			}
			if(attachmentObj[3] != null) {
				attachment.setThumbnailWidth(attachmentObj[3].toString());
			}				
			if(attachmentObj[4] != null) {
				attachment.setThumbnailHeight(attachmentObj[4].toString());
			}
			if(attachmentObj[5] != null) {
				attachment.setDocType(Byte.valueOf(attachmentObj[5].toString()));
			}				
		}	
		attachmentlist.add(attachment);
		logger.debug("received attachment data for id: " +attachment.getId());				
	}
		return attachmentlist;
	}
	
    
}
