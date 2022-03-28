package core.daos;

import com.fasterxml.jackson.databind.node.ArrayNode;

public interface CacheApplicationSettingsDao {
	public ArrayNode getApplicationSettingByOrgId(Integer orgId);
}
