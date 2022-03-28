package core.daos.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import core.daos.CacheUserVideoKycDao;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import play.libs.Json;
import redis.clients.jedis.Jedis;

@Repository
public class CacheUserKycDaoImpl implements CacheUserVideoKycDao, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(CacheUserKycDaoImpl.class);
	@Autowired
	public RedisConnection redisConnection;

	@Autowired
	public Environment env;

	public String userVideoKycBucketName;
	public String videoKycBucket;

	@Override
	public void afterPropertiesSet() throws Exception {
		userVideoKycBucketName = env.getProperty(Constants.REDIS_IMS_USER_VIDEOKYC_STORE);
		logger.info("userVideoKycBucketName: " + userVideoKycBucketName);
		
		videoKycBucket = env.getProperty(Constants.REDIS_IMS_VIDEOKYC_STORE);
		logger.info("videoKycBucket: " + videoKycBucket);
	}

	@Override
	public JsonNode getVideoKycId(Integer userId) {
		logger.info("get kyc info for id : " + userId);
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		JsonNode json = null;
		try {
			String kycIdStr = jedis.hget(userVideoKycBucketName, String.valueOf(userId)); // get json from redis
			logger.info("userVideoKycBucketName: " + userVideoKycBucketName + ", kycIdStr: " + kycIdStr);
			if (kycIdStr != null && !"".equalsIgnoreCase(kycIdStr)) {
				String kycStr = jedis.hget(videoKycBucket, kycIdStr);
				logger.info("videoKycBucket: " + videoKycBucket + ", kycStr: " + kycStr);
				if (kycStr != null && !"".equalsIgnoreCase(kycStr)) {
					json = Json.parse(kycStr);
					((ObjectNode) json).put("id", kycIdStr);
				}
			}
			logger.info("returning kyc id : " + json);
			return json;
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
	}
}
