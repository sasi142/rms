package messages;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import akka.actor.ActorRef;
import core.entities.UserContext;
import core.exceptions.ApplicationException;


@JsonInclude(Include.NON_NULL)
public class UserConnection implements Serializable {
	private static final long serialVersionUID = 1L;
	private UserContext userContext;
	private String uuid;
	private ActorRef actorRef;
	private Set<Integer> recipientIds = new HashSet<Integer>();
	private Boolean hasSentUnavailableMessage = false;
	private Boolean hasSentUnregisteredMessage = false;
	private ApplicationException ex;

	private final ConnId connId;

	public UserConnection(String uuid, String clientId, ApplicationException ex) {
		this.uuid = uuid;
		this.ex = ex;
		this.connId = new ConnId(-1, clientId, uuid);
	}

	public UserConnection(UserContext userContext, String uuid) {	
		this.userContext = userContext;
		this.uuid = uuid;
		this.connId = new ConnId(userContext.getUser() != null ? userContext.getUser().getId() : -1, userContext.getClientId(), uuid);
	}
	public UserContext getUserContext() {
		return userContext;
	}
	public void setUserContext(UserContext userContext) {
		this.userContext = userContext;
	}

	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public ActorRef getActorRef() {
		return actorRef;
	}
	public void setActorRef(ActorRef actorRef) {
		this.actorRef = actorRef;
	}
	public Set<Integer> getRecipientIds() {
		return recipientIds;
	}
	public void setRecipientIds(Set<Integer> recipientIds) {
		this.recipientIds = recipientIds;
	}	
	public Boolean getHasSentUnavailableMessage() {
		return hasSentUnavailableMessage;
	}	
	public void setHasSentUnavailableMessage(Boolean hasSentUnavailableMessage) {
		this.hasSentUnavailableMessage = hasSentUnavailableMessage;
	}	
	public Boolean getHasSentUnregisteredMessage() {
		return hasSentUnregisteredMessage;
	}	
	public void setHasSentUnregisteredMessage(Boolean hasSentUnregisteredMessage) {
		this.hasSentUnregisteredMessage = hasSentUnregisteredMessage;
	}

	public Exception getEx() {
		return ex;
	}
	public void setEx(ApplicationException ex) {
		this.ex = ex;
	}


	@Override
	public String toString() {
		return "UserConnection [userContext=" + userContext + ", uuid=" + uuid+ "]";
	}

	public ConnId getConnId() {
		return connId;
	}
}
