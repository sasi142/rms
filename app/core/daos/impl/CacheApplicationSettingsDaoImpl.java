package core.daos.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import core.daos.CacheApplicationSettingsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.JsonNode;


import core.daos.CacheChannelDao;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import play.libs.Json;
import redis.clients.jedis.Jedis;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Repository
public class CacheApplicationSettingsDaoImpl implements CacheApplicationSettingsDao, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(CacheApplicationSettingsDaoImpl.class);
	@Autowired
	public RedisConnection redisConnection;

	@Autowired
	public Environment env;

    private String applicationSettingBucketName;

    @Override
    public void afterPropertiesSet() throws Exception {
        applicationSettingBucketName = env.getProperty(Constants.REDIS_APPLICATION_SETTINGS_STORE);
        logger.info("applicationSettingBucketName: " + applicationSettingBucketName);
    }


    @Override
    public ArrayNode getApplicationSettingByOrgId(@NonNull Integer orgId) {    	
    	ObjectMapper mapper = new ObjectMapper();
		ArrayNode rootNode = mapper.createArrayNode();
        Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
        try {
            String appSettingsStr = jedis.hget(applicationSettingBucketName, String.valueOf(orgId));
          //  JSONArray settingsJson = new JSONArray(json);
            if (appSettingsStr != null) {            	
    				ArrayNode node = (ArrayNode) mapper.readTree(appSettingsStr);
    				if (node.isArray() && node.size() > 0) {
    					for (int indx = 0; indx < node.size(); indx++) {
    						JsonNode json = node.get(indx);
    						logger.info("json: "+json.toString());
    						rootNode.add(json);
    					}
    				}
    			}
        } catch (Exception e) {
            logger.error("Retrieving ApplicationSettings from Redis Cache failed: " + e);
        } finally {
            redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
        }
        return rootNode;
    }




}
