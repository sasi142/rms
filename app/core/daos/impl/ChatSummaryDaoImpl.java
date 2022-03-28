/**
 * 
 */
package core.daos.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.*;

import core.entities.projections.UnreadCountSummary;
import org.hibernate.procedure.ProcedureCall;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.CallableStatementCreatorFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.orm.jpa.EntityManagerFactoryInfo;
import org.springframework.stereotype.Repository;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import play.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.daos.CacheUserDao;
import core.daos.ChatSummaryDao;
import core.entities.ChatMessage;
import core.entities.ChatSummary;
import core.entities.UserContext;
import core.services.EventNotificationBuilder;
import core.utils.CommonUtil;
import core.utils.Enums.ChatMessageType;
import core.utils.Enums.ChatType;
import core.utils.Enums.EventType;
import javax.persistence.*;

/**
 * @author Chandramohan.Murkute
 */
@Repository
public class ChatSummaryDaoImpl extends AbstractJpaDAO<ChatSummary> implements ChatSummaryDao {

	@Autowired
	private Environment					env;

	@Autowired
	private EventNotificationBuilder	eventNotificationBuilder;
	
	@Autowired
	private CommonUtil					commonUtil;
	
	@Autowired
	private CacheUserDao				cacheUserDao;
	
    @PersistenceContext(unitName = "readEntityManagerFactory")
    protected EntityManager readEntityManager;

	public ChatSummaryDaoImpl() {
		super();
		setClazz(ChatSummary.class);
	}

	/*
	 * (non-Javadoc)
	 * @see core.daos.ChatSummaryDao#getContactChatSummary(java.lang.Integer, java.util.Map)
	 */
	public Map<String, ChatSummary> getContactChatSummary(Integer currentUserId, Map<Byte, List<Integer>> contactMap) {
		Logger.underlying().info("get Contact Chat Summary for currentUserId : " + currentUserId);
		Logger.underlying().debug("get Contact Chat Summary for contact Map " + contactMap);
		List<ChatSummary> chatSummaries = null;

		String strOne2OneQuery = "SELECT c.Id AS LastMessageId, cu.ChatType, cu.SenderId, cu.UnReadMsgCount AS UnreadCount, c.Message, c.Sender AS LastMsgSenderId, c.CreatedDate"
				+ " FROM chat_user_last_message cu INNER JOIN One2One_Chat c ON c.Id = cu.LastMessageId"
				+ " WHERE cu.chatType = 0 AND cu.SenderId IN :senderIds AND cu.recipientId = :currentUserId";

		String strGroupQuery = " SELECT c.Id AS LastMessageId, cu.ChatType, cu.SenderId, cu.UnReadMsgCount AS UnreadCount, c.Message, c.SenderId AS LastMsgSenderId, c.CreatedDate"
				+ " FROM chat_user_last_message cu LEFT OUTER JOIN Group_Chat c ON c.Id = cu.LastMessageId"
				+ " WHERE cu.chatType = 1  AND cu.SenderId IN :groupIds AND cu.recipientId = :currentUserId";

		String strQuery = null;
		if (contactMap != null && !contactMap.isEmpty()) {
			if (contactMap.containsKey(ChatType.One2One.getId())) {
				strQuery = strOne2OneQuery;
			}
			if (contactMap.containsKey(ChatType.GroupChat.getId())) {
				if (strQuery == null) {
					strQuery = strGroupQuery;
				} else {
					strQuery = strQuery + " UNION ALL " + strGroupQuery;
				}
			}
		}
		Logger.underlying().debug("get Contact Chat Summary, Executing query : " + strQuery);
		Map<String, ChatSummary> contactSummaryMap = new LinkedHashMap<String, ChatSummary>();
		try {
			Query query = entityManager.createNativeQuery(strQuery, "ChatSummary.GetContactChatSummary");
			query.setParameter("currentUserId", currentUserId);
			if (contactMap.containsKey(ChatType.One2One.getId())) {
				query.setParameter("senderIds", contactMap.get(ChatType.One2One.getId()));
			}
			if (contactMap.containsKey(ChatType.GroupChat.getId())) {
				query.setParameter("groupIds", contactMap.get(ChatType.GroupChat.getId()));
			}
			chatSummaries = query.getResultList();
			Logger.underlying().debug("get Contact Chat Summary, query result size : " + (chatSummaries == null ? null : chatSummaries.size()));
			if (chatSummaries != null) {
				for (ChatSummary chatSummary : chatSummaries) {
					contactSummaryMap.put(chatSummary.getContactId() + "_" + chatSummary.getChatType().byteValue(), chatSummary);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return contactSummaryMap;
	}

	/*
	 * (non-Javadoc)
	 * @see core.daos.ChatSummaryDao#updateChatReadStatus(java.lang.Integer, java.lang.Integer, core.utils.Enums.ChatType)
	 */
	public void updateChatReadStatus(Integer toUserGroupId, Integer currentUserId, ChatType chatType) {
		Logger.underlying().debug("update read status for user from " + toUserGroupId + "&to: " + currentUserId);
		try {
			Query query = entityManager.createNamedQuery("ChatSummary.UpdateUnReadChat");
			query.setParameter("contcatId", toUserGroupId);
			query.setParameter("recipientId", currentUserId);
			query.setParameter("chatType", chatType.getId());
			query.executeUpdate();
		} catch (NonUniqueResultException | NoResultException ex) {
			Logger.underlying().error("entity not found with given user id");
		}
		Logger.underlying().debug("updated read status for user from " + toUserGroupId + "&to: " + currentUserId);
	}

	@Override
	public List<ChatMessage> updateChatReadStatusV2(Integer currentUserId, String one2oneMsgIds, String groupMsgIds) {
		Logger.underlying().debug("update read status for user to: " + currentUserId + " one2oneMsgIds: " + one2oneMsgIds + " groupMsgIds: " + groupMsgIds);

		List<SqlParameter> params = new ArrayList<>();
		params.add(new SqlParameter("P_UserId", Types.INTEGER));
		params.add(new SqlParameter("P_One2OneChatMarkRead", Types.VARCHAR));
		params.add(new SqlParameter("P_GroupChatMarkRead", Types.VARCHAR));
		params.add(new SqlParameter("P_Date", Types.BIGINT));

		final long currentDate = System.currentTimeMillis();
		params.add(new SqlReturnResultSet("chat-messages", new RowMapper<ChatMessage>() {
			@Override
			public ChatMessage mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				ChatMessage msg = new ChatMessage(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getLong(4), (Long) rs.getObject(5));
				return msg;
			}
		}));

		CallableStatementCreatorFactory cscFactory = new CallableStatementCreatorFactory("call USP_MarkChatReadOrDelivered(?,?,?,?)", params);

		JdbcTemplate jdbcTemplate = new JdbcTemplate(((EntityManagerFactoryInfo) entityManager.getEntityManagerFactory()).getDataSource());
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put("P_UserId", currentUserId);
		inParams.put("P_One2OneChatMarkRead", one2oneMsgIds);
		inParams.put("P_GroupChatMarkRead", groupMsgIds);
		inParams.put("P_Date", currentDate);

		CallableStatementCreator csc = cscFactory.newCallableStatementCreator(inParams);
		Map<String, Object> resultSets = jdbcTemplate.call(csc, params);

		List<ChatMessage> msgs = null;
		if (resultSets != null || resultSets.size() == 0) {
			msgs = (List<ChatMessage>) resultSets.get("chat-messages");
			Logger.underlying().debug("updated read status for user to: " + (msgs == null ? null : msgs.size()));
		}
		return msgs;
	}

	public List<ChatSummary> getUnReadMsgsPerContact(Integer to) {
		Logger.underlying().debug("get unread messages users for " + to);
		ChatSummary cs = null;
		List<ChatSummary> chatSummaries = new ArrayList<ChatSummary>();
		try {
			Query query = entityManager.createNamedQuery("ChatSummary.getUnReadMsgsPerContact");
			query.setParameter("recipientId", to);
			List<Object[]> users = query.getResultList();
			if (users != null) {
				for (Object[] row : users) {
					if (row[0] != null && row[1] != null && row[2] != null) {
						cs = new ChatSummary(Integer.valueOf(row[0].toString()), Byte.valueOf(row[1].toString()), Short.valueOf(row[2].toString()));
						chatSummaries.add(cs);
					}
				}
			}
			Logger.underlying().debug("num users found " + chatSummaries.size());
		} catch (NonUniqueResultException | NoResultException ex) {
			Logger.underlying().info("unread messages users not found ");
		}
		return chatSummaries;
	}

	public Long getUnReadSenderCount(Integer to) {
		Long unreadSenderCount = 0L;
		Logger.underlying().debug("get unread senders for receipient" + to);
		try {
			Query query = entityManager.createNamedQuery("ChatSummary.getUnReadSenderCount");
			query.setParameter("recipientId", to);
			unreadSenderCount = (Long) query.getSingleResult();
			Logger.underlying().debug("num users found who has sent messages = " + unreadSenderCount);
		} catch (NonUniqueResultException | NoResultException ex) {
			Logger.underlying().info("unread messages users not found ");
		}
		return unreadSenderCount;
	}

	@Override
	public List<ChatSummary> getUnReadMsgContact(List<Integer> userIds, Integer offset, Integer limit) {
		ChatSummary cs = null;
		List<ChatSummary> chatSummaries = new ArrayList<ChatSummary>();
		try {
			Query query = entityManager.createNamedQuery("ChatSummary.getUnReadMsgContact");
			query.setParameter("userIds", userIds);
			query.setFirstResult(offset);
			query.setMaxResults(limit);
			List<Object[]> results = query.getResultList();
			if (results != null) {
				for (Object[] row : results) {
					if (row[0] != null && row[1] != null) {
						cs = new ChatSummary(Integer.valueOf(row[0].toString()), Short.valueOf(row[1].toString()));
						chatSummaries.add(cs);
					}
				}
			}
			Logger.underlying().debug("num users found " + chatSummaries.size());
		} catch (NonUniqueResultException | NoResultException ex) {
			Logger.underlying().info("unread message contacts for multiple users not found ");
		}
		return chatSummaries;
	}

	/*
	 * (non-Javadoc)
	 * @see core.daos.ChatSummaryDao#deactivateOne2OneChatSummary(java.lang.Integer)
	 */
	public void deactivateOne2OneChatSummary(Integer deletedUserId) {
		Logger.underlying().debug("update read status for receipinets of the sender " + deletedUserId);
		try {
			Query query = entityManager.createNamedQuery("ChatSummary.DeactivateOne2OneChatSummary");
			query.setParameter("deletedUserId", deletedUserId);
			query.executeUpdate();
		} catch (NonUniqueResultException | NoResultException ex) {
			Logger.underlying().error("entity not found with given user id");
		}
		Logger.underlying().debug("updated read status for user from " + deletedUserId);
	}

	/*
	 * (non-Javadoc)
	 * @see core.daos.ChatSummaryDao#getChatHistory(core.entities.UserContext, java.lang.Integer, java.lang.Integer, java.lang.String)
	 */
	@Override
	public List<ChatMessage> getChatHistory(UserContext userContext, Integer offset, Integer limit, String syncDates, Boolean isFirstTimeSync,
			Integer perUserMsgCout) {

		final Integer currentUserId = userContext.getUser().getId();
		final UserContext userContextObject = userContext;
		List<SqlParameter> params = new ArrayList<>();
		params.add(new SqlParameter("P_UserId", Types.INTEGER));
		params.add(new SqlParameter("P_SyncDates", Types.VARCHAR));
		params.add(new SqlParameter("P_IsFirstTimeSync", Types.BOOLEAN));
		params.add(new SqlParameter("P_PerUserMsgCount", Types.INTEGER));
		params.add(new SqlParameter("P_Offset", Types.INTEGER));
		params.add(new SqlParameter("P_Count", Types.INTEGER));

		params.add(new SqlReturnResultSet("ChatHistory", new RowMapper<ChatMessage>() {
			@Override
			public ChatMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
				String data = rs.getString(8);
				Integer chatMessageType = rs.getInt(7);
				Integer chatType = rs.getInt(6);
				String text = rs.getString(1);
				Integer from = rs.getInt(4);
				Integer to = rs.getInt(5);
				// Change all video call messages to system messages. This was done as we wanted
				// to avoid have the client apps
				// treat VideoMessages as system messages. At the time we did not want to change
				// the mobile apps.
				if (chatMessageType.byteValue() == ChatMessageType.VideoCallMessage.getId().byteValue()) {
					chatMessageType = ChatMessageType.SystemMessage.getId().intValue();
				}
				
				if ((chatType.byteValue() == ChatType.GroupChat.getId().byteValue()) && 
						(data != null && !data.isEmpty() && chatMessageType.byteValue() == ChatMessageType.SystemMessage.getId().byteValue())) {
					// text = eventNotificationBuilder.buildMessage(null, data, currentUserId);
					text  = eventNotificationBuilder.buildMessage(rs.getLong(2), null, data, currentUserId, userContextObject);
					data = null;
				}else if(chatType.byteValue() == ChatType.One2One.getId().byteValue() && 
						(chatMessageType.byteValue() == ChatMessageType.SystemMessage.getId().byteValue() && (text == null || "".equalsIgnoreCase(text.trim())))){
					ObjectMapper mapper = new ObjectMapper();
					try {
						if (data != null && !data.trim().isEmpty()) {
							JsonNode node = (JsonNode) mapper.readTree(data);
							JsonNode eventTypeNode = node.findPath("eventType");
							EventType eventType = null;
							// There is a special case for NotRegistered events, which needs to be 
							// split into 2 other events depending upon who is asking for this history.
							if (eventTypeNode != null && EventType.getEventTypeById(eventTypeNode.asInt()).equals(EventType.NotRegisteredMessage)) {
								if(from.equals(currentUserId)){
									eventType = EventType.NotRegisteredMessageSent;
								} else {
									eventType = EventType.NotRegisteredMessageReceived;
								}
							}
							text  = eventNotificationBuilder.buildMessage(rs.getLong(9), eventType, data, currentUserId, userContextObject);
						}
					} catch (Exception e) {
						Logger.underlying().warn("some problem with json string. ignore attachment data " + data);
					}
					text  = eventNotificationBuilder.buildMessage(rs.getLong(9), null, data, currentUserId, userContextObject);
				}
				
				// 1 2 3 4 5 6 7 8 9 10
				// String text, Long utcDate, Long mid, Integer from, Integer to, Integer chatType, Integer chatMessageType, String data, Long deliveryDate,
				// Long readDate
				ChatMessage contact = new ChatMessage(text, rs.getLong(2), rs.getLong(3), from, to, chatType, chatMessageType, data, rs
						.getLong(9), rs.getLong(10), (Long) rs.getObject(11), rs.getString(12), ((rs.getString(13) != null) ? Long.valueOf(rs.getString(13)) :null), rs.getBoolean(14));
				return contact;
			}
		}));

		CallableStatementCreatorFactory cscFactory = new CallableStatementCreatorFactory("call USP_GetChatMessage(?,?,?,?,?,?)", params);

		JdbcTemplate jdbcTemplate = new JdbcTemplate(((EntityManagerFactoryInfo) readEntityManager.getEntityManagerFactory()).getDataSource());
		Map<String, Object> inParams = new HashMap<String, Object>();
		inParams.put("P_UserId", currentUserId);
		inParams.put("P_SyncDates", syncDates);
		inParams.put("P_IsFirstTimeSync", isFirstTimeSync);
		inParams.put("P_PerUserMsgCount", perUserMsgCout);
		inParams.put("P_Offset", offset);
		inParams.put("P_Count", limit);

		CallableStatementCreator csc = cscFactory.newCallableStatementCreator(inParams);
		Map<String, Object> resultSets = jdbcTemplate.call(csc, params);

		return (List<ChatMessage>) resultSets.get("ChatHistory");
	}

	@Override
	public List<ChatSummary> getUnReadMsgsForContacts(Integer to, List<Integer> userIds, List<Integer> groupIds) {
		Logger.underlying().debug("get unread messages users for " + to);
		ChatSummary cs = null;
		List<ChatSummary> chatSummaries = new ArrayList<ChatSummary>();
		try {
			Query query = entityManager.createNamedQuery("ChatSummary.getUnReadMsgsForContacts");
			query.setParameter("recipientId", to);
			query.setParameter("userIds", userIds);
			query.setParameter("groupIds", groupIds);
			List<Object[]> users = query.getResultList();
			if (users != null) {
				for (Object[] row : users) {
					if (row[0] != null && row[1] != null) {
						cs = new ChatSummary(Integer.valueOf(row[0].toString()), Byte.valueOf(row[1].toString()), Short.valueOf(row[2].toString()));
						chatSummaries.add(cs);
					}
				}
			}
			Logger.underlying().debug("num users found " + chatSummaries.size());
		} catch (NonUniqueResultException | NoResultException ex) {
			Logger.underlying().info("unread messages users not found ");
		}
		return chatSummaries;
	}

	@Override
	public List<ChatSummary> getRecipientUnreadCount(Integer recipientId) {
		Query query = entityManager.createNamedQuery("ChatSummary.RecipientUnreadCount");
		query.setParameter("recipientId", recipientId);
		List<Object[]> users = query.getResultList();
		List<ChatSummary> chatSummaries = users.stream().map(user -> new ChatSummary((Integer)user[0], (Byte)user[1], (Short)user[2])).collect(Collectors.toList());
		Logger.underlying().debug("num users found " + chatSummaries.size());
		return chatSummaries;
	}

	@Override
	public ChatSummary getChatSummary(Integer contactId, Integer recipientId, ChatType chatType) {
		Logger.underlying().debug("get chat summary for contact " + contactId + " and recipient " + recipientId);
		ChatSummary cs = null;
		try {
			Query query = entityManager.createNamedQuery("ChatSummary.getChatSummary");
			query.setParameter("recipientId", recipientId);
			query.setParameter("contactId", contactId);
			query.setParameter("chatType", chatType.getId());
			cs = (ChatSummary) query.getSingleResult();
		} catch (NonUniqueResultException | NoResultException ex) {
			Logger.underlying().info("No chat summary found for contact " + contactId + " and recipient " + recipientId);
		}
		return cs;
	}

	@Override
	@Transactional
	public boolean deleteChatMessages(Integer currentUserId,
			String msgIds, Integer contactId, Integer deleteOption,
			Integer chatType, Long deletedDate) {
		StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("ChatSummary.DeleteChatMessages");
		spQuery.setParameter("P_UserId", currentUserId);
		spQuery.setParameter("P_ContactId", contactId);
		spQuery.setParameter("P_ChatType", chatType.byteValue());
		spQuery.setParameter("P_ChatMessageIds", msgIds);
		spQuery.setParameter("P_DeleteOption", deleteOption.byteValue());
		spQuery.setParameter("P_DeletionDate", deletedDate);
		Byte outParam = (Byte) spQuery.getOutputParameterValue("O_Status");
		if (outParam == 1){
			return true;
		}
		logger.info("Failed to delete messages for given params. UserId {}, Message Ids {}, Contact Id {}, " +
						"DeleteOption {}, ChatType {}, DeleteDate {}, Result {}",
				currentUserId, msgIds, contactId, deleteOption, chatType, deletedDate, outParam);
		return false;
	}

	@Override
	public Optional<UnreadCountSummary> unreadCountSummary(Integer orgId, String userName) {
		StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("ChatSummary.AllUnreadCountSummary");
		spQuery.setParameter("P_OrganizationId", orgId);
		spQuery.setParameter("P_UserName", userName);
		List<UnreadCountSummary> result = spQuery.getResultList();
		return result.stream().findFirst();
	}

	private String getUserName(Map<Integer, String> userIdNameMap, Integer userId) {
		String fromName = null;
		if (userIdNameMap.containsKey(userId)) {
			fromName = userIdNameMap.get(userId);
		} else {
			JsonNode fromUserJson = cacheUserDao.find(userId);
			fromName = fromUserJson.findPath("firstName").asText();
			userIdNameMap.put(userId, fromName);
		}
		return fromName;
	}
}
