package core.daos;

import core.entities.VideoKycCustomerQueue;

import java.util.List;

public interface VideoKycCustomerQueueDao extends JpaDao<VideoKycCustomerQueue> {
	void deleteCustomerByIds(List<Integer> userIds);

	void deleteCustomerById(Integer userId);
	void add(Integer videoKycId, Long guestGroupId, Integer groupId, Integer guestUserId, Byte priority);
	
}
