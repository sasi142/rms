package core.services;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import utils.RmsApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import core.daos.CacheConnectionInfoDao;
import core.entities.ActorMonitor;

@Service
public class MonitoringServiceImpl implements MonitoringService {
	final static Logger logger = LoggerFactory.getLogger(MonitoringServiceImpl.class);
	@Autowired
	@Qualifier("CacheImpl")
	private CacheConnectionInfoDao cacheConnectionInfoDao;

	@Override
	public ActorMonitor getActorMonitor(Boolean actors, Boolean details) {
		logger.info("Getting Actor Monitor with details " + details);
		ActorMonitor actorMonitor = RmsApplicationContext.getInstance().getActorMonitor();

		logger.info("Getting all cached connections ");
		Map<String, ArrayNode> map = cacheConnectionInfoDao.getAll();
		if (details) {
			actorMonitor.setDbActorMap(map);
		} else {
			actorMonitor.setDbActorMap(null);
		}
		logger.info("Got all cached connections of size " + (map == null ? null : map.size()));

		String ip = RmsApplicationContext.getInstance().getIp();
		actorMonitor.setIp(ip);
		actorMonitor.setInstanceId(RmsApplicationContext.getInstance().getInstanceId());
		Set<String> dbActorIds = new HashSet<>();
		int count = 0;
		if (map != null && !map.isEmpty()) {
			for (Entry<String, ArrayNode> entry : map.entrySet()) {
				ArrayNode node = entry.getValue();
				count = count + node.size();
				for (int indx = 0; indx < node.size(); indx++) {
					JsonNode json = node.get(indx);
					String ip1 = json.findPath("ip").asText();
					if (ip.equalsIgnoreCase(ip1)) {
						String uuid = json.findPath("uuid").asText();
						dbActorIds.add(uuid);
					}
				}
			}
		}
		if (actors) {
			actorMonitor.setDbActorIds(dbActorIds);
		} else {
			actorMonitor.setDbActorIds(null);
		}
		return actorMonitor;
	}

}
