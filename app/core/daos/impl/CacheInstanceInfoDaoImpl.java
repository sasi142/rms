package core.daos.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import core.daos.CacheInstanceInfoDao;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import redis.clients.jedis.Jedis;

@Repository
public class CacheInstanceInfoDaoImpl implements CacheInstanceInfoDao, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(CacheInstanceInfoDaoImpl.class);
	
	@Autowired
	private RedisConnection	redisConnection;
	
	@Autowired
	private Environment env;
	
	private String pubsubInstanceInfoBucketName;

	@Override
	public void saveInstanceInfo(String instanceId, String json) {
		logger.debug("save instance info  to redis: {}, json {} ",instanceId ,json);
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		try {
			jedis.hset(pubsubInstanceInfoBucketName, instanceId, json);
			logger.debug("instance info saved");
		} finally {
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}		
	}

	@Override
	public void removeInstanceInfo(String instanceId) {
		logger.debug("remove instanceId info from redis: "+instanceId);
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		try {
			jedis.hdel(pubsubInstanceInfoBucketName, instanceId);
			logger.debug("removed instanceId");
		} finally {
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}			
	}

	@Override
	public Map<String, String> getAllInstanceInfo() {
		logger.debug("get all instance info list stored in redis");
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		try {
			Map<String, String> instanceInfoMap = jedis.hgetAll(pubsubInstanceInfoBucketName);
			logger.debug("list of instance info: "+instanceInfoMap.toString());
			return instanceInfoMap;
		} finally {
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}			
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		pubsubInstanceInfoBucketName = env.getProperty(Constants.REDIS_RMS_INSTANCE_INFO_STORE);
		logger.info("UserBucketName: " + pubsubInstanceInfoBucketName);		
	}
}
