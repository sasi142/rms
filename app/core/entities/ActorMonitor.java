package core.entities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.node.ArrayNode;

import core.utils.Enums;
import core.utils.Enums.MessageType;
import core.utils.Enums.RmsMessageType;
import messages.UserConnection;

public class ActorMonitor {
	private String						ip;
	private String						instanceId;
	private Map<String, ConnectionInfo>	actorMap		= new ConcurrentHashMap<String, ConnectionInfo>();
	private Map<String, ArrayNode>		dbActorMap;
	private Set<String>					dbActorIds;
	private static Map<String, Long>	inMsgCounts		= new HashMap<String, Long>();
	private static Map<String, Long>	outMsgCounts	= new HashMap<String, Long>();
	
	private static ActorMonitor actorMonitor = new ActorMonitor();
	
	private ActorMonitor() {
		
	}
	
	public static ActorMonitor getMonitor() {
		return actorMonitor;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getTotalActors() {
		return actorMap == null ? 0 : actorMap.size();
	}

	public int getTotalDbActors() {
		return dbActorIds == null ? 0 : dbActorIds.size();
	}

	public Set<String> getActorIds() {
		return actorMap.keySet();
	}

	public Set<String> getDbActorIds() {
		return dbActorIds;
	}

	public void setDbActorIds(Set<String> dbActorIds) {
		this.dbActorIds = dbActorIds;
	}

	public Map<String, ArrayNode> getDbActorMap() {
		return dbActorMap;
	}

	
	public void setDbActorMap(Map<String, ArrayNode> dbActorMap) {
		this.dbActorMap = dbActorMap;
	}


	public Map<String, ConnectionInfo> getActorMap() {
		return actorMap;
	}

	public void setActorMap(Map<String, ConnectionInfo> actorMap) {
		this.actorMap = actorMap;
	}

	public static void incrementCount(RmsMessageType rmsMsgType, String msgType, String msgSubType) {
		Long count = 1L;
		String key = msgType + "_" + msgSubType;
		if (RmsMessageType.In.equals(rmsMsgType)) {
			if (inMsgCounts.containsKey(key)) {
				count = inMsgCounts.get(key);
				++count;
			}
			inMsgCounts.put(key, count);
		} else if (RmsMessageType.Out.equals(rmsMsgType)) {
			if (outMsgCounts.containsKey(key)) {
				count = outMsgCounts.get(key);
				++count;
			}
			outMsgCounts.put(key, count);
		}
	}

	public Map<String, Long> getInMsgCounts() {
		return inMsgCounts;
	}

	public Map<String, Long> getOutMsgCounts() {
		return outMsgCounts;
	}
	
	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public void recordMsgCounts(RmsMessageType rmsMessageType, int type, int subType) {
		String typeName = MessageType.getMessageTypeById(type).name();
		String subTypeName = Enums.getMessageSubTypeByTypeIdSubTypeId(type, subType);
		ActorMonitor.incrementCount(rmsMessageType, typeName, subTypeName);
	}
	

	
	public UserConnection getUserConnection(String uuid) {
		ConnectionInfo info = actorMap.get(uuid);
		if (info != null) {
			return info.getConnection();
		}
		return null;
	}
}
