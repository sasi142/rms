package core.daos.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import redis.clients.jedis.Jedis;
import core.daos.CacheOpenMapInfoDao;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;

@Repository
public class CacheOpenMapInfoDaoImpl implements CacheOpenMapInfoDao, InitializingBean{
	
	final static Logger logger = LoggerFactory.getLogger(CacheOpenMapInfoDaoImpl.class);	

	@Autowired
	public RedisConnection	redisConnection;

	@Autowired
	public Environment		env;

	private String			openMapWindowBucketName;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		openMapWindowBucketName = env.getProperty(Constants.REDIS_IMS_USER_MAP_WINDOW_STORE);
		logger.info("openMapWindowBucketName: " + openMapWindowBucketName);
	}

	@Override
	public void create(Integer userId) {
		logger.debug("add user " + userId + " in OpenMap");
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Ims);
		try {
			jedis.sadd(getOpenMapWindowBucketName(), String.valueOf(userId));
		} finally {
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Ims);
		}
		logger.debug("User added");
	}

	@Override
	public void remove(Integer userId) {
		logger.debug("remove user " + userId + " in OpenMap");
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Ims);
		try {
			jedis.srem(getOpenMapWindowBucketName(), String.valueOf(userId));
		} finally {
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Ims);
		}
		logger.debug("User removed");
	}
	
	public String getOpenMapWindowBucketName() {
		return openMapWindowBucketName;
	}
}
