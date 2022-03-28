package core.redis;

import com.fasterxml.jackson.databind.JsonNode;
import core.entities.PubSubChannelMessage;
import core.utils.Enums.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import play.libs.Json;
import redis.clients.jedis.Jedis;

import java.util.Map;

@Component
public class RedisPublisher implements InitializingBean {
	
	final static Logger logger = LoggerFactory.getLogger(RedisPublisher.class);
	
	@Autowired
	public RedisConnection redisConnection;

	public void publish(Map<String, PubSubChannelMessage> channelMessages) {
		Jedis jedis = null;
		try{
			jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
			for (Map.Entry<String, PubSubChannelMessage> entry : channelMessages.entrySet()) {
				String channel = entry.getKey();
				PubSubChannelMessage pMessage = entry.getValue();
				JsonNode message = Json.toJson(pMessage);
				jedis.publish(channel, message.toString());
				logger.debug("Publish message type {} on channel {} to connectionIds {}",
						pMessage.getRmsMessageType(), channel, pMessage.getConnectionIds());
			}
		}
		catch(Exception e){
				logger.error("Publish channel messages {} failed on jedis",
					channelMessages, e);
			throw e;
		}
		finally{
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}
	}

	public void publish(String channel, PubSubChannelMessage pMessage) {
		JsonNode message = Json.toJson(pMessage);
		logger.debug("Publish message type {} on channel {} to connectionIds {}",
				pMessage.getRmsMessageType(), channel, pMessage.getConnectionIds());

		Jedis jedis = null;
		try{
			jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
			jedis.publish(channel, message.toString());
		}
		catch(Exception e){
			logger.error("Publish message type {} on channel {} to connectionIds {} failed on jedis",
					pMessage.getRmsMessageType(), channel, pMessage.getConnectionIds(), e);
			throw e;
		}
		finally{
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		//publisherJedis = redisConnection.getMasterConnection(DatabaseType.Rms);	
	}
}