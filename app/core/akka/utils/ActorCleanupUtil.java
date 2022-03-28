/**
 * 
 */
package core.akka.utils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import core.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import core.daos.CacheConnectionInfoDao;
import core.daos.CacheInstanceInfoDao;
import core.daos.ElasticSearchDao;
import core.entities.ActorMonitor;
import core.entities.ConnectionInfo;
import core.services.VideokycService;
import core.utils.Constants;
import play.libs.Json;
import utils.RmsApplicationContext;

@EnableScheduling
@Component
public class ActorCleanupUtil implements InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(ActorCleanupUtil.class);

	@Autowired
	@Qualifier("CacheImpl")
	private CacheConnectionInfoDao	cacheConnectionInfoDao;

	@Autowired
	private CacheInstanceInfoDao cacheInstanceInfoDao;

	@Autowired
	private AkkaUtil akkaUtil;

	@Autowired
	private JsonUtil jsonUtil;

	@Autowired
	private Environment			env;

	private ActorMonitor actorMonitor;


	@Autowired
	private VideokycService videokycService;

	private Integer websocketIdleTime = 120000;	

	private Integer rmsInstanceIdleTime = 120000;

	@Autowired
	private ElasticSearchDao elasticSearchDao;

	private String esJobIndexName;

	/*
	 * This job is used to clean up actors that are not killed though socket stopped sending pings
	 * This happens in cases when client is sending ping but due to network issue, it is not reaching
	 * to server. Ideally Akka should have taken care of this. But this doesn't happen.
	 * Run this Job every 2 mins and remove all actors that has not receive any ping since last
	 * X min.
	 */
	@Scheduled(initialDelay = 60000, fixedDelayString = "${stale.actors.cleanup.job.time.interval}")
	public void cleanUpStaleActors() {
		String instanceId = RmsApplicationContext.getInstance().getInstanceId();
		logger.info("deleting stale actors. Removing actor and DB connections: "+instanceId);
		boolean isJobOn = Boolean.parseBoolean(env.getProperty(Constants.STALE_ACTORS_CLEANUP_JOB_ON));
		Long t1 = System.currentTimeMillis();
		if (!isJobOn) {
			logger.info("non-active cleanup job is off");
			return;
		}

		Map<String, ConnectionInfo> actorMap = actorMonitor.getActorMap();
		long currentTime = System.currentTimeMillis();
		List<String> uuids = new ArrayList<>();
		for (Entry<String, ConnectionInfo> actorInfo : actorMap.entrySet()) {
			ConnectionInfo connectionInfo = actorInfo.getValue();			
			logger.debug("currentTime: "+currentTime+", connectionInfo.getPingTime:"+connectionInfo.getPingTime()+", websocketIdleTime: "+websocketIdleTime);
			if (currentTime - connectionInfo.getPingTime() > websocketIdleTime) {
				logger.info("connection is idle. remove it");
				uuids.add(actorInfo.getKey());
			}
		}
		stopActors(uuids);
		Long t2 = System.currentTimeMillis();
		logger.info("End of Job - deleting stale actors. time taken: "+(t2-t1));
		ObjectNode json = Json.newObject();
		json.put("time", String.valueOf(System.currentTimeMillis()));
		json.put("instanceIp", RmsApplicationContext.getInstance().getIp());

		// Store channel current time in Redis. This is used to delete the channel info in case its closed		
		cacheInstanceInfoDao.saveInstanceInfo(instanceId, json.toString());

//		ObjectNode json = Json.newObject();
//		json.put("socketsCleaned", uuids.size());
//
//		addJobDetailsToEs("LocalActorCleanupJob", "success", json, System.currentTimeMillis());
	}

	public void cleanUpConnectionsOnShutdown() {
		Map<String, ConnectionInfo> actorMap = actorMonitor.getActorMap();
		Map<Integer, List<String>> connectionMap = new HashMap<>();
		for (Entry<String, ConnectionInfo> entry : actorMap.entrySet()) {
			connectionMap.computeIfAbsent(entry.getValue().getUserId(), (k)-> new ArrayList<>()).add(entry.getKey());
		}
		logger.info("Clear the following connections {}", connectionMap);
		if (!connectionMap.isEmpty()) {
			List<Integer> removedUserIds = cacheConnectionInfoDao.remove(connectionMap);
			logger.info("Clear the following users {}", removedUserIds);
			if (!removedUserIds.isEmpty()) {
				videokycService.removeCustomersFromQueue(removedUserIds);
			}
		}
		//Remove the instance info from the cache
		String instanceId = RmsApplicationContext.getInstance().getInstanceId();
		cacheInstanceInfoDao.removeInstanceInfo(instanceId);
	}

	public void cleanUpConnectionsOnStartup(String prevInstanceId) {
		Map<Integer, List<String>> connectionMap = new HashMap<>();
		Map<String, String> consMap = cacheConnectionInfoDao.getAllConnections();
		for (Entry<String, String> entry : consMap.entrySet()) {
			Integer userId = Integer.parseInt(entry.getKey());
			String consStr = entry.getValue();
			if (consStr == null || consStr.trim().length() == 0) continue;
			ArrayNode connectionNodes = (ArrayNode) jsonUtil.readTree(consStr);
			for (JsonNode connectionNode : connectionNodes) {
				String instanceId = connectionNode.findPath("instanceId").asText();
				if (instanceId.equals(prevInstanceId)){
					String uuid = connectionNode.findPath("uuid").asText();
					connectionMap.computeIfAbsent(userId, (k) -> new ArrayList<>()).add(uuid);
				}
			}
		}
		logger.info("Clear the following connections {}", connectionMap);
		if (!connectionMap.isEmpty()) {
			List<Integer> removedUserIds = cacheConnectionInfoDao.remove(connectionMap);
			logger.info("Clear the following users {}", removedUserIds);
			if (!removedUserIds.isEmpty()) {
				videokycService.removeCustomersFromQueue(removedUserIds);
			}
		}
		//Remove the instance info from the cache
		cacheInstanceInfoDao.removeInstanceInfo(prevInstanceId);
	}

	@Scheduled(initialDelay = 120000, fixedDelayString = "${redis.websocket.conn.cleanup.job.interval}")
	public void cleanUpNonActiveConnections() {
		String currentInstanceId = RmsApplicationContext.getInstance().getInstanceId();
		logger.info("Starting redis websocket non active websocket connections info clean up job: "+currentInstanceId);
		Long t1 = System.currentTimeMillis();
		ObjectNode json = Json.newObject();	
		try {
			Boolean isJobOn = Boolean.valueOf(env.getProperty(Constants.REDIS_WEBSOCKET_CONN_CLEANUP_JOB_ON));
			if (!isJobOn) {
				logger.info("non-active redis websocket cleanup job is off");
				addJobDetailsToEs("WebSocketCleanupJob", "ignore", null, System.currentTimeMillis());
				return;
			}

			Map<String, String> instanceMap = cacheInstanceInfoDao.getAllInstanceInfo();
			if (instanceMap.isEmpty()) {
				logger.info("instance list is empty");
			}

			Set<String> removeInstanceList = new HashSet<>();
			List<String> liveInstance = new ArrayList<>();
			logger.info("instance list: "+instanceMap.entrySet().size());
			logger.info("idle time: "+websocketIdleTime);
			for (Entry<String, String> channelEntry: instanceMap.entrySet()) {
				String channelInfo = channelEntry.getValue();
				logger.info("channel Info: {}", channelInfo);			
				JsonNode actualObj = jsonUtil.readTree(channelInfo);
				Long lastPingTime = Long.valueOf(actualObj.get("time").asText());
				logger.debug("instance Id: "+channelEntry.getKey()+", lastPingTime: "+lastPingTime);
				if (System.currentTimeMillis() - lastPingTime >= rmsInstanceIdleTime) {
					removeInstanceList.add(channelEntry.getKey());					
				}
				else {
					liveInstance.add(channelEntry.getKey());
				}
			}			
			logger.info("removeInstanceList:"+removeInstanceList+", liveInstance: "+liveInstance);

			// TODO: Ideally We should run job only on first machine but we may get into problem while looking at akka actor
			// and DB compatibility. So running job on all machine
			/*if (!liveInstance.isEmpty() && liveInstance.get(0).equals(currentInstanceId)) {
				logger.info("Running job on this machine. Instance Id: "+currentInstanceId);
			}
			else {
				logger.info("Not running cleanup job on this machine. Instance Id: "+currentInstanceId);
				return;
			}*/

			ObjectMapper mapper = new ObjectMapper();	
			Map<String, String> consMap = cacheConnectionInfoDao.getAllConnections();
			Set<String> activeConnections = new HashSet<>();
			Map<Integer, List<String>> removeNodesMap = new HashMap<>();
			if (consMap != null && !consMap.isEmpty()) {
				for (Entry<String, String> entry : consMap.entrySet()) {	
					Integer userId = Integer.parseInt(entry.getKey());
					String consStr = entry.getValue();
					logger.debug("userId: "+userId+", consStr: "+consStr);					
					if (consStr != null) {
						ArrayNode arrayNode = (ArrayNode) mapper.readTree(consStr);
						if (arrayNode.isArray() && arrayNode.size() > 0) {
							for (int indx = 0; indx < arrayNode.size(); indx++) {								
								JsonNode connectionNode = arrayNode.get(indx);
								String instanceId = connectionNode.findPath("instanceId").asText();
								String uuid = connectionNode.findPath("uuid").asText();
								logger.debug("connection: uuid: "+uuid+", instanceId: "+instanceId);
								if (removeInstanceList.contains(instanceId)) {
									logger.debug("connection is of removed instance so remove it");
									removeNodesMap.computeIfAbsent(userId, (k)->new ArrayList<>()).add(uuid);
									//addToRemoveList(removeNodesMap, userId, uuid);
								}
								else if (currentInstanceId.equals(instanceId) && !actorMonitor.getActorMap().containsKey(uuid)) {
									// remove this node. these are connections available in db for current instance but actor is not there.
									logger.debug("acotr doesn't exist for this connection");
									removeNodesMap.computeIfAbsent(userId, (k)->new ArrayList<>()).add(uuid);
									//addToRemoveList(removeNodesMap, userId, uuid);
								}
								else if (currentInstanceId.equals(instanceId)) {
									logger.info("valid connection. keep it");
									activeConnections.add(uuid);
								}
							}
						}
					}
				}
			}

			// find the places where actor is available but DB connection info is missing
			Map<String, ConnectionInfo> actorMap = actorMonitor.getActorMap();			
			List<String> staleActors = new ArrayList<>();
			for (Entry<String, ConnectionInfo> actorInfo : actorMap.entrySet()) {
				String uuid = actorInfo.getKey();
				if (!activeConnections.contains(uuid)) {
					staleActors.add(uuid);
				}
			}			
			stopActors(staleActors);

			int staleDbConnections = removeNodesMap.values().stream().map(List::size).reduce(0, Integer::sum);
			logger.info("Clear the following connection map {}", removeNodesMap);
			if (!removeNodesMap.isEmpty()) {
				List<Integer> removedUserIds = cacheConnectionInfoDao.remove(removeNodesMap);
				logger.info("Clear the following users {}", removedUserIds);
				if (!removedUserIds.isEmpty()) {
					videokycService.removeCustomersFromQueue(removedUserIds);
				}
			}
			// remove stale db connections		
//			int staleDbConnections = 0;
//			for (Entry<String, List<String>> nodeInfo : removeNodesMap.entrySet()) {
//				String userId = nodeInfo.getKey();
//				List<String> uuids = nodeInfo.getValue();
//				for (String uuid: uuids) {
//					logger.debug("remove instance: userId: "+userId+", uuid: "+uuid);
//					cacheConnectionInfoDao.remove(Integer.valueOf(userId), uuid);
//					logger.debug("update user status in agent queue, for userId: "+userId);
//					videokycService.changeUserStatusInQueue(Integer.valueOf(userId));
//					staleDbConnections++;
//				}
//			}

			// Remove Channel List
			for (String instanceId: removeInstanceList) {
				logger.debug("remove instance id: "+instanceId);
				cacheInstanceInfoDao.removeInstanceInfo(instanceId);
			}
			
			json.put("instancesCleaned", removeInstanceList.size());
			json.put("dbConnectionsCleaned", staleDbConnections);
			json.put("actorsCleaned", staleActors.size());
			
			addJobDetailsToEs("WebSocketCleanupJob", "success", json, System.currentTimeMillis());
		}
		catch(Exception ex) {
			logger.error("faild to run the job", ex);
		}
		Long t2 = System.currentTimeMillis();
		logger.info("end of websocket db connection job: time taken: "+(t2-t1));
	}

	private void addToRemoveList(Map<String, List<String>> removeNodesMap, String userId, String uuid) {
		List<String> list = removeNodesMap.get(userId);
		if (removeNodesMap.get(userId) == null) {
			list = new ArrayList<>();
			removeNodesMap.put(userId, list);
		}
		removeNodesMap.get(userId).add(uuid); // remove this connection. these are stopped instances
	}

	private void stopActors(List<String> actors) {
		logger.info("number of actors closing: "+actors.size());
		for (String uuid: actors) {
			logger.info("deleting stale actor: "+uuid);
			ActorRef ref = akkaUtil.getActorRef(uuid);
			if (ref != null) {
				ref.tell(PoisonPill.getInstance(), null);
			}
		}
	}

	private void addJobDetailsToEs(String jobName, String status, ObjectNode data, Long startTime) {
		try {			
			ObjectNode json = Json.newObject();			
			InetAddress host = InetAddress.getLocalHost();		
			json.put("machineIp", host.getHostAddress());
			json.put("jobStatus", status);
			json.put("jobName", jobName);
			json.put("startDate", startTime);
			Long end = System.currentTimeMillis();
			json.put("endDate", end);
			json.put("duration", (end-startTime));
			if (data != null) {
				Iterator<String> keys = data.fieldNames();
				while(keys.hasNext()) {
					String key = keys.next();
					if (data.get(key) != null) {
						json.put(key, data.get(key).asText());
					}
				}
			}
			elasticSearchDao.add(esJobIndexName, json.toString());
		} catch (Exception e) {
			logger.info("failed to send data to ES");
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {		
		actorMonitor = RmsApplicationContext.getInstance().getActorMonitor();
		websocketIdleTime = Integer.valueOf(env.getProperty(Constants.WEBSOCKET_IDLE_TIME));
		logger.info("websocketIdleTime: "+websocketIdleTime);

		rmsInstanceIdleTime = Integer.valueOf(env.getProperty(Constants.RMS_INSTNACE_IDLE_TIME));
		logger.info("rmsInstanceIdleTime: "+rmsInstanceIdleTime);

		esJobIndexName = env.getProperty(Constants.ES_JOB_INDEX_NAME);
		logger.info("esJobIndexName: "+esJobIndexName);
	}
}
