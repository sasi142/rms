/**
 * 
 */
package core.daos.impl;

import java.util.List;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import core.daos.MemoRecipientDao;
import core.entities.MemoRecipient;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums.ErrorCode;

/**
 * @author Chandramohan.Murkute
 */
@Repository
public class MemoRecipientDaoImpl extends AbstractJpaDAO<MemoRecipient> implements MemoRecipientDao {

	final static Logger logger = LoggerFactory.getLogger(MemoRecipientDaoImpl.class);

	public MemoRecipientDaoImpl() {
		super();
		setClazz(MemoRecipient.class);
	}

	public void changeReadStatus(Integer memoId, Integer userId, Boolean readStatus) {
		logger.debug("update read status for user from " + userId + "& memoId: " + memoId);
		try {
			Query query = entityManager.createNamedQuery("MemoRecipient.ChangeReadStatus");
			query.setParameter("memoId", memoId);
			query.setParameter("userId", userId);
			query.setParameter("readFlag", readStatus);
			query.setParameter("updatedDate", System.currentTimeMillis());
			query.executeUpdate();
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to update memo read status of memoId = " + memoId, e);
		}
		logger.debug("updated read status for user from " + userId + "& memoId: " + memoId);
	}

	public Integer isReceipient(Integer memoId, Integer userId) {
		Long count = 0L;
		logger.debug("select count for user from " + userId + "& memoId: " + memoId);
		try {
			TypedQuery<Long> query = entityManager.createNamedQuery("MemoRecipient.IsReceipient", Long.class);
			query.setParameter("memoId", memoId);
			query.setParameter("userId", userId);
			count = (Long) query.getSingleResult();
		} catch (Exception e) {
			// Exception handled in validation on the basis of returned count
			e.printStackTrace();
		}
		logger.debug("selected count for user from " + userId + "& memoId: " + memoId);

		return count.intValue();
	}


	public Long getMemoCountByStatus(Integer userId, Boolean readStatus) {
		Long count = 0L;
		logger.debug("select count of memos for user " + userId + "& readStatus: " + readStatus);
		try {
			TypedQuery<Long> query = entityManager.createNamedQuery("MemoRecipient.GetMemoCountByStatus", Long.class);
			query.setParameter("readFlag", readStatus);
			query.setParameter("userId", userId);
			count = query.getSingleResult();
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to get the memo count for user " + userId + "& readStatus: " + readStatus, e);
		}
		logger.debug(
				"selected count of memos for user " + userId + "& readStatus: " + readStatus + " as count " + count);
		return count;
	}
}
