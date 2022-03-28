package core.services;

import java.util.List;
import java.util.Optional;

import core.entities.User;
import core.entities.VideokycAgentQueue;
import core.entities.projections.VideoKyc;
import org.springframework.lang.NonNull;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface VideokycService {	
	void onUserConnection(User user);
	void onUserDisconnect(User user);
	void addAgentInQueue(List<Integer> members, Long groupId);
	void removeAgentFromQueue(List<Integer> members, Long groupId);		
	Integer getGroupCallWaitTime(Integer groupId, Integer priority);
	void updateAgentStatusInAgentQueue(Integer userId);
    void changeUserStatusInQueue(Integer userId);
	List<VideokycAgentQueue> getCallWaitTime(Short breathingTime, Integer avgCallDuration, Boolean syncStatus);
	void deleteCustomerById(Integer customerId);
	ObjectNode handleCallWaitChange(List<VideokycAgentQueue> customers);

	void removeCustomersFromQueue(List<Integer> userIds);

	Optional<VideoKyc> getVideoKycStatusByGroupId(Long groupId);
}