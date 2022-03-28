/**
 * TODO: How to decide presence of actors
 * http://stackoverflow.com/questions/23331950/three-ways-to-know-existence-of-an-akka-actor
 * http://stackoverflow.com/questions/18012945/how-can-i-check-if-an-akka-actor-exists-akka-2-2
 * https://nickebbitt.wordpress.com/2014/04/01/akka-utils-get-a-single-actorref-for-a-path/
 */

package core.services;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.akka.actors.RmsActorSystem;
import core.akka.utils.AkkaUtil;
import core.daos.CacheConnectionInfoDao;
import core.daos.CacheOpenMapInfoDao;
import core.entities.*;
import core.exceptions.UnAuthorizedException;
import core.redis.RedisPublisher;
import core.utils.AuthUtil;
import core.utils.CommonUtil;
import core.utils.Constants;
import core.utils.Enums;
import core.utils.Enums.*;
import messages.ConnId;
import messages.UserConnection;
import org.agrona.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.RmsApplicationContext;

import java.security.SecureRandom;
import java.util.*;
import java.util.Map.Entry;

@Service
public class UserConnectionServiceImpl implements UserConnectionService, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(UserConnectionServiceImpl.class);

	@Autowired
	@Qualifier("CacheImpl")
	private CacheConnectionInfoDao cacheConnectionInfoDao;

	@Autowired
	private AkkaUtil akkaUtil;

	@Autowired
	private AuthUtil authUtil;

	@Autowired
	private Environment env;

	@Autowired
	private PresenceService presenceService;

	private String actorDispatcherName;

	private Long mobileConnectionIdletime = 21600000L; // 6 hrs

	private Long mobileConnectionPingIdletime = 90000L; // 90 Sec.

	@Autowired
	@Qualifier("RmsCacheService")
	private CacheService cacheService;

	@Autowired
	private CacheOpenMapInfoDao cacheOpenMapInfoDao;

	@Autowired
	private RedisPublisher redisPublisher;

	@Autowired
	private CommonUtil commonUtil;

	private String tokenValidtionUrl;

	@Autowired
	private EventTrackingService eventTrackingService;

	@Autowired
	private VideokycService videokycService;

	@Override
	public void initialiseUserConnection(UserConnection userConnection) {
		if(userConnection.getEx() == null) {
			// create akka actor
			logger.info("create actor for: " + userConnection.toString());
			long t1 = System.currentTimeMillis();
			//logger.debug("create actor for 1: " + userConnection.toString());
			userConnection.setActorRef(userConnection.getActorRef());
			ConnectionInfo connInfo = new ConnectionInfo(userConnection, System.currentTimeMillis());
			// save connection info to Redis
			ConnectionInfo info = new ConnectionInfo();
			info.setClientId(userConnection.getUserContext().getClientId());
			info.setIp(RmsApplicationContext.getInstance().getIp());
			info.setInstanceId(RmsApplicationContext.getInstance().getInstanceId());
			info.setUuid(userConnection.getUuid());
			info.setType(commonUtil.getConnectionType(userConnection.getUserContext().getClientId()));
			info.setTime(System.currentTimeMillis());
			info.setStatus(ConnectionStatus.Open.getId());
			info.setPingTime(System.currentTimeMillis());
			logger.debug("create actor for 2: " + userConnection.toString());
			cacheConnectionInfoDao.create(userConnection.getUserContext().getUser().getId(), info);
			logger.debug("create actor for 3: " + userConnection.toString());
			long t2 = System.currentTimeMillis();
			logger.debug("time required: " + userConnection.getUuid() + " - " + (t2 - t1));
			presenceService.sendPresenceInfoToContact(userConnection.getUserContext().getUser().getId());
			eventTrackingService.sendUserConnectionEvent(userConnection.getUserContext().getUser().getId(), null, UserEventType.ChatConnected.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);
			logger.info("created actor for: " + userConnection.toString());
		}
	}



	@Override
	public void closeUserConnection(UserConnection userConnection) {
		logger.info("closing UserConnection: " + userConnection.getUserContext().getUser().getId()
				+ "uuid:" + userConnection.getUuid());
		cacheConnectionInfoDao.remove(userConnection.getUserContext().getUser().getId(), userConnection.getUuid());
		presenceService.sendPresenceInfoToContact(userConnection.getUserContext().getUser().getId());

		// remove user map window open entry if this is the only connection where map is open
		ArrayNode connectionNodes = cacheConnectionInfoDao.getAll(userConnection.getUserContext().getUser().getId());
		int count = 0;
		if (connectionNodes != null && connectionNodes.size() > 0) {
			for (JsonNode connectionNode : connectionNodes) {
				if (!connectionNode.findPath("mapOpen").isMissingNode()
						&& connectionNode.findPath("mapOpen").asInt() == 1) {
					count++;
				}
			}
		}
		if (count == 0) {
			cacheOpenMapInfoDao.remove(userConnection.getUserContext().getUser().getId());
		}
		eventTrackingService.sendUserConnectionEvent(userConnection.getUserContext().getUser().getId(), null, UserEventType.ChatDisconnected.getId().byteValue(), EventTrackingSource.Server.getId().byteValue(), null);
		logger.info("closed UserConnection: " + userConnection.getUserContext().getUser().getId() + "uuid:"
				+ userConnection.getUuid() + " sent presence update.");
	}

	@Override
	public void closeAllUserConnection(Integer Id) {
		logger.info("Closing all user connections (terminate all actors) for user " + Id);
		ArrayNode arrayNode = cacheConnectionInfoDao.getAll(Id);
		if (arrayNode != null && arrayNode.size() > 0) {
			for (JsonNode node : arrayNode) {
				sendDeleteMessage(node);
			}
		}
		cacheConnectionInfoDao.removeAll(Id);

		// Removing user from map-window-open list.
		cacheOpenMapInfoDao.remove(Id);
		logger.info("Closed all user connections (terminated all actors) for user " + Id);
	}

	@Override
	public Set<String> sendMessageToActor(Integer userId, JsonNode message, ActorRef actorRef, List<String> clientIds) {
		Set<String> msgReceiverClients = new HashSet<String>();
		ArrayNode arrayNode = cacheConnectionInfoDao.getAll(userId);
		logger.info("sendMessageToActor::Got all user connections for user " + userId + " of size "
				+ ((arrayNode == null) ? 0 : arrayNode.size()));

		if (arrayNode != null && arrayNode.size() > 0) {
			String originalText = message.findPath("text").asText();				
			for (JsonNode node : arrayNode) {
				String clientId = node.findPath("cid").asText();
				logger.debug("cid: "+clientId+", clientId: "+clientIds.toString());
				if (clientIds.contains(clientId)) {
					logger.info("sendMessageToActor::sendClientId:"+clientId+" is present in the list of clientIds");
					if (message.has("text")) {					
						((ObjectNode) message).put("text", originalText);					
					}
					sendRmsMessage(userId, message, node);					
					logger.info("sendMessageToActor::Sent message to clientId:"+clientId);
					msgReceiverClients.add(clientId);
				}
			}
		}
		logger.info("sendMessageToActor::User " + userId + " has sent Messages clients " + msgReceiverClients.toString());
		return msgReceiverClients;
	}

	@Override
	public List<Integer> sendMessageToActorSet(Set<Integer> userIds, JsonNode message, List<String> clientIds) {
		logger.info("Send message to userIds: {}", userIds);
		Set<Integer> usersNeedPush = new HashSet<>();
		Map<Integer, ArrayNode> allUserConnections = cacheConnectionInfoDao.getAll(new ArrayList<>(userIds));
		if (allUserConnections.isEmpty()){
			logger.debug("No user connections found. Returning all of them for push notification. User Ids {}", userIds);
			usersNeedPush.addAll(userIds);
			return new ArrayList<>(usersNeedPush);
		}
		
		Map<String, List<String>> channelRecipients = new HashMap<>();
		for (Integer userId : userIds) {
			logger.debug("Check connection for userId: {}", userId);
			ArrayNode userConnections = allUserConnections.get(userId);
			if (userConnections == null || userConnections.size() == 0){
				//No connections found for this user			
				logger.debug("No connections found for this user {}. Add to push list.", userId);
				usersNeedPush.add(userId);
				continue;
			}
			Set<String> connectedClientIds = new HashSet<>();
//			boolean matchedClientConnectionFound = false;
			for (JsonNode ucNode : userConnections) {
				String clientId = ucNode.findPath("cid").asText();
				String connectionId = ucNode.findPath("uuid").asText();
				logger.debug("Process send messages to user {} for client {} connection {}", userId, clientId, connectionId);
				if (clientIds != null && !clientIds.isEmpty() && !clientIds.contains(clientId)){
					logger.debug("Client not found in the list for user {} for client {} connection {}", userId, clientId, connectionId);
					continue;
				}
//				matchedClientConnectionFound = true;
				ActorRef ref = akkaUtil.getActorRef(connectionId);
				if (ref != null) {
					logger.debug("Send messages to user {} for connection {} locally", userId, connectionId);
					RmsMessage rmsMessage = new RmsMessage(message, RmsMessageType.Out);
					ref.tell(rmsMessage, null);
				} else {
					String ip = ucNode.findPath("ip").asText();
					String instanceId = ucNode.findPath("instanceId").asText();
					if (!Strings.isEmpty(ip) && !Strings.isEmpty(instanceId)) {
						String channelName = getChannelName(ip, instanceId);
						List<String> connectionIds = channelRecipients.computeIfAbsent(channelName, k -> new ArrayList<>());
						connectionIds.add(connectionId);
						logger.debug("Send messages to user {} over channel {} for connection {}", userId, channelName, connectionId);
					}
					else {
						logger.debug("Send messages to user {} for connection {} could not be sent due to empty ip {} / empty instanceid {}", userId, connectionId, ip, instanceId);
					}
				}
				connectedClientIds.add(clientId);
			}
//			if (!matchedClientConnectionFound){
//				//No matched client connection found for user {}. Add to push list.
//				logger.debug("No matched client connection found for user {}. Add to push list.", userId);
//				usersNeedPush.add(userId);
//			}
			Boolean isSendPN = commonUtil.isAlwaysSendPushNofication(connectedClientIds);
			if (isSendPN) {
				logger.debug("Notification to user {} is required for clientIds {}", userId, connectedClientIds);
				usersNeedPush.add(userId);
			}
			else {
				logger.debug("Notification to user {} is not configured for clientIds {}", userId, connectedClientIds);
			}
		}

		// publish all messages to remaining server
		publishMessagesOnChannel(message, channelRecipients);
		return new ArrayList<>(usersNeedPush);
	}

	//	public void sendMessageToActor(List<Integer> userIds, JsonNode message, List<String> clientIds) {
	//		Map<String, List<String>> puslishMap = new HashMap<>();
	//		ArrayNode arrayNode = cacheConnectionInfoDao.getAll(userIds);
	//		if (arrayNode != null && arrayNode.size() > 0) {
	//			for (JsonNode node : arrayNode) {
	//				String clientId = node.findPath("cid").asText();
	//				if (clientIds.contains(clientId)) {
	//					ActorRef ref = akkaUtil.getActorRef(node);
	//					if (ref != null) {
	//						logger.debug("actor exists. send message " + message.toString());
	//						RmsMessage rmsMessage = new RmsMessage(message, RmsMessageType.Out);
	//						ref.tell(rmsMessage, null);
	//					} else {
	//						String uuid = node.findPath("uuid").asText();
	//						String ip = node.findPath("ip").asText();
	//						String instanceId = node.findPath("instanceId").asText();
	//						if (!Objects.isNull(ip) && !Strings.isEmpty(ip) && !Objects.isNull(instanceId) && !Strings.isEmpty(instanceId)) {
	//							String channelName = getChannelName(ip, instanceId);
	//							logger.debug("channel Name: "+channelName);
	//							List<String> connectionIds = puslishMap.get(channelName);
	//							if (connectionIds == null) {
	//								connectionIds = new ArrayList<>();
	//								puslishMap.put(channelName, connectionIds);
	//							}
	//							connectionIds.add(uuid);
	//						}
	//					}
	//				}
	//			}
	//		}
	//		// publish all messages to remaining server
	//		publishMessagesOnChannel(message, puslishMap);
	//	}

	private void publishMessagesOnChannel(JsonNode message, Map<String, List<String>> puslishMap) {
		logger.debug("sendMessageToActorSet::publishMessagesOnChannel: "+puslishMap.toString());
		Map<String, PubSubChannelMessage> channelMessages = new HashMap<>();
		String sMessage = message.toString();
		for (Entry<String, List<String>> entry: puslishMap.entrySet()) {
			String channel = entry.getKey();
			List<String> connectionIds = entry.getValue();
			PubSubChannelMessage channelMessage = new PubSubChannelMessage(sMessage, RmsMessageType.Out, connectionIds);
			channelMessages.put(channel, channelMessage);
		}
		redisPublisher.publish(channelMessages);
	}

	public int sendMessageToActor(Integer userId, JsonNode message, ActorRef ref1) {
		ArrayNode connectionNodes = cacheConnectionInfoDao.getAll(userId);
		logger.info("Got all user connections for user " + userId + " of size "
				+ ((connectionNodes == null) ? 0 : connectionNodes.size()));
		int count = 0;
		if (connectionNodes != null && connectionNodes.size() > 0) {
			for (JsonNode connectionNode : connectionNodes) {
				logger.info("Send message to user {}, connection {}, message {}", userId, connectionNode, message);
				sendRmsMessage(userId, message, connectionNode);
				count++;
			}
		}
		logger.debug("User " + userId + " has sent Messages clients " + count);
		return count;
	}

	@Override
	public int sendSessionExpiredMessageToActor(Integer userId, JsonNode message) {
		ArrayNode connectionNodes = cacheConnectionInfoDao.getAll(userId);
		logger.info("Got all user connections for user " + userId + " of size "
				+ ((connectionNodes == null) ? 0 : connectionNodes.size()));
		int count = 0;
		if (connectionNodes != null && connectionNodes.size() > 0) {
			for (JsonNode connectionNode : connectionNodes) {
				sendRmsMessage(userId, message, connectionNode);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					logger.error("threas sleep failed ", e1);
				}
				String uuid = connectionNode.findPath("uuid").asText();
				ActorRef ref = akkaUtil.getActorRef(connectionNode);
				sendDeleteMessage(connectionNode);
				//   removeUserConnection(userId, uuid);
				count++;
			}
		}
		logger.debug("User " + userId + " has sent Messages clients " + count);
		return count;
	}


	public int sendMessageToActor(Integer userId, String uuid, JsonNode message) {
		ArrayNode arrayNode = cacheConnectionInfoDao.getAll(userId);
		logger.debug("Got all user connections for user " + userId + " of size "
				+ ((arrayNode == null) ? 0 : arrayNode.size()));
		int count = 0;
		if (uuid != null && !"".equalsIgnoreCase(uuid)) {
			for (JsonNode node : arrayNode) {
				String uuid1 = node.findPath("uuid").asText();
				if (uuid1.equalsIgnoreCase(uuid)) {
					sendRmsMessage(userId, message, node);					
					count++;
				}
			}
		}
		logger.info("User " + userId + " has sent Messages clients " + count);
		return count;
	}

	public int sendMessageToActorWithOpenMapWindow(Integer userId, JsonNode message) {
		ArrayNode connectionNodes = cacheConnectionInfoDao.getAll(userId);
		logger.info("Got all user connections for user " + userId + " of size "
				+ ((connectionNodes == null) ? 0 : connectionNodes.size()));
		int count = 0;
		if (connectionNodes != null && connectionNodes.size() > 0) {
			for (JsonNode connectionNode : connectionNodes) {
				if (!connectionNode.findPath("mapOpen").isMissingNode()
						&& connectionNode.findPath("mapOpen").asInt() == 1) {
					sendRmsMessage(userId, message, connectionNode);
					count++;
				}
			}
		}
		logger.info("User " + userId + " has sent Messages clients " + count);
		return count;
	}

	private void sendRmsMessage(Integer Id, JsonNode message, JsonNode node) {	
		ActorRef ref = akkaUtil.getActorRef(node);
		if (ref != null) {
			logger.debug("send message " + message.toString());
			RmsMessage rmsMessage = new RmsMessage(akkaUtil.getConnId(node), message, RmsMessageType.Out);
			ref.tell(rmsMessage, null);
		} else {
			String uuid = node.findPath("uuid").asText();
			String ip = node.findPath("ip").asText();
			String instanceId = node.findPath("instanceId").asText();
			List<String> connectionIds = new ArrayList<>();
			connectionIds.add(uuid);
			if (!Objects.isNull(ip) && !Strings.isEmpty(ip) && !Objects.isNull(instanceId) && !Strings.isEmpty(instanceId)) {
				PubSubChannelMessage channelMessage = new PubSubChannelMessage(message.toString(), RmsMessageType.Out, connectionIds);
				String channelName = getChannelName(ip, instanceId);
				logger.debug("channelMessage: "+channelMessage);
				redisPublisher.publish(channelName, channelMessage);
			}
		}
	}

	public void sendRemoveConnectionMessageToActor(ActorRef ref, JsonNode message) {
		RmsMessage rmsMessage = new RmsMessage(message, RmsMessageType.Out);
		ref.tell(rmsMessage, null);		
	}

	@Override
	public Boolean deleteIdleConnection(Integer Id, JsonNode node) {
		logger.debug("delete User Connection for userId " + Id);
		String uuid = node.findPath("uuid").asText();
		logger.info("redandunt connection info. remove the connection info" + Id + "&uuid:" + uuid);
		removeUserConnection(Id, uuid);
		logger.debug("deleted User Connection for userId " + Id);
		return true;
	}

	public Set<String> sendMessageToActor(Integer userId, UserConnection connection, ChatMessage message,
			List<String> clientIds) {
		ActorRef actorRef = (connection != null) ? connection.getActorRef() : null;
		Set<String> msgReceiverClients = sendMessageToActor(userId, Json.toJson(message), actorRef, clientIds);
		logger.debug("sent Message To Actor received msgReceiverClients as " + msgReceiverClients);
		return msgReceiverClients;
	}

	public void updateUserInUserContext(UserConnection userConnection) {
		User user = cacheService.getUser(userConnection.getUserContext().getUser().getId(), true);
		userConnection.getUserContext().setUser(user);
		logger.debug("Refreshing updated user in context: " + user.getId());
	}

	private void removeUserConnection(Integer userId, String uuid) {
		cacheConnectionInfoDao.remove(userId, uuid);
		logger.debug("removed User Connection for userId " + userId);
		presenceService.sendPresenceInfoToContact(userId);
		logger.debug("In onClose, completed presence service");
	}

	private void sendDeleteMessage(JsonNode node) {
		ActorRef ref = akkaUtil.getActorRef(node);
		if (ref != null) {
			ref.tell(PoisonPill.getInstance(), null);
		}
		else {			
			String uuid = node.findPath("uuid").asText();
			String ip = node.findPath("ip").asText();
			List<String> connectionIds = new ArrayList<>();
			connectionIds.add(uuid);
			PubSubChannelMessage channelMessage = new PubSubChannelMessage(RmsMessageType.DeleteActor, connectionIds);
			String instanceId = node.findPath("instanceId").asText();
			if (!Objects.isNull(ip) && !Strings.isEmpty(ip) && !Objects.isNull(instanceId) && !Strings.isEmpty(instanceId)) {
				String channelName = getChannelName(ip, instanceId);
				redisPublisher.publish(channelName, channelMessage);
			}
		}
	}

	private String getChannelName(String ip, String instanceId) {
		SecureRandom rand = new SecureRandom();
		int randomNum = rand.nextInt(5) + 1;
		return Constants.PUBSUB_MESSAGE_CHANNEL_PREFIX+ip+"_"+instanceId+"_"+randomNum;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		actorDispatcherName = env.getProperty(Constants.USER_CONNECTION_ACTOR_DISPATCHER);
		logger.info("actorDispatcherName: " + actorDispatcherName);

		mobileConnectionIdletime = Long.valueOf(env.getProperty(Constants.MOBILE_CONNECTION_IDLE_TIME));
		logger.info("mobileConnectionIdletime: " + mobileConnectionIdletime);

		mobileConnectionPingIdletime = Long.valueOf(env.getProperty(Constants.MOBILE_CONNECTION_PING_IDLE_TIME));
		logger.info("mobileConnectionPingIdletime: " + mobileConnectionPingIdletime);

		tokenValidtionUrl = env.getProperty(Constants.IMS_VALIDATE_TOKEN_URL);
		logger.info("tokenValidtionUrl: " + tokenValidtionUrl);
	}

//	@Override
//	public void onSocketConnection(User user, ConnId connId) {
//		Event event = new Event(connId);
//		event.setType(EventType.SocketConnect.getId());
//		Map<String, String> data = new HashMap<>();
//		data.put("userId", String.valueOf(user.getId()));
//		event.setData(data);
//		logger.debug("handle event " + event.toString());
//		RmsActorSystem.getEventRouterActorRef().tell(event, null);
//	}

//	@Override
//	public void agentAssignedEvent(Event event) {
//		logger.info("Sending agent assigned event with id {}", event.getId());
//		RmsActorSystem.getEventRouterActorRef().tell(event, null);
//	}

	@Override
	public void validateUserToken(UserConnection userConnection, Long lastTokenValidationTime) {
		logger.info("ValidateTokenStarted");

		logger.info("validate token url to create context : -" + tokenValidtionUrl);
		UserContext userContext = userConnection.getUserContext();
		String clientId = userContext.getClientId();
		String encodedToken = userContext.getToken();
		String requestId = userContext.getRequestId();
		try {
			authUtil.executeRESTApi(tokenValidtionUrl, encodedToken, clientId, requestId, null);
			logger.info("token validated successfully");
		} catch (Exception e) {
			logger.info("failed to validate token ", e);
			ObjectNode node = Json.newObject();
			node.put("type", MessageType.Event.getId());
			node.put("subtype", EventType.SessionExpired.getId());
			node.put("reason", SessionExpiryReason.TokenNotValidated.getName());
			logger.info("send msg to actor" +node.toString());
			logger.info("send msg to actor"+userConnection.getActorRef());
			sendRemoveConnectionMessageToActor(userConnection.getActorRef(), node);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				logger.error("threas sleep failed ", e1);
			}
			throw new UnAuthorizedException(Enums.ErrorCode.InvalidToken, Enums.ErrorCode.InvalidToken.getName());
		}
	}


	@Override
	public String validateUserToken(UserContext userContext) {
		logger.info("ValidateTokenStarted");
		logger.info("validate token url to create context : -" + tokenValidtionUrl);
		String clientId = userContext.getClientId();
		String encodedToken = userContext.getToken();
		String requestId = userContext.getRequestId();
		String resp = null;
		try {
			resp = authUtil.executeRESTApi(tokenValidtionUrl, encodedToken, clientId, requestId, null);
			logger.info("token validated successfully");
		} catch (Exception e) {
			logger.info("failed to validate token ");
			throw new UnAuthorizedException(Enums.ErrorCode.InvalidToken, Enums.ErrorCode.InvalidToken.getName());
		}
		return resp;
	}



	public void onSocketClosed(User user, ConnId connId) {
		Event event = new Event(connId);
		event.setType(EventType.SocketDisConnect.getId());
		Map<String, String> data = new HashMap<>();
		data.put("userId", String.valueOf(user.getId()));
		event.setData(data);		
		logger.debug("handle event " + event.toString());
		RmsActorSystem.getEventRouterActorRef().tell(event, null);
	}
}
