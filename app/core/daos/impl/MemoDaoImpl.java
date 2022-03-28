package core.daos.impl;

import java.util.List;
import java.util.Objects;

import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import core.daos.MemoDao;
import core.entities.Memo;
import core.entities.MemoChatUser;
import core.exceptions.InternalServerErrorException;
import core.exceptions.ResourceNotFoundException;
import core.utils.Enums.ErrorCode;

import javax.persistence.*;

@Repository
public class MemoDaoImpl extends AbstractJpaDAO<Memo> implements MemoDao {

	final static Logger logger = LoggerFactory.getLogger(MemoDaoImpl.class);
	
    @PersistenceContext(unitName = "readEntityManagerFactory")
    protected EntityManager readEntityManager;

	public MemoDaoImpl() {
		super();
		setClazz(Memo.class);
	}

	public Memo createMemo(Memo memo) {
		logger.info("Create memo with subject " + (Objects.isNull(memo) ? "null" : memo.getSubject()));
		Memo createdMemo = null;
		
		try {
			StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("Memo.CreateMemo");
			spQuery.setParameter("P_UserId", memo.getCreatedById());
			spQuery.setParameter("P_OrganizationId", memo.getOrganizationId());
			spQuery.setParameter("P_Subject", memo.getSubject());
			spQuery.setParameter("P_Message", memo.getMessage());
			
			List<Long> recipientIds = memo.getRecipientIds();
			spQuery.setParameter("P_RecipientIds", StringUtils.join(recipientIds, ","));
			logger.debug("Number of RecipientIds:" + recipientIds.size());
			
			Integer uploadId = memo.getUploadId();
			uploadId = (Objects.nonNull(uploadId) && uploadId > 0) ? memo.getUploadId() : 0 ;
			spQuery.setParameter("P_UploadId", uploadId);
			spQuery.setParameter("P_GroupIds",
					Objects.isNull(memo.getAdGroupIds()) ? "" : StringUtils.join(memo.getAdGroupIds(), ","));
			spQuery.setParameter("P_AttachmentIds",
					Objects.isNull(memo.getAttachmentIds()) ? "" : StringUtils.join(memo.getAttachmentIds(), ","));
			spQuery.setParameter("P_IsPublic", memo.getIsPublic());
			spQuery.setParameter("P_PublicURL", memo.getPublicURL());
			spQuery.setParameter("P_ChannelId",
					(Objects.isNull(memo.getChannelId())  || memo.getChannelId() <= 0) ? 0 : memo.getChannelId());
			spQuery.setParameter("P_Snippet", memo.getSnippet());
			spQuery.setParameter("P_SendAs", memo.getSendAs());
			spQuery.setParameter("P_One2OneChatIds",
					Objects.isNull(memo.getOne2OneChatIds()) ? "" : StringUtils.join(memo.getOne2OneChatIds(), ","));
			spQuery.setParameter("P_GroupChatIds",
					Objects.isNull(memo.getGroupChatIds()) ? "" : StringUtils.join(memo.getGroupChatIds(), ","));
			spQuery.setParameter("P_ShowUserDetailOnSCP", memo.getShowUserDetailOnSCP());
			spQuery.setParameter("P_MemoType", memo.getMemoType());

			createdMemo = (Memo) spQuery.getSingleResult();
			logger.info("Create Memo procedure returned with"
					+ (Objects.isNull(createdMemo) ? "memo is null" : createdMemo.getSubject() + "," + createdMemo.getId()));
			
		} 
		catch (Exception ex) {
			logger.info("Error while creating Memo at DB", ex);
			throw new InternalServerErrorException(ErrorCode.Invalid_Data, "Error while creating Memo at DB");
		}
		return createdMemo;
	}

	@SuppressWarnings("unchecked")
	public List<Memo> getMemosByOrgId(Integer userId, Integer organizationId, Integer offset, Integer limit) {
		logger.info("Get memo list of org " + organizationId);
		List<Memo> memos = null;
		try {
			String strQuery = "SELECT m.Id, m.Subject, m.CreatedById, m.CreatedDate, m.OrganizationId, m.Active, x.ReadPercent FROM memo m INNER JOIN ( SELECT	mr.MemoId, ROUND(SUM((CASE WHEN mr.ReadFlag = TRUE THEN 1 ELSE 0 END)) * 100 / COUNT(mr.Id)) AS ReadPercent FROM memo_recipient mr INNER JOIN memo m ON m.Id = mr.MemoId AND m.OrganizationId = :orgId AND m.Active = TRUE WHERE mr.Active = TRUE GROUP BY mr.MemoId) AS x ON x.MemoId = m.Id WHERE m.OrganizationId = :orgId  AND m.Active = TRUE AND m.CreatedById = :userId ORDER BY m.CreatedDate DESC";
			Query query = readEntityManager.createNativeQuery(strQuery, "Memo.GetMemosByOrgId");
			query.setParameter("orgId", organizationId);
			query.setParameter("userId", userId);
			query.setFirstResult(offset);
			query.setMaxResults(limit);
			memos = query.getResultList();
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to list memos  of org = " + organizationId, e);
		}
		logger.info(
				"Returning memo list of size " + (memos == null ? null : memos.size()) + " for org " + organizationId);
		return memos;
	}

	@SuppressWarnings("unchecked")
	public List<Memo> getMemosByUserId(Integer userId, Integer offset, Integer limit) {
		logger.info("Get memo list of user " + userId);
		List<Memo> memos = null;
		try {
			String strQuery = "SELECT m.Id, m.Subject, m.CreatedById, m.CreatedDate, m.Active, mr.ReadFlag FROM memo m INNER JOIN memo_recipient mr ON mr.MemoId = m.Id AND mr.Active = TRUE WHERE mr.UserId = :userId AND m.Active = TRUE ORDER BY m.Id DESC";
			Query query = readEntityManager.createNativeQuery(strQuery, "Memo.GetMemosByUserId");
			query.setParameter("userId", userId);
			query.setFirstResult(offset);
			query.setMaxResults(limit);
			memos = query.getResultList();
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to list memos  of user = " + userId, e);
		}
		logger.info("Returning memo list of size " + (memos == null ? null : memos.size()) + " for user " + userId);
		return memos;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Memo> getMemosByUserIdV2(Integer id, Integer channelId, Integer offset, Integer limit) {
		logger.info("Get ChannelMessages List");
		List<Memo> memos = null;
		try {
			StoredProcedureQuery spQuery = readEntityManager.createNamedStoredProcedureQuery("Memo.GetMemoList");
			spQuery.setParameter("P_UserId", id);
			spQuery.setParameter("P_ChannelId", channelId);
			spQuery.setParameter("P_Offset", offset);
			spQuery.setParameter("P_count", limit);

			logger.info("Getting ChannelMessages List...Passing parameters to GetMemoList PROC...P_UserId:" + id + ",P_ChannelId:" + channelId
					+ ",P_Offset:" + offset + ",P_count:" + limit);

			memos = (List<Memo>) spQuery.getResultList();
			logger.info("got MemoList " + memos);

		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to list memos  of user = " + id + " and channelId:"+channelId, e);
		}
		return memos;

	}

	public Memo getMemoDetails(Integer memoId, Boolean needSummary, Integer loggedInUserId) {
		logger.info("Get memo details of memo " + memoId + " with needSummary falg " + needSummary);
		Memo memo = null;
		try {
			String strQuery = "SELECT	m.Id, m.Subject, m.Message, m.CreatedById, m.CreatedDate, m.OrganizationId, m.Active, m.IsPublic, m.PublicURL, m.ShowUserDetailOnSCP, m.snippet";
			if (needSummary) {
				strQuery = strQuery
						+ " ,COUNT(mr.Id) AS TotalRecipient, SUM((CASE WHEN mr.ReadFlag = TRUE THEN 1 ELSE 0 END)) AS ReadCount,"
						+ " SUM((CASE WHEN mr.ReadFlag = FALSE THEN 1 ELSE 0 END)) AS UnReadCount,"
						+ " ROUND(SUM((CASE WHEN mr.ReadFlag = TRUE THEN 1 ELSE 0 END)) * 100 / COUNT(mr.Id)) AS ReadPercent, umr.ReadFlag"
						+ " FROM memo m INNER JOIN memo_recipient mr ON mr.MemoId = m.Id AND mr.Active = TRUE"
						+ " INNER JOIN memo_recipient umr ON umr.MemoId = m.Id AND umr.Active = TRUE AND umr.UserId = :loggedInUserId"
						+ " WHERE m.Id = :memoId AND m.Active = TRUE"
						+ " GROUP BY m.Id, m.Subject, m.Message, m.CreatedById, m.CreatedDate, m.OrganizationId, m.Active, m.IsPublic, m.PublicURL, umr.ReadFlag";
			} else {
				strQuery = strQuery
						+ " ,null AS TotalRecipient, null AS ReadCount, null AS UnReadCount, null AS ReadPercent, mr.ReadFlag"
						+ " FROM memo m INNER JOIN memo_recipient mr ON mr.MemoId = m.Id AND mr.Active = TRUE AND mr.UserId = :loggedInUserId"
						+ " WHERE m.Id = :memoId AND m.Active = TRUE";
			}

			Query query = readEntityManager.createNativeQuery(strQuery, "Memo.GetMemoDetails");
			query.setParameter("memoId", memoId);
			query.setParameter("loggedInUserId", loggedInUserId);
			memo = (Memo) query.getSingleResult();
			logger.debug("Returning memo details as memo " + memo);
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to get memo details of memo = " + memoId, e);
		}
		return memo;
	}

	public Memo getMemoDetailsV2(Integer memoId, Boolean needSummary, Integer loggedUserId) {
		logger.info("Get memo details of memo " + memoId + " with needSummary falg " + needSummary);

		Memo memo = null;
		try {
			StoredProcedureQuery spQuery = readEntityManager.createNamedStoredProcedureQuery("Memo.GetMemoById");
			spQuery.setParameter("P_UserId", loggedUserId);
			spQuery.setParameter("P_MemoId", memoId);
			spQuery.setParameter("P_NeedSummay", needSummary);
			logger.info("Get memo details of memo " + memoId + "for user " + loggedUserId + " with needSummary falg "
					+ needSummary);
			memo = (Memo) spQuery.getSingleResult();
			logger.info("Got memo details of memo " + memoId + " with needSummary falg " + needSummary);

		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to get memo details of memo = " + memoId, e);
		}
		return memo;
	}
	
	@Override
	public Memo getMemoDetailsById(Integer memoId, Boolean needSummary, Integer loggedUserId) {
		logger.info("Get memo details of memo " + memoId + " with needSummary falg " + needSummary+ "loggedUserId : "+loggedUserId);
		Memo memo = null;
		try {
			StoredProcedureQuery spQuery = readEntityManager.createNamedStoredProcedureQuery("Memo.GetMemoDetailsById");			
			spQuery.setParameter("P_MemoId", memoId);
			spQuery.setParameter("P_UserId", loggedUserId);
			spQuery.setParameter("P_NeedSummay", needSummary);
			logger.info("Get memo details of memo " + memoId + "for user " + loggedUserId + " with needSummary falg "
					+ needSummary);
			memo = (Memo) spQuery.getSingleResult();
			logger.info("Got memo details of memo " + memoId + " with needSummary falg " + needSummary);

		} catch (Exception e) {
			logger.info("exception :", e);
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to get memo details of memo = " + memoId, e);
		}
		return memo;
	}
	
	
	@Override
	public String getMemoReportById(Integer memoId) {
		logger.info("Get memo details of memo " + memoId );
		String memoReport = null;
		try {
			StoredProcedureQuery spQuery = readEntityManager.createNamedStoredProcedureQuery("Memo.GetCustomMemoReport");			
			spQuery.setParameter("P_MemoId", memoId);		
			logger.info("Get memo details of memo " + memoId );
			memoReport =  (String) spQuery.getSingleResult();
			logger.info("Got memo details of memo " + memoId );

		} catch (Exception e) {
			logger.info("exception :", e);
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to get memo details of memo = " + memoId, e);
		}
		return memoReport;
	}
	
	@Override
	public String getRegularMemoReportById(Integer memoId) {
		logger.info("Get memo details of memo " + memoId );
		String memoReport = null;
		try {
			StoredProcedureQuery spQuery = readEntityManager.createNamedStoredProcedureQuery("Memo.GetRegularMemoReport");			
			spQuery.setParameter("P_MemoId", memoId);		
			logger.info("Get regularmemo details of memo " + memoId );
			memoReport =  (String) spQuery.getSingleResult();
			logger.info("Got regular memo details of memo " + memoId );

		} catch (Exception e) {
			logger.info("exception :", e);
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to get memo details of memo = " + memoId, e);
		}
		return memoReport;
	}

	@Override
	public void updateMemoPublicState(Integer memoId, Boolean isPublic) {
		logger.debug("update public state of memo with id: " + memoId);
		try {
			Query query = entityManager.createNamedQuery("Memo.UpdatePublicState");
			query.setParameter("memoId", memoId);
			query.setParameter("isPublic", isPublic);
			query.setParameter("updatedDate", System.currentTimeMillis());
			query.executeUpdate();
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to update memo public state of memoId = " + memoId, e);
		}
		logger.debug("updated public state of memo with id " + memoId);

	}

	public Memo getMemoByPublicURL(String url) {
		logger.info("Get memo by public url : " + url);
		Memo memo = null;
		try {
			TypedQuery<Memo> query = entityManager.createNamedQuery("Memo.GetMemoByPublicURL", Memo.class);
			query.setParameter("url", url);
			memo = query.getSingleResult();
			logger.debug("Returning memo details as memo " + memo);
		} catch (Exception ex) {
			throw new ResourceNotFoundException(ErrorCode.Invalid_Memo_Public_Url,
					"Memo with public url " + url + " does not exists", ex);
		}
		return memo;
	}

	@Override
	public void updateMemoPageViewCount(Integer memoId) {
		logger.debug("update page views count of memo with id: " + memoId);
		try {
			Query query = entityManager.createNamedQuery("Memo.UpdatePageViews");
			query.setParameter("memoId", memoId);
			query.setParameter("updatedDate", System.currentTimeMillis());
			query.executeUpdate();
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to update memo page views of memoId = " + memoId, e);
		}
		logger.debug("updated page views of memo with id " + memoId);

	}

	@SuppressWarnings("unchecked")
	public List<MemoChatUser> getMemoChatUsers(Integer memoId, Integer offset, Integer limit) {
		logger.info("Get chat user list of memo " + memoId);
		List<MemoChatUser> chatUsers = null;
		try {
			StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("Memo.GetMemoChatList");
			spQuery.setParameter("P_MemoId", memoId);
			spQuery.setParameter("P_Offset", offset);
			spQuery.setParameter("P_count", limit);
			logger.info("Get chat users of memo " + memoId);
			chatUsers = (List<MemoChatUser>) spQuery.getResultList();
			logger.info("Got memo details of memo " + memoId);
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to get memo details of memo = " + memoId, e);
		}
		return chatUsers;
	}

	public Memo getMessagePublicPage(String url) {
		logger.info("Getting data from DaoLayer for url: " + url);
		Memo memo = null;
		try {
			StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("Memo.GetMemoPublicPage");
			spQuery.setParameter("P_PublicURL", url);
			memo = (Memo) spQuery.getSingleResult();
			logger.debug("Returning memo details as memo " + memo);
		} catch (Exception ex) {
			throw new ResourceNotFoundException(ErrorCode.Resource_Not_Found, ex.toString());
		}
		return memo;
	}

}