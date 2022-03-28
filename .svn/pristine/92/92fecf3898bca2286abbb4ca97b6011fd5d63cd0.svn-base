package core.daos;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;


public interface CacheUserDao {
	public JsonNode find(Integer userId);

	List<JsonNode> findAll(List<Integer> userIds);

    void storeUserUnreadCount(Integer userId, String data);

	String getUserUnreadCount(Integer userId);
}
