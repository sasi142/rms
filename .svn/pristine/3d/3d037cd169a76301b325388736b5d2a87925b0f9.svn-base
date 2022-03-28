package core.daos.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import core.daos.One2OneChatDao;
import core.entities.ChatSummary;
import core.entities.One2OneChat;
import core.utils.CommonUtil;
import javax.persistence.*;

@Repository
public class One2OneChatDaoImpl extends AbstractJpaDAO<One2OneChat> implements One2OneChatDao {

	final static Logger logger = LoggerFactory.getLogger(One2OneChatDaoImpl.class);
	
    @PersistenceContext(unitName = "readEntityManagerFactory")
    protected EntityManager readEntityManager;


	public One2OneChatDaoImpl() {
		super();
		setClazz(One2OneChat.class);
	}

	@Override
	public List<One2OneChat> getOne2OneChatHistory(Integer from, Integer to, Boolean isGuest, Long lastMsgDate,
			Integer offset, Integer limit) {
		logger.debug("get chat history for= " + from + "& to " + to);
		List<One2OneChat> One2OneChats = new ArrayList<>();
		try {

			StringBuilder queryBuilder = new StringBuilder();

			queryBuilder.append("SELECT	c.Id, c.active, c.createdDate, c.sender, c.lastUpdated, c.status, c.message, "
					+ "c.recipient, c.data, pc.Id AS ParentMessageId, c.DeliveredDate, c.ReadDate, "
					+ "( CASE WHEN pc.Id IS NOT NULL THEN JSON_OBJECT('to', pc.recipient,'text', pc.Message ,'from', pc.sender ,'mid', pc.Id , "
					+ "'chatType', 0, 'chatMessageType', 1, 'utcDate', pc.CreatedDate, "
					+ "'data', pc.Data) ELSE NULL END ) AS ParentMsg, c.MessageType FROM one2one_chat c LEFT OUTER JOIN one2one_chat pc ON pc.Id = c.ParentMessageId "
					+ "AND ( CASE WHEN pc.Sender = :from AND pc.DeletedDateForSender IS NOT NULL THEN 0 "
					+ "WHEN pc.Recipient = :from AND pc.DeletedDateForRecipient IS NOT NULL THEN 0 ");

			if (isGuest) {
				queryBuilder.append("WHEN pc.Recipient = :from AND c.MessageType IN (4,5) THEN 0 ");
			}
			queryBuilder.append("ELSE pc.active END ) = TRUE 	"
					+ "WHERE ( CASE WHEN c.Sender = :from AND c.DeletedDateForSender IS NOT NULL THEN 0 "
					+ "WHEN c.Recipient = :from AND c.DeletedDateForRecipient IS NOT NULL THEN 0 ");

			if (isGuest) {
				queryBuilder.append("WHEN c.Recipient = :from AND c.MessageType IN (4,5) THEN 0 ");
			}
			queryBuilder.append("ELSE c.active END ) = TRUE  AND c.createdDate < :lastMsgDate AND "
					+ "((c.sender = :from AND c.recipient = :to) OR (c.Sender= :to AND c.recipient = :from)) ORDER BY c.createdDate DESC");

			String sqlString = queryBuilder.toString();

			Query query = readEntityManager.createNativeQuery(sqlString, "InsertOne2oneChatResultMappings");
			query.setParameter("from", from);
			query.setParameter("to", to);
			query.setParameter("lastMsgDate", lastMsgDate);
			query.setFirstResult(offset);
			query.setMaxResults(limit);
			One2OneChats = query.getResultList();
			logger.debug("get chat history depatments. total entities found " + One2OneChats.size());
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.info("chat history not found " + One2OneChats);
		}
		return One2OneChats;
	}

	@Override
	public One2OneChat createChatHistory(One2OneChat one2OneChat, Long sysMsgDuration) {
		logger.debug("Create One2OneChat chat history for input " + one2OneChat);
		One2OneChat insertedOne2OneChat = null;
		try {
			StoredProcedureQuery spQuery = entityManager
					.createNamedStoredProcedureQuery("One2OneChat.InsertOne2oneChat");
			spQuery.setParameter("P_Sender", one2OneChat.getFrom());
			spQuery.setParameter("P_Recipient", one2OneChat.getTo());
			spQuery.setParameter("P_Message", one2OneChat.getText());
			spQuery.setParameter("P_Data", one2OneChat.getData() == null ? "" : one2OneChat.getData());
			spQuery.setParameter("P_CreatedDate", one2OneChat.getCreatedDate());
			spQuery.setParameter("P_ParentMessageId",
					one2OneChat.getParentMsgId() == null ? 0 : one2OneChat.getParentMsgId());
			spQuery.setParameter("P_MessageType", one2OneChat.getChatMessageType());
			spQuery.setParameter("P_NotRegisteredSysMsgChkDuration", sysMsgDuration);
			insertedOne2OneChat = (One2OneChat) spQuery.getSingleResult();
			logger.debug("created One2OneChat entity in db with id = " + insertedOne2OneChat.getId());
		} catch (Exception e) {
			logger.error("Failed to insert One2OneChat chat ", e);
			throw e;
		}
		return insertedOne2OneChat;
	}

	@Override
	public Map<Integer, ChatSummary> getUnReadMsgCountAndLastMsgUsers(Integer to, List contactList) {
		logger.debug("get unread messages users for " + to + " of contact list " + contactList);
		Map<Integer, ChatSummary> unreadMessageMap = new HashMap<>();
		String contactStr = CommonUtil.convertListToString(contactList);
		try {
			String msg = "SELECT x.sender, x.Count as UnreadCount, c.message, c.sender AS LastMsgSenderId, c.CreatedDate, c.Status "
					+ "FROM(SELECT o.sender, SUM(( CASE WHEN o.status = false THEN 1 ELSE 0 END )) As Count,"
					+ "( SELECT MAX(oc.Id) AS Id FROM one2one_chat oc WHERE (( oc.sender = o.sender and oc.recipient = o.recipient ) OR "
					+ "( oc.sender = o.recipient and oc.recipient = o.sender ))) AS LastMessageId FROM One2One_Chat o "
					+ "WHERE o.active = TRUE AND o.recipient=" + to + " AND o.sender IN (" + contactStr
					+ ") GROUP BY o.Sender UNION " + "SELECT 	o.recipient, 0 AS Count,"
					+ "(SELECT MAX(oc.Id) AS Id FROM one2one_chat oc WHERE ((oc.sender = o.sender AND oc.recipient = o.recipient) "
					+ "OR (oc.sender = o.recipient AND oc.recipient = o.sender))) AS LastMessageId FROM One2One_Chat o WHERE o.active = TRUE "
					+ "AND o.recipient IN (" + contactStr + ") " + "AND o.sender=" + to
					+ " AND NOT EXISTS (SELECT 1 FROM one2one_chat ooc WHERE ooc.Active = TRUE AND ooc.sender = o.recipient "
					+ "AND ooc.recipient = o.sender) GROUP BY o.recipient) As x INNER JOIN one2one_chat c ON c.Id = x.LastMessageId";
			Query query = entityManager.createNativeQuery(msg);
			List<Object[]> users = query.getResultList();
			if (users != null) {
				for (Object[] row : users) {
					ChatSummary chatSummary = new ChatSummary();
					if (row[0] != null) {
						chatSummary.setContactId(Integer.valueOf(row[0].toString()));
					}
					if (row[1] != null) {
						chatSummary.setUnReadMsgCount(Short.valueOf(row[1].toString()));
					}
					if (row[2] != null) {
						chatSummary.setLastMessage(row[2].toString());
					}
					if (row[3] != null) {
						chatSummary.setLastMsgSenderId(Integer.valueOf(row[3].toString()));
					}
					if (row[4] != null) {
						chatSummary.setTime(Long.valueOf(row[4].toString()));
					}
					if (row[5] != null) {
						chatSummary.setStatus(Boolean.valueOf(row[5].toString()));
					}
					if (row[0] != null) {
						unreadMessageMap.put(Integer.valueOf(row[0].toString()), chatSummary);
					}
				}
			}
			logger.debug("num users found " + unreadMessageMap.size());
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.info("unread messages users not found ");
		}
		return unreadMessageMap;
	}

	@Override
	public void UpdateOne2OneChatReadStatus(Integer to, Long Id) {
		logger.debug("update read status for user Id " + Id);
		try {
			Query query = entityManager.createNamedQuery("One2OneChat.UpdateUnReadChatById");
			query.setParameter("Id", Id);
			query.setParameter("to", to);
			query.setParameter("time", System.currentTimeMillis());
			query.executeUpdate();
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.error("entity not found with given user id");
		}
		logger.debug("updated read status");
	}

	@Override
	public Long getLastMsgIdBySender(Integer senderId, Integer recipientId) {
		logger.debug("get last massage Id by sender " + senderId + " and recipient " + recipientId);
		Long msgId = null;
		try {

			Query query = entityManager.createNamedQuery("One2OneChat.GetLastMsgIdBySender");
			query.setParameter("senderId", senderId);
			query.setParameter("recipientId", recipientId);
			msgId = (Long) query.getSingleResult();
			logger.debug("last massage Id " + msgId + " by sender " + senderId + " and recipient " + recipientId);
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.info("No msgs found for recipient " + recipientId + " by sender " + senderId);
		}
		return msgId;
	}

	@Override
	public One2OneChat getMsgById(Long Id, Integer senderId) {
		logger.debug("get massage by Id " + Id + "sender " + senderId);
		One2OneChat msg = null;
		try {
			Query query = entityManager.createNamedQuery("One2OneChat.GetMsgById");
			query.setParameter("msgId", Id);
			query.setParameter("senderId", senderId);
			msg = (One2OneChat) query.getSingleResult();
			logger.debug("Massage found by Id " + Id);
		} catch (NonUniqueResultException | NoResultException ex) {
			logger.info("No msg found by Id " + Id + "sender " + senderId);
		}
		return msg;
	}
}
