package core.daos.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.JsonNode;
import core.daos.CacheContactsDao;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import redis.clients.jedis.Jedis;
import play.libs.Json;

@Repository
public class CacheContactsDaoImpl implements CacheContactsDao, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(CacheContactsDaoImpl.class);

	@Autowired
	public RedisConnection	redisConnection;

	@Autowired
	public Environment		env;

	private String			contactsBucketName;
	private String			orgUsersBucketName;
	private String			openChatWindowBucketName;
	private String			openMapWindowBucketName;

	//for follow
	private String 			channelUserBucketName;


	@Override
	public void afterPropertiesSet() throws Exception {
		contactsBucketName = env.getProperty(Constants.REDIS_IMS_CONTACTS_STORE);
		logger.info("contactsBucketName: " + contactsBucketName);

		orgUsersBucketName = env.getProperty(Constants.REDIS_ORG_USERS_STORE);
		logger.info("orgUsersBucketName: " + orgUsersBucketName);

		openChatWindowBucketName = env.getProperty(Constants.REDIS_IMS_USER_CHAT_WINDOW_STORE);
		logger.info("openChatWindowBucketName: " + openChatWindowBucketName);

		openMapWindowBucketName = env.getProperty(Constants.REDIS_IMS_USER_MAP_WINDOW_STORE);
		logger.info("openMapWindowBucketName: " + openMapWindowBucketName);

		//for follow
		channelUserBucketName   = env.getProperty(Constants.REDIS_IMS_CHANNEL_FOLLOWER_STORE);
		logger.info("channelUserBucketName: " + channelUserBucketName);

	}

	@Override
	public List<Integer> findAll(Integer userId, Integer orgId) {
		logger.info("find all contacts for " + userId + " and orgId " + orgId);
		List<Integer> contacts = new ArrayList<>();
		Set<String> contactSet = new HashSet<>();
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		try {
			Set<String> coworkers = jedis.smembers(getContactsBucketName(userId));
			if (coworkers != null)
				contactSet.addAll(coworkers);

			Set<String> orgContacts = jedis.smembers(getOrgUsersBucketName(orgId));
			if (orgContacts != null)
				contactSet.addAll(orgContacts);

			Iterator<String> ite = contactSet.iterator();
			while (ite.hasNext()) {
				String member = ite.next();
				contacts.add(Integer.valueOf(member));
			}
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
		logger.debug("returning all contacts, size = " + contacts.size());
		return contacts;
	}

	/*
	 * (non-Javadoc)
	 * @see core.daos.CacheContactsDao#findAllOrgContacts(java.lang.Integer)
	 */
	public List<Integer> findAllOrgContacts(Integer organizationId) {
		logger.debug("find all Org contacts for orgId " + organizationId);
		List<Integer> contactIds = new ArrayList<>();
		Set<String> contactSet = new HashSet<>();
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		try {
			Set<String> orgContacts = jedis.smembers(getOrgUsersBucketName(organizationId));
			if (orgContacts != null)
				contactSet.addAll(orgContacts);

			Iterator<String> ite = contactSet.iterator();
			while (ite.hasNext()) {
				String member = ite.next();
				contactIds.add(Integer.valueOf(member));
			}

		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
		logger.debug("returning all org contacts, size = " + contactIds.size());
		return contactIds;
	}


	//for follow
	public List<Integer> findAllChannelContacts(Integer channelId) {
		logger.debug("getting all contacts for channelId: " + channelId);
		List<Integer> contactIds = new ArrayList<>();
		Set<String> contactSet = new HashSet<>();
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		try {
			Set<String> channelContacts = jedis.smembers(getChannelUsersBucketName(channelId));
			if (channelContacts != null)
				contactSet.addAll(channelContacts);

			Iterator<String> ite = contactSet.iterator();
			while (ite.hasNext()) {
				String member = ite.next();
				contactIds.add(Integer.valueOf(member));
			}

		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
		logger.debug("returning all channel contacts, size = " + contactIds.size());
		return contactIds;
	}

	@Override
	public Boolean isInOrgContact(Integer organizationId, Integer contactId) {
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		logger.debug("check if contact " + contactId + " is in org contact list of Organization " + organizationId);
		try {
			boolean isOrgContact = jedis.sismember(getOrgUsersBucketName(organizationId), String.valueOf(contactId));
			logger.debug("for contact " + contactId + " and Organization " + organizationId + " isOrgContact status is " + isOrgContact);
			return isOrgContact;
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
	}

	@Override
	public Boolean isInContact(Integer userId, Integer contactId) {
		logger.debug("check if contact " + contactId + " is in contact list of userid " + userId);
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		try {
			boolean inContact = jedis.sismember(getContactsBucketName(userId), String.valueOf(contactId));
			logger.debug("for contact " + contactId + " and user " + userId + " isInContact status is " + inContact);
			return inContact;
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
	}

	@Override
	public JsonNode getUserObjectWithOpenChatWindow(Integer userId) {
		logger.debug("find all userIds who have opened chat window of user " + userId);		
		JsonNode json = null;	
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		String strContactIds = null;
		try {
			strContactIds = jedis.hget(getOpenChatWindowBucketName(), String.valueOf(userId)); // get json from redis
			if (strContactIds != null) {
				json = Json.parse(strContactIds);				
			}		
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
		logger.debug("returning all userIds who have opened chat window of user " );
		return json;
	}

	@Override
	public List<Integer> getUsersWithOpenChatWindow(Integer userId) {
		logger.debug("find all Registered userIds who have opened chat window of user " + userId);		
		List<Integer> contactIds = new ArrayList<>();
		try {
			JsonNode arrayNode = getUserObjectWithOpenChatWindow(userId);		
			JsonNode userIds =arrayNode.get("users");
			for (JsonNode connectionNode : userIds) {
				contactIds.add(connectionNode.asInt());			
			}
		}catch (Exception e) {
		  logger.error("No any result found : " );
		}
		return contactIds;
	}

	@Override
	public Set<Integer> getUsersWithOpenMapWindow(Integer userId, Integer orgId) {
		logger.debug("find all userIds who have opened chat window of user " + userId);
		Set<String> contactStrIds = new HashSet<String>();
		Set<Integer> contactIds = new HashSet<Integer>();
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		try {
			contactStrIds = jedis.sinter(getOpenMapWindowBucketName(), getContactsBucketName(userId));
			for (String strContact : contactStrIds) {
				contactIds.add(Integer.valueOf(strContact));
			}
			contactStrIds = jedis.sinter(getOpenMapWindowBucketName(), getOrgUsersBucketName(orgId));
			for (String strContact : contactStrIds) {
				contactIds.add(Integer.valueOf(strContact));
			}
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
		logger.debug("returning all userIds who have opened map window, size = " + contactIds.size());
		return contactIds;
	}

	private String getContactsBucketName(Integer userId) {
		return contactsBucketName + ":" + userId;
	}

	private String getOrgUsersBucketName(Integer orgId) {
		return orgUsersBucketName + ":" + orgId;
	}

	private String getChannelUsersBucketName(Integer channelId) {
		return channelUserBucketName + ":" + channelId;
	}

	private String getOpenChatWindowBucketName() {
		return openChatWindowBucketName;
	}

	private String getOpenMapWindowBucketName() {
		return openMapWindowBucketName;
	}

	@Override
	public Boolean isFollowing(Integer channelId, Integer contactId) {
		logger.debug("check if consumer " + contactId + " is following channel  " + channelId);
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Ims);
		try {
			boolean inContact = jedis.sismember(getChannelUsersBucketName(channelId), String.valueOf(contactId));
			logger.debug("Consumer " + contactId + " is following Channel " + channelId + " status" + inContact);
			return inContact;
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Ims);
		}
	}

}



