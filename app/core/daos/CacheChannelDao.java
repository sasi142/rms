package core.daos;

import com.fasterxml.jackson.databind.JsonNode;


public interface CacheChannelDao {
	public JsonNode find(Integer channelId);
}
