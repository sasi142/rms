package core.daos.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.JsonNode;
import core.daos.CacheChannelDao;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import play.libs.Json;
import redis.clients.jedis.Jedis;

@Repository
public class CacheChannelDaoImpl implements CacheChannelDao, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(CacheChannelDaoImpl.class);
	@Autowired
	public RedisConnection redisConnection;

	@Autowired
	public Environment env;

	public String UserBucketName;

	@Override
	public void afterPropertiesSet() throws Exception {
		UserBucketName = env.getProperty(Constants.REDIS_IMS_CHANNEL_STORE);
		logger.info("UserBucketName: " + UserBucketName);
	}

	@Override
	public JsonNode find(Integer channelId) {
		logger.info("get channel info for id : " + channelId);
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		JsonNode json = null;
		try {
			String channelStr = jedis.hget(UserBucketName, String.valueOf(channelId)); // get json from redis
			if (channelStr != null && !"".equalsIgnoreCase(channelStr)) {
				json = Json.parse(channelStr);
			}
			logger.debug("returning channel info for id : " + channelId);
			return json;
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
	}
}
