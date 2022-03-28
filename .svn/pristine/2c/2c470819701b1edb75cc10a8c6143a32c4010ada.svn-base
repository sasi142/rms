package core.redis;

import core.entities.ChatMessage;
import core.services.NotificationService;
import core.utils.Enums;
import core.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import core.akka.utils.AkkaUtil;
import core.entities.RmsMessage;
import core.utils.Enums.DatabaseType;
import core.utils.Enums.RmsMessageType;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Collections;

@Component
public class RedisPubSubSubscriber implements InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(RedisPubSubSubscriber.class);

	@Autowired
	private RedisConnection	redisConnection;

	@Autowired
	private AkkaUtil akkaUtil;

	@Autowired
	private JsonUtil  jsonUtil;

	@Autowired
	private NotificationService notificationService;

	public void subscribe(String channel) {
		logger.info("subscribe to channel"+channel);
		Jedis jedis = null;
		try {
			JedisPubSub jedisPubSub = new JedisPubSub() {                 
				@Override
				public void onMessage(String channel, String message) {
					logger.debug("Channel " + channel + " has sent a message : " + message );
					try {
						JsonNode channelMessageNode = getPubSubChannelMessage(message);
						String messageType = channelMessageNode.get("rmsMessageType").asText();
						if (messageType.equals(RmsMessageType.DeleteActor.toString())) {
							ArrayNode arrayNode = (ArrayNode)channelMessageNode.get("connectionIds");
							if (arrayNode != null && arrayNode.size()> 0) {
								for (int index = 0; index < arrayNode.size(); index++) {	
									String uuid = arrayNode.get(index).asText();
									ActorRef ref = akkaUtil.getActorRef(uuid);
									if (ref != null) {
										ref.tell(PoisonPill.getInstance(), null);
									}
								}
							}
						}
						else if (messageType.equals(RmsMessageType.Out.toString())) {
							ArrayNode arrayNode = (ArrayNode)channelMessageNode.get("connectionIds");
							if (arrayNode != null && arrayNode.size()> 0) {
								for (int index = 0; index < arrayNode.size(); index++) {	
									String uuid = arrayNode.get(index).asText();
									ActorRef ref = akkaUtil.getActorRef(uuid);
									if (ref != null) {
										String node = channelMessageNode.get("json").asText();
										ObjectMapper mapper = new ObjectMapper();
										JsonNode jsonNode = mapper.readTree(node);
										RmsMessage rmsMessage = new RmsMessage(jsonNode, RmsMessageType.Out); 
										ref.tell(rmsMessage, null);
									}
								}
							}
						}
						else if (messageType.equals(RmsMessageType.MobilePush.toString())) {
							final int notificationType = channelMessageNode.get("notificationType").asInt();
							final String sChatMessage = channelMessageNode.get("chatMessage").asText();
							final int visibility = channelMessageNode.get("visibility").asInt();
							final int userId = channelMessageNode.get("userId").asInt();
							logger.info("Notify mobile push to user {} with notificationType {}, visibility {}, message {}", userId, notificationType, visibility, sChatMessage);
							final ChatMessage chatMessage = jsonUtil.read(sChatMessage, ChatMessage.class);
							final Enums.PushNotificationVisibility notificationVisibility = Enums.get(Enums.PushNotificationVisibility.class, (byte) visibility);
							notificationService.sendMobileNotification(notificationType, chatMessage, Collections.singletonList(userId), notificationVisibility);
						}
						else {
							logger.error("invalid message on pubsub channel");
						}
					} catch (Exception e) {
						logger.error("failed to get rms message", e);
					}
				}

				@Override
				public void onSubscribe(String channel, int subscribedChannels) {
					logger.info("Client is Subscribed to channel : "+ channel);
					logger.info("Client is Subscribed to "+ subscribedChannels + " no. of channels");
				}

				@Override
				public void onUnsubscribe(String channel, int subscribedChannels) {
					logger.info("Client is Unsubscribed from channel : "+ channel);
					logger.info("Client is Subscribed to "+ subscribedChannels + " no. of channels");
				}             
			};
			
			jedis = redisConnection.getSlaveConnection(DatabaseType.Rms);
			jedis.subscribe(jedisPubSub, channel);
			
			logger.info("subscribed to channel :"+channel);
		} catch(Exception ex) {         
			logger.error("Exception : ", ex);   
		} finally {
			if(jedis != null) {
				jedis.close();
			}
		}       
	}

	public JsonNode getPubSubChannelMessage(String json) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readTree(json);
		return jsonNode;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		
	}
}