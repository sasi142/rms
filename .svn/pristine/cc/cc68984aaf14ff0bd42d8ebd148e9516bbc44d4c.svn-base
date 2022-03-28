package core.daos;


import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import core.entities.BulkMemoDump;
public interface BulkMemoDumpDao extends JpaDao<BulkMemoDump> {

	BulkMemoDump getMemoDump();

	List<BulkMemoDump> createMemoFromMemoDump(Integer orgId, Integer userId, List<ObjectNode> memoList);

	void updateBulkMemoDumpStatus(Integer bulkMemoDumpId, Byte status);

	void updateSentMemoCount(Integer bulkMemoDumpId, Integer memoSentCount);

}
