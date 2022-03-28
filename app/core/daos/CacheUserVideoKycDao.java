package core.daos;

import com.fasterxml.jackson.databind.JsonNode;

public interface CacheUserVideoKycDao {
	public JsonNode getVideoKycId(Integer userId);
}
