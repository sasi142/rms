package core.daos;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import core.entities.ConnectionInfo;

public interface CacheConnectionInfoDao {
	public void create(Integer userId, ConnectionInfo info);

	public ArrayNode getAll(Integer userId);

	public Map<String, ArrayNode> getAll();

	public Map<Integer, ArrayNode> getAll(List<Integer> userIds);

	public void remove(Integer userId, String uuid);

	public void removeAll(Integer userId);

	public JsonNode get(Integer userId, String uuid);

	public void removeStaleActors(Map<String, ConnectionInfo> actorMap, String ipAddr);

	public Map<String, List<JsonNode>> getStaleActors(Map<String, ConnectionInfo> actorMap, String ipAddr);
	
	public void updateMapOpenStatus(Integer userId, String uuid, Integer status);

	public Map<String, String> getAllConnections();

	List<Integer> remove(Map<Integer, List<String>> connectionMap);
}
