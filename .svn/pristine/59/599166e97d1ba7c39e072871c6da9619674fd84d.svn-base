package core.daos.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import play.libs.Json;
import redis.clients.jedis.Jedis;

import com.fasterxml.jackson.databind.JsonNode;

import core.daos.CacheOrgDao;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;

@Repository
public class CacheOrgDaoImpl implements CacheOrgDao, InitializingBean {
	
	final static Logger logger = LoggerFactory.getLogger(CacheOrgDaoImpl.class);
	
	@Autowired
	public RedisConnection	redisConnection;

	@Autowired
	public Environment		env;

	public String			OrgBucketName;

	@Override
	public void afterPropertiesSet() throws Exception {
		OrgBucketName = env.getProperty(Constants.REDIS_IMS_ORGS_STORE);
		logger.info("OrgBucketName: " + OrgBucketName);
	}

	@Override
	public JsonNode find(Integer orgId) {
		logger.debug("get Org info for id : " + orgId);
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		JsonNode json = null;
		try {
			String orgStr = jedis.hget(OrgBucketName, String.valueOf(orgId)); // get json from redis
			if (orgStr != null && !"".equalsIgnoreCase(orgStr)) {
				json = Json.parse(orgStr);
			}
			logger.debug("returning Org info for id : " + orgId);
			return json;
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
	}
}
