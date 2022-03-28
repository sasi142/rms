package core.daos.impl;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import core.daos.BroadcastMessageDao;
import core.entities.BroadcastMessage;
import core.entities.BroadcastRecipient;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums.ErrorCode;

@Repository
public class BroadcastMessageDaoImpl extends AbstractJpaDAO<BroadcastMessage> implements BroadcastMessageDao {
	final static Logger logger = LoggerFactory.getLogger(BroadcastMessageDaoImpl.class);

	public BroadcastMessageDaoImpl() {
		super();
		setClazz(BroadcastMessage.class);
	}

	public BroadcastMessage getBroadcastMessageById(Integer msgId, Integer userId) {
		logger.debug("get BroadcastMessageById for " + msgId);
		BroadcastMessage bm = null;
		try {
			TypedQuery<BroadcastMessage> query = entityManager
					.createNamedQuery("BroadcastMessage.getBroadcastMessageById", BroadcastMessage.class);
			query.setParameter("id", Long.valueOf(msgId.longValue()));
			query.setParameter("created", userId);
			query.setParameter("recipient", userId);
			List<BroadcastMessage> messages = query.getResultList();
			if (messages != null && !messages.isEmpty()) {
				bm = (BroadcastMessage) messages.get(0);
				logger.debug("message found " + bm.getSubject());
			}

		} catch (NonUniqueResultException | NoResultException ex) {
			logger.info("message  not found " + ex.getMessage());
		}
		return bm;
	}

	public List<BroadcastMessage> getAllBroadcastMessages(Integer id) {
		logger.debug("get all Broadcast Messages ");
		List<BroadcastMessage> broadcastMessages = new ArrayList<BroadcastMessage>();
		try {
			Query query = entityManager.createNamedQuery("BroadcastMessage.getAllBroadcastMessages");
			query.setParameter("id", id);
			List<Object[]> messages = query.getResultList();
			if (messages != null && !messages.isEmpty()) {
				for (Object[] row : messages) {
					BroadcastMessage msg = new BroadcastMessage();
					if (row[0] != null) {
						msg.setSubject(row[0].toString());
					}
					if (row[1] != null) {
						msg.setMessage(row[1].toString());
					}
					if (row[2] != null) {
						msg.setCreatedById(Integer.parseInt(row[2].toString()));
					}
					if (row[3] != null) {
						msg.setReadFlag(Boolean.valueOf(row[3].toString()));
					}
					if (row[4] != null) {
						msg.setCreatedDate(Long.valueOf(row[4].toString()));
					}

					broadcastMessages.add(msg);
				}
			}

		} catch (NonUniqueResultException | NoResultException ex) {
			logger.info("broadcast messages not found " + ex.getMessage());
		}
		return broadcastMessages;
	}

	public void createBroadcastMessage(BroadcastMessage message, BroadcastRecipient recipient) {
		logger.debug("create Broadcast Message ");
		try {
			entityManager.persist(message);
			recipient.setBroadcastMessageId(message.getId());
			entityManager.persist(recipient);
		} catch (Exception e) {
			logger.error("error while create Broadcast Message" + e.getMessage());
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					ErrorCode.Internal_Server_Error.getName(), e);
		}
		logger.debug("message created");
	}

	public Integer getUnreadMessageCount(Integer id) {
		logger.debug("get unread message count");
		Integer unreadCount = 0;
		try {

			Query query = entityManager.createNamedQuery("BroadcastRecipient.getUnreadMessageCount");
			query.setParameter("id", id);
			List<Number> counts = (List<Number>) query.getResultList();
			if (counts != null && !counts.isEmpty()) {
				Long count = counts.get(0).longValue();
				unreadCount = count.intValue();
			}
		} catch (Exception e) {
			logger.error("error while create Broadcast Message" + e.getMessage(), e);
		}
		logger.debug("message created");
		return unreadCount;
	}
}
