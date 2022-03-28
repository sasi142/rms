package core.daos.impl;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import core.utils.CacheUtil;
import core.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;


import redis.clients.jedis.Jedis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jedis.lock.JedisLock;

import core.daos.CacheConnectionInfoDao;
import core.entities.ConnectionInfo;
import core.exceptions.ApplicationException;
import core.exceptions.InternalServerErrorException;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import core.utils.Enums.ErrorCode;

@Repository("RedisCache")
public class CacheConnectionInfoDaoImpl implements CacheConnectionInfoDao, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(CacheClientCertificateDaoImpl.class);

	@Autowired
	public RedisConnection	redisConnection;

	@Autowired
	public Environment		env;

	@Autowired
	private CacheUtil cacheUtil;

	@Autowired
	private JsonUtil jsonUtil;


	private ObjectMapper mapper;

	private String			userConnectionBucketName;

	@Override
	public void afterPropertiesSet() throws Exception {
		userConnectionBucketName = env.getProperty(Constants.REDIS_RMS_USER_CONNECTION_STORE);
		logger.info("userConnectionBucketName: " + userConnectionBucketName);
		mapper = new ObjectMapper();
	}

	public CacheConnectionInfoDaoImpl() {
		logger.info("Using RedisCache");
	}

	@Override
	public void create(Integer userId, ConnectionInfo info) {
		Long t1 = System.currentTimeMillis();
		logger.debug("Create ConnectionInfo in Cache for user " + userId);
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		JedisLock lock = getLock(userId, jedis);
		try {
			lock.acquire();
			logger.debug("Got JEDIS Lock for user " + userId);
			ArrayNode rootNode = mapper.createArrayNode();
			String consStr = jedis.hget(userConnectionBucketName, String.valueOf(userId));
			logger.debug("Loaded Connection info for user " + userId + " from cache as " + consStr);
			if (consStr != null) {
				rootNode = (ArrayNode) mapper.readTree(consStr);
				if (rootNode.isArray()) {
					rootNode.add(info.getJson());
				}
			} else {
				ObjectNode jsonNode = info.getJson();
				rootNode.add(jsonNode);
			}
			String value = rootNode.toString();
			jedis.hset(userConnectionBucketName, String.valueOf(userId), value);
			logger.debug("Created ConnectionInfo in Cache for user " + userId);
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			lock.release();
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}
		logger.debug("Time taken by create(userId,info) : " + (System.currentTimeMillis() - t1));
	}

	@Override
	public ArrayNode getAll(Integer userId) {
		long t1 = System.currentTimeMillis();
		logger.debug("Get All Connections in Cache for user " + userId);
		ArrayNode rootNode = mapper.createArrayNode();
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Rms);
		try {
			String consStr = jedis.hget(userConnectionBucketName, String.valueOf(userId));
			logger.debug("consStr: "+consStr);
			if (consStr != null) {
				ArrayNode node = (ArrayNode) mapper.readTree(consStr);
				if (node.isArray() && node.size() > 0) {
					for (int indx = 0; indx < node.size(); indx++) {
						JsonNode json = node.get(indx);
						logger.debug("json: "+json);
						rootNode.add(json);
					}
				}
			}
			logger.debug("Got All Connections in Cache for user " + userId + " as " + rootNode.size());
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}
		logger.debug("Time taken by getAll(userId) : " + (System.currentTimeMillis() - t1));
		return rootNode;
	}

	@Override
	public Map<Integer, ArrayNode> getAll(List<Integer> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		logger.debug("Get All Connections in Cache for users " + userIds.toString());
		//ArrayNode rootNode = mapper.createArrayNode();
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Rms);
		try {
			Map<Integer, ArrayNode> result = new HashMap<>(userIds.size());
			for (Integer userId : userIds) {
				logger.debug("Get connection for id " + userId.toString());
				String consStr = jedis.hget(userConnectionBucketName, String.valueOf(userId));
				logger.debug("conn Str: " + consStr);
				if (consStr != null) {
					ArrayNode node = (ArrayNode) mapper.readTree(consStr);
					result.put(userId, node);
				}
			}
			logger.debug("Got All Connections in Cache for users " + (userIds == null ? null : userIds.toString()));
			logger.debug(" connection size is " + result.size());
			return result;
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}
	}

	@Override
	public void remove(Integer userId, String uuid) {
		logger.debug("removing connection for:" + userId + "& uuid:" + uuid);
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		JedisLock lock = getLock(userId, jedis);
		try {
			lock.acquire();
			ArrayNode rootNode = mapper.createArrayNode();
			String consStr = jedis.hget(userConnectionBucketName, String.valueOf(userId));
			if (consStr != null) {
				ArrayNode node = (ArrayNode) mapper.readTree(consStr);
				if (node.isArray() && node.size() > 0) {
					for (int indx = 0; indx < node.size(); indx++) {
						JsonNode json = node.get(indx);
						if (json.findPath("uuid").asText().equalsIgnoreCase(uuid)) {
							continue;
						} else {
							rootNode.add(json);
						}
					}
					if (rootNode.size() > 0) {
						String value = rootNode.toString();
						jedis.hset(userConnectionBucketName, String.valueOf(userId), value);
					} else {
						jedis.hdel(userConnectionBucketName, String.valueOf(userId));
					}
				}
			}
			logger.debug("removed connection successfully");
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			lock.release();
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}
	}

	@Override
	public void removeAll(Integer userId) {
		logger.debug("remove all connection info for user: " + userId);
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		JedisLock lock = getLock(userId, jedis);
		try {
			lock.acquire();
			Long keys = jedis.hdel(userConnectionBucketName, String.valueOf(userId));
			logger.debug("number of keys deleted: " + keys);
			logger.debug("removed all connection info for user: " + userId);
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			lock.release();
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}
	}

	@Override
	public Map<String, ArrayNode> getAll() {
		logger.debug("Get All Connections in Cache ");
		ArrayNode rootNode = mapper.createArrayNode();
		Map<String, ArrayNode> userConsMap = new HashMap<>();
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Rms);
		try {
			Map<String, String> consMap = jedis.hgetAll(userConnectionBucketName);
			if (consMap != null && !consMap.isEmpty()) {
				for (Entry<String, String> entry : consMap.entrySet()) {
					String id = entry.getKey();
					String consStr = entry.getValue();
					ArrayNode node = (ArrayNode) mapper.readTree(consStr);
					if (consStr != null) {
						if (node.isArray() && node.size() > 0) {
							for (int indx = 0; indx < node.size(); indx++) {
								JsonNode json = node.get(indx);
								rootNode.add(json);
							}
						}
					}
					userConsMap.put(id, node);
				}
			}
			logger.debug("Got All Connections in Cache of size " + userConsMap.size());
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}
		return userConsMap;
	}

	@Override
	public JsonNode get(Integer userId, String uuid) {
		logger.debug("Get connection for:" + userId + "& uuid:" + uuid);
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Rms);
		try {
			String consStr = jedis.hget(userConnectionBucketName, String.valueOf(userId));
			if (consStr != null) {
				ArrayNode node = (ArrayNode) mapper.readTree(consStr);
				if (node.isArray() && node.size() > 0) {
					for (int indx = 0; indx < node.size(); indx++) {
						JsonNode json = node.get(indx);
						if (json.findPath("uuid").asText().equalsIgnoreCase(uuid)) {
							logger.debug("Got connection for:" + userId + "& uuid:" + uuid);
							return json;
						}
					}
				}
			}
			logger.debug("No connection for:" + userId + "& uuid:" + uuid);
			return null;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}
	}

	// LOCK DETAILS : https://github.com/abelaska/jedis-lock/blob/master/src/main/java/com/github/jedis/lock/JedisLock.java
	private synchronized JedisLock getLock(Integer userId, Jedis jedis) {
		JedisLock lock = new JedisLock(jedis, String.valueOf(userId), 10000, 30000);
		return lock;
	}

	/*
	 * (non-Javadoc)
	 * @see core.daos.CacheConnectionInfoDao#removeStaleActors(java.util.Set, java.lang.String)
	 */
	@Override
	public void removeStaleActors(Map<String, ConnectionInfo> actorMap, String ipAddr) {
		logger.debug("remove all stale Actors for input ip " + ipAddr + " and actors" + (actorMap == null ? null : actorMap.keySet().toString()));
		Set<String> activeActorIds = actorMap.keySet();
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Rms);
		try {
			Map<String, String> consMap = jedis.hgetAll(userConnectionBucketName);
			if (consMap != null && !consMap.isEmpty()) {
				for (Entry<String, String> entry : consMap.entrySet()) {
					ArrayNode rootNode = mapper.createArrayNode();
					String id = entry.getKey();
					String consStr = entry.getValue();
					ArrayNode node = (ArrayNode) mapper.readTree(consStr);
					if (consStr != null) {
						if (node.isArray() && node.size() > 0) {
							for (int indx = 0; indx < node.size(); indx++) {
								JsonNode json = node.get(indx);
								String ip1 = json.findPath("ip").asText();
								if (ipAddr.equalsIgnoreCase(ip1)) {
									String uuid = json.findPath("uuid").asText();
									if (!activeActorIds.contains(uuid)) {
										continue;
									} else {
										rootNode.add(json);
									}
								}
							}
						}
					}
					if (rootNode.size() > 0) {
						String value = rootNode.toString();
						jedis.hset(userConnectionBucketName, id, value);
						logger.debug("remove all stale Actors, Updated for user connection " + id);
					} else {
						jedis.hdel(userConnectionBucketName, id);
						logger.debug("remove all stale Actors, Deleted for user connection " + id);
					}
				}
			}
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}
	}

	@Override
	public Map<String, String> getAllConnections() {
		Map<String, String> consMap = null;
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Rms);
		try {
			consMap = jedis.hgetAll(userConnectionBucketName);
		} 
		catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} 
		finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}
		return consMap;
	}

	@Override
	public List<Integer> remove(Map<Integer, List<String>> connectionMap) {
		String[] userIds = connectionMap.keySet().stream().map(String::valueOf).toArray(String[]::new);
		Map<String, String> userConnections = cacheUtil.hmget(DatabaseType.Rms, userConnectionBucketName, userIds);

		List<String> deleteUserIds = new ArrayList<>();
		Map<String, String> updatedUserIds = new HashMap<>();
		for (Entry<String, String> entry : userConnections.entrySet()) {
			String sUserId = entry.getKey();
			Integer userId = Integer.parseInt(sUserId);
			String connection = entry.getValue();
			if (connection != null && connection.trim().length() > 0){
				ArrayNode retains = mapper.createArrayNode();
				List<String> retainedIds = new ArrayList<>();
				ArrayNode nodes = (ArrayNode) jsonUtil.readTree(connection);
				for (JsonNode node : nodes) {
					String uuid = node.findPath("uuid").asText("");
					if (connectionMap.get(userId).contains(uuid)){
						//connection is part of removal
						logger.info("Connection {} for user {} will be removed", uuid, userId);
					}
					else{
						//connection is part of retain
						logger.info("Connection {} for user {} will be retained", uuid, userId);
						retains.add(node);
						retainedIds.add(uuid);
					}
				}
				if (retains.size() == 0){
					logger.info("No connection left for user {}", userId);
					//we are left with nothing for this user
					//remove this user id from cache
					deleteUserIds.add(sUserId);
				}
				else{
					logger.info("Connections {} left for user {}", retainedIds, userId);
					//we are left with some nodes for this user
					updatedUserIds.put(sUserId, retains.toString());
				}
			}
			else{
				logger.warn("No connection detail found for use {}", userId);
			}
		}

		cacheUtil.pipeline("", DatabaseType.Rms, (p) -> {
			p.hdel(userConnectionBucketName, deleteUserIds.toArray(String[]::new));
			p.hmset(userConnectionBucketName, updatedUserIds);
		});
		return deleteUserIds.stream().map(Integer::parseInt).collect(Collectors.toList());
	}

	/*
	 * DB connections exists in Redis but no actor exists for the same. Delete respective DB connections
	 */

	@Override
	public Map<String, List<JsonNode>> getStaleActors(Map<String, ConnectionInfo> actorMap, String ipAddr) {
		logger.debug("Get all stale Actors for input ip " + ipAddr + " and actors" + (actorMap == null ? null : actorMap.keySet().toString()));
		Set<String> activeActorIds = actorMap.keySet();
		Map<String, List<JsonNode>> staleConnections = new HashMap<>();
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Rms);
		try {
			Map<String, String> consMap = jedis.hgetAll(userConnectionBucketName);
			if (consMap != null && !consMap.isEmpty()) {
				for (Entry<String, String> entry : consMap.entrySet()) {					
					String id = entry.getKey();
					String consStr = entry.getValue();
					ArrayNode node = (ArrayNode) mapper.readTree(consStr);
					List<JsonNode> list = new ArrayList<>();
					if (consStr != null) {
						if (node.isArray() && node.size() > 0) {
							for (int indx = 0; indx < node.size(); indx++) {
								JsonNode json = node.get(indx);
								String ip1 = json.findPath("ip").asText();
								if (ipAddr.equalsIgnoreCase(ip1)) {
									String uuid = json.findPath("uuid").asText();
									if (!activeActorIds.contains(uuid)) {
										list.add(json);
									}
								}
							}
						}
					}
					if (!list.isEmpty()) {
						staleConnections.put(id, list);
					}
				}
			}
		} catch (ApplicationException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}

		logger.debug("Added stale connections");
		return staleConnections;
	}

	@Override
	public void updateMapOpenStatus(Integer userId, String uuid, Integer status) {
		logger.debug("updating map window open status " + status + " for :" + userId + "& uuid:" + uuid);
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		JedisLock lock = getLock(userId, jedis);
		try {
			lock.acquire();
			String consStr = jedis.hget(userConnectionBucketName, String.valueOf(userId));
			if (consStr != null) {
				ArrayNode node = (ArrayNode) mapper.readTree(consStr);
				if (node.isArray() && node.size() > 0) {
					for (int indx = 0; indx < node.size(); indx++) {
						JsonNode json = node.get(indx);
						if (json.findPath("uuid").asText().equalsIgnoreCase(uuid)) {
							((ObjectNode) json).put("mapOpen", status);
							break;
						}
					}
					jedis.hset(userConnectionBucketName, String.valueOf(userId), node.toString());
				}
			}
			logger.debug("updated map window open status successfully");
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			lock.release();
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}
	}
}
