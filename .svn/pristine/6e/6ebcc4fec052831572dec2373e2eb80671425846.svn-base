package core.daos.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import core.daos.CacheConnectionInfoDao;
import core.entities.ConnectionInfo;
import core.redis.RedisConnection;

@Repository("LocalCache")
public class LocalConnectionInfoDaoImpl implements CacheConnectionInfoDao {
	
	final static Logger logger = LoggerFactory.getLogger(LocalConnectionInfoDaoImpl.class);

	@Autowired
	public RedisConnection								redisConnection;

	@Autowired
	public Environment									env;

	// <userId,<uuid,connectionInfo>
	private Map<Integer, Map<String, ConnectionInfo>>	userConnectionMap	= new ConcurrentHashMap<Integer, Map<String, ConnectionInfo>>();

	public LocalConnectionInfoDaoImpl() {
		logger.info("Using LocalCache");
	}

	@Override
	public void create(Integer userId, ConnectionInfo info) {
		Long t1 = System.currentTimeMillis();
		logger.debug("Create ConnectionInfo in LocalCache for user " + userId);
		Map<String, ConnectionInfo> connInfoMap = null;
		if (userConnectionMap.containsKey(userId)) {
			connInfoMap = userConnectionMap.get(userId);
		} else {
			connInfoMap = new ConcurrentHashMap<String, ConnectionInfo>();
			userConnectionMap.put(userId, connInfoMap);
		}
		connInfoMap.put(info.getUuid(), info);
		logger.debug("Created ConnectionInfo in Cache for user " + userId);
		logger.debug("Time taken by create(userId,info) : " + (System.currentTimeMillis() - t1));
	}

	@Override
	public ArrayNode getAll(Integer userId) {
		long t1 = System.currentTimeMillis();
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode rootNode = mapper.createArrayNode();
		logger.debug("Get All Connections in Cache for user " + userId);
		if (userConnectionMap.containsKey(userId)) {
			for (ConnectionInfo connInfo : userConnectionMap.get(userId).values()) {
				rootNode.add(connInfo.getJson());
			}
			logger.debug("Got All Connections in Cache for user " + userId + " as " + rootNode.size());
		}
		logger.debug("Time taken by getAll(userId) : " + (System.currentTimeMillis() - t1));
		return rootNode;
	}

	@Override
	public Map<Integer, ArrayNode> getAll(List<Integer> userIds) {
		// TODO unused method, so not implemented
		return null;
	}

	@Override
	public void remove(Integer userId, String uuid) {
		logger.info("removing connection for:" + userId + "& uuid:" + uuid);
		Map<String, ConnectionInfo> connInfoMap = null;
		if (userConnectionMap.containsKey(userId)) {
			connInfoMap = userConnectionMap.get(userId);
			if (connInfoMap.containsKey(uuid)) {
				connInfoMap.remove(uuid);
			}
		}
		logger.debug("removed connection successfully");
	}

	@Override
	public void removeAll(Integer userId) {
		logger.debug("remove all connection info for user: " + userId);
		if (userConnectionMap.containsKey(userId)) {
			userConnectionMap.remove(userId);
		}
		logger.debug("removed all connection info for user: " + userId);
	}

	@Override
	public Map<String, ArrayNode> getAll() {
		logger.debug("Get All Connections in Cache ");
		Map<String, ArrayNode> userConsMap = new HashMap<>();
		for (Integer userId : userConnectionMap.keySet()) {
			userConsMap.put(userId.toString(), getAll(userId));
		}
		return userConsMap;
	}

	@Override
	public JsonNode get(Integer userId, String uuid) {
		logger.debug("Get connection for:" + userId + "& uuid:" + uuid);
		Map<String, ConnectionInfo> connInfoMap = null;
		ConnectionInfo connInfo = null;
		JsonNode json = null;

		if (userConnectionMap.containsKey(userId)) {
			connInfoMap = userConnectionMap.get(userId);
			if (connInfoMap.containsKey(uuid)) {
				connInfo = connInfoMap.get(uuid);
				json = connInfo.getJson();
			}
		}

		logger.debug("No connection for:" + userId + "& uuid:" + uuid);
		return json;

	}

	@Override
	public void removeStaleActors(Map<String, ConnectionInfo> actorMap, String ipAddr) {
		// TODO unused method, so not implemented
	}

	@Override
	public Map<String, List<JsonNode>> getStaleActors(Map<String, ConnectionInfo> actorMap, String ipAddr) {
		logger.info("Get all stale Actors for input ip " + ipAddr + " and actors" + (actorMap == null ? null : actorMap.keySet().toString()));
		Set<String> activeActorIds = actorMap.keySet();
		Map<String, List<JsonNode>> staleConnections = new HashMap<>();
		List<JsonNode> list = new ArrayList<>();
		for (Integer userId : userConnectionMap.keySet()) {
			Map<String, ConnectionInfo> connInfoMap = userConnectionMap.get(userId);
			for (String uuid : connInfoMap.keySet()) {
				ConnectionInfo connInfo = connInfoMap.get(uuid);
				if (ipAddr.equalsIgnoreCase(connInfo.getIp())) {
					if (!activeActorIds.contains(uuid)) {
						list.add(connInfo.getJson());
					}
				}
			}
			staleConnections.put(userId.toString(), list);
			logger.info("Added stale connection for " + userId);
		}
		logger.info("Got all stale Actors for input ip " + ipAddr);
		return staleConnections;
	}

	@Override
	public void updateMapOpenStatus(Integer userId, String uuid, Integer status) {
		logger.info("updating map window open status " + status + " for :" + userId + "& uuid:" + uuid);
		Map<String, ConnectionInfo> connInfoMap = null;
		if (userConnectionMap.containsKey(userId)) {
			connInfoMap = userConnectionMap.get(userId);
			if (connInfoMap.containsKey(uuid)) {
				connInfoMap.get(uuid).setStatus(status);
			}
		}
		logger.info("updated map window open status successfully");
	}

	@Override
	public Map<String, String> getAllConnections() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> remove(Map<Integer, List<String>> connectionMap) {
		return Collections.emptyList();
	}
}
