package core.daos.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.node.ObjectNode;

import core.daos.BulkMemoDumpDao;
import core.entities.BulkMemoDump;
import core.exceptions.InternalServerErrorException;
import core.utils.Enums.ErrorCode;

@Repository
public class BulkMemoDumpDaoImpl extends AbstractJpaDAO<BulkMemoDump> implements BulkMemoDumpDao {

	final static Logger logger = LoggerFactory.getLogger(BulkMemoDumpDaoImpl.class);

	public BulkMemoDumpDaoImpl() {
		super();
		setClazz(BulkMemoDump.class);
	}
	
	@Override
	public BulkMemoDump getMemoDump() {
		logger.info("get memo dump detail for process memo");	
		BulkMemoDump memoDump = new BulkMemoDump();
		try {	
			StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("BulkMemoDump.GetMemoDump");		
					    
			memoDump =  (BulkMemoDump) spQuery.getSingleResult();
			logger.info("get memo dump is done ");	
		}catch (Exception e) {
			logger.info("memo dump not found ");
		}	
		return memoDump;
	}
	
	@Override
	public List<BulkMemoDump> createMemoFromMemoDump(Integer orgId, Integer userId, List<ObjectNode> memoList) {
		logger.info("Create Bulk Memo started");	
		List<BulkMemoDump> memoDumpList = new ArrayList<BulkMemoDump>();
		try {	
			StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("BulkMemoDump.InserBulkMemo");		
			spQuery.setParameter("P_OrganizationId", orgId);    
			spQuery.setParameter("P_UserId", userId); 
			spQuery.setParameter("P_MemoObject", memoList.toString()); 
			memoDumpList =  spQuery.getResultList();
			logger.info("Creation of bulk Memo Done.");	
		}catch (Exception e) {
			logger.info("create bulk Memo Failed: ", e);
		}	
		return memoDumpList;
	}
	
	
	@Override
	public void updateBulkMemoDumpStatus(Integer bulkMemoDumpId, Byte status) {
		logger.debug("update public state of memo with id: " + bulkMemoDumpId);
		try {
			Query query = entityManager.createNamedQuery("BulkMemoDump.UpdateBulkMemoStatus");
			query.setParameter("bulkMemoDumpId", bulkMemoDumpId);
			query.setParameter("status", status);
			query.setParameter("updatedDate", System.currentTimeMillis());
			query.executeUpdate();
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to update bulk Memo Dump table= " + bulkMemoDumpId, e);
		}
		logger.info("updated status in  bulk memo dump " + bulkMemoDumpId);

	}
	
	@Override
	public void updateSentMemoCount(Integer bulkMemoDumpId, Integer memoSentCount) {
		logger.debug("update memo sent count: "+memoSentCount +" in bulk memo dump: " + bulkMemoDumpId);
		try {
			Query query = entityManager.createNamedQuery("BulkMemoDump.UpdateMemoSentCount");
			query.setParameter("bulkMemoDumpId", bulkMemoDumpId);
			query.setParameter("memoSentCount", memoSentCount);
			query.setParameter("updatedDate", System.currentTimeMillis());
			query.executeUpdate();
		} catch (Exception e) {
			logger.info("Failed updated memo sent count " + e);
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error,
					"Failed to update bulk Memo count= " + bulkMemoDumpId, e);
		}
		logger.info("updated memo sent count in bulk memo dump " + bulkMemoDumpId);

	}
	
}