package core.services;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import core.entities.*;
import messages.ConnId;
import messages.UserConnection;

public interface UserConnectionService {
	public void initialiseUserConnection(UserConnection userConnection);
	public void closeUserConnection(UserConnection userConnection);
	public int sendMessageToActor(Integer userId, JsonNode message, ActorRef ref);	
	public void closeAllUserConnection(Integer Id);
	public int sendMessageToActor(Integer userId, String uuid, JsonNode message);
	public Boolean deleteIdleConnection(Integer Id, JsonNode node);
	public Set<String> sendMessageToActor(Integer userId, JsonNode json, ActorRef actorRef, List<String> clientIds);
	public void updateUserInUserContext(UserConnection userConnection);	
	public int sendMessageToActorWithOpenMapWindow(Integer userId, JsonNode message);
	public Set<String> sendMessageToActor(Integer userId, UserConnection connection, ChatMessage message, List<String> clientIds);
	public List<Integer> sendMessageToActorSet(Set<Integer> userIds, JsonNode message, List<String> clientIds);	
//	void onSocketConnection(User user, ConnId connId);
	void onSocketClosed(User user, ConnId connId);
	void validateUserToken(UserConnection userConnection, Long lsatTokenValidationTime);
	String validateUserToken(UserContext userContext);
	void sendRemoveConnectionMessageToActor(ActorRef ref, JsonNode message);
	int sendSessionExpiredMessageToActor(Integer guestUserId, JsonNode node1);
//	void agentAssignedEvent(Event event);


}
