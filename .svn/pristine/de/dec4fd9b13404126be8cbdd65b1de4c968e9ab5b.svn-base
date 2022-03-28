package core.daos;

import java.util.List;
import java.util.Set;

import core.entities.VideokycAgentQueue;

public interface VideokycAgentQueueDao  extends JpaDao<VideokycAgentQueue>  {
//	List<VideokycAgentQueue> findByAgentId(Integer agentId);
//	VideokycAgentQueue findByGroupId(Long groupId, Byte agentStatus);
	void changeVideoKycStatus(Integer videoKycId, String status, Integer userId, Integer orgId);

//	void markAllAgentInactive();
	VideokycAgentQueue getByGroupAndAgentId(Integer userId, Long groupId);
	Integer GetVideoKycGroupCallWait(Integer groupId, Byte priority, Short breathingTime,
			Integer avgCallDuration);
//	void updateAgentQueue(Integer agentId, Byte agentStatus);
	List<VideokycAgentQueue> getCallWaitTime(Short breathingTime, Integer avgCallDuration, Boolean syncStatus);		
	void refreshVideoKycCache(List<Integer> videoKycIds, List<Long> groupIds);
	void updateAgentQueueStatus(Integer agentId, Byte agentStatus);
	
}
