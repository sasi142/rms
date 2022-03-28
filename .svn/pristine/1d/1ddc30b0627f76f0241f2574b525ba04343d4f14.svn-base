package core.daos.impl;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import core.daos.CacheConnectionInfoDao;
import core.daos.PubSubDao;
import core.entities.ConnectionInfo;
import core.entities.PubSubMessage;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;

@Repository
public class RedisPubSubDaoImpl extends JedisPubSub implements PubSubDao, InitializingBean {
	

	final static Logger logger = LoggerFactory.getLogger(RedisPubSubDaoImpl.class);

	@Autowired
	private Environment				env;

	private String					connectionInfoChannel;

	@Autowired
	public RedisConnection			redisConnection;

	private Boolean					subscribed	= false;

	@Autowired
	@Qualifier("CacheImpl")
	private CacheConnectionInfoDao	connectionInfoDao;

	public void subscribe() {
		if (!subscribed) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
					try {
						logger.info("subscribe to redis channel: " + connectionInfoChannel);
						jedis.subscribe(RedisPubSubDaoImpl.this, connectionInfoChannel);
						logger.info("Subscription ended.");
					}
					finally {
						redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
					}
				}
			});
			thread.start();
		}
	}

	@Override
	public void publishConnectionInfo(PubSubMessage message) {
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		try {
			String data = message.getJson().toString();
			jedis.publish(connectionInfoChannel, data);
		} finally {
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}
	}

	@Override
	public void unsubscribe() {
		this.unsubscribe(connectionInfoChannel);

	}

	@Override
	public void onMessage(String channel, String message) {
		if (connectionInfoChannel.compareToIgnoreCase(channel) == 0) {
			try {
				ConnectionInfo info = new ConnectionInfo(message);
				logger.info("onPMessage - channel:" + channel + ", message:" + info.getJson().toString());
			} catch (Exception e) {
			}
		}
	}

	@Override
	public void onSubscribe(String channel, int subscribedChannels) {
		logger.info("onSubscribe - channel:" + channel + ", subscribedChannels:" + subscribedChannels);
	}

	@Override
	public void onUnsubscribe(String channel, int subscribedChannels) {
		logger.info("onUnsubscribe - channel:" + channel + ", subscribedChannels:" + subscribedChannels);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		connectionInfoChannel = env.getProperty(Constants.WS_CONNECTION_INFO_REDIS_CHANNEL);
	}

	@Override
	public void onPMessage(String arg0, String arg1, String arg2) {

	}

	@Override
	public void onPSubscribe(String arg0, int arg1) {

	}

	@Override
	public void onPUnsubscribe(String arg0, int arg1) {

	}

}
