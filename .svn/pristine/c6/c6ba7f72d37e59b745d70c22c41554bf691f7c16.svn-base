package core.daos.impl;

import core.daos.VideoKycCustomerQueueDao;
import core.entities.VideoKycCustomerQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;
import java.util.List;

@Repository
public class VideoKycCustomerQueueDaoImpl extends AbstractJpaDAO<VideoKycCustomerQueue> implements VideoKycCustomerQueueDao {

	final static Logger logger = LoggerFactory.getLogger(VideoKycCustomerQueueDaoImpl.class);

	public VideoKycCustomerQueueDaoImpl() {
		super();
		setClazz(VideoKycCustomerQueue.class);
	}

	@Override
	public void deleteCustomerByIds(List<Integer> userIds) {
		logger.info("Mark customer Inactive in db, count {} customerId: {}", userIds.size(), userIds);
		Query query = entityManager.createNamedQuery("VideoKycCustomerQueue.MarkInactiveByIds");
		query.setParameter("updatedDate", System.currentTimeMillis());
		query.setParameter("customerIds", userIds);
		int count = query.executeUpdate();
		logger.info("Marked customer inactive query result {}", count);
	}

	@Override
	public void deleteCustomerById(Integer userId) {
		logger.info("Mark customer Inactive in db, customerId: {}", userId);
		Query query = entityManager.createNamedQuery("VideoKycCustomerQueue.MarkInactive");
		query.setParameter("updatedDate", System.currentTimeMillis());
		query.setParameter("customerId", userId);
		int count = query.executeUpdate();
		logger.info("Marked customer inactive query result {}", count);
	}
	
	@Override
	public void add(Integer videoKycId, Long guestGroupId, Integer groupId, Integer guestUserId, Byte priority) {
		logger.info("Add customer to queue. VideoKycId {}, GuestGroupId {}, GroupId {}, GuestUserId {}, Priority {}",
				videoKycId, guestGroupId, groupId, guestUserId, priority );
		StoredProcedureQuery spQuery = entityManager.createNamedStoredProcedureQuery("VideoKycCustomerQueue.Add");
		spQuery.setParameter("P_CustomerId", guestUserId);
		spQuery.setParameter("P_GroupId", groupId); 
		spQuery.setParameter("P_Priority", priority); // 1 ,2
		spQuery.setParameter("P_VideoKycId", videoKycId); // 0		
		spQuery.setParameter("P_GuestGroupId", guestGroupId);
		spQuery.execute();
		logger.info("Added customer to queue. VideoKycId {}, GuestGroupId {}, GroupId {}, GuestUserId {}, Priority {}",
				videoKycId, guestGroupId, groupId, guestUserId, priority );
	}
}
