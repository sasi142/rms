package core.daos.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import core.utils.CacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.daos.CacheUserDao;
import core.entities.User;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import play.libs.Json;
import redis.clients.jedis.Jedis;

@Repository
public class CacheUserDaoImpl implements CacheUserDao, InitializingBean {
	
	final static Logger logger = LoggerFactory.getLogger(CacheUserDaoImpl.class);
	
	@Autowired
	public RedisConnection	redisConnection;

	@Autowired
	public Environment		env;

	@Autowired
	private CacheUtil cacheUtil;

	public String			UserBucketName;

	private Integer unreadExpiryTime = 300000;
	private String userUnreadCountCacheStorePrefix;

	@Override
	public void afterPropertiesSet() throws Exception {
		UserBucketName = env.getProperty(Constants.REDIS_IMS_USER_STORE);
		logger.info("UserBucketName: " + UserBucketName);

		userUnreadCountCacheStorePrefix = env.getProperty(Constants.REDIS_RMS_UNREAD_COUNT_STORE_PREFIX);
		logger.info("UserBucketName: " + userUnreadCountCacheStorePrefix);

		if (env.getProperty(Constants.REDIS_UNREAD_COUNT_EXPIRY_TIME_MILLIS) != null){
			unreadExpiryTime = Integer.parseInt(env.getProperty(Constants.REDIS_UNREAD_COUNT_EXPIRY_TIME_MILLIS));
		}

		logger.info("unreadExpiryTime: " + unreadExpiryTime);
	}

	@Override
	public JsonNode find(Integer userId) {
		logger.debug("get user info for id : " + userId);
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		JsonNode json = null;
		try {
			String userStr = jedis.hget(UserBucketName, String.valueOf(userId));		
			if (userStr != null && !"".equalsIgnoreCase(userStr)) {
				json = Json.parse(userStr);
			}
			logger.debug("returning user info for id : " + userId);
			return json;
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
	}

	
	@Override
	public List<JsonNode> findAll(List<Integer> userIds) {
		logger.info("get user info for id : " + userIds);
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		List<JsonNode> jsonList = new ArrayList<>();
		try {
			List<String> userStrs = jedis.hmget(UserBucketName, String.valueOf(userIds)); // get json from redis
			logger.info("list of userStr returned from cache: " + userIds);
			
			if (userStrs != null && userStrs.size() > 0) {
				for(String userStr : userStrs) {					
					JsonNode json = Json.parse(userStr);
					jsonList.add(json);
				}				
			}
			logger.info("returning user info for ids : " + userIds);
			return jsonList;
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
	}

    @Override
    public void storeUserUnreadCount(Integer userId, String data) {
		cacheUtil.put(DatabaseType.Rms, userUnreadCountCacheStorePrefix, userId, data, unreadExpiryTime);
    }

	@Override
	public String getUserUnreadCount(Integer userId) {
		return cacheUtil.get(DatabaseType.Rms, userUnreadCountCacheStorePrefix, userId);
	}

	/*
	 * (non-Javadoc)
	 * @see core.daos.CacheUserDao#getUser(java.lang.Integer)
	 */
	public User getUser(Integer userId) {
		String userStr = null;
		User user = null;
		logger.debug("get User with userId " + userId);
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		try {
			userStr = jedis.hget(UserBucketName, String.valueOf(userId)); // get json from redis
			if (userStr != null && !"".equalsIgnoreCase(userStr)) {
				ObjectMapper mapper = new ObjectMapper();
				user = mapper.readValue(userStr, User.class);
			}
		} catch (IOException e) {
			logger.error("Error parsing User JSON String : " + userStr, e);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
		logger.debug("returning User with userId " + userId);
		return user;
	}

}
