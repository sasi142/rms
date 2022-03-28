package core.services;

import java.util.List;
import com.fasterxml.jackson.databind.node.ObjectNode;
import core.entities.BulkMemoDump;

public interface BulkMemoProcessService {

	BulkMemoDump getMemoDump();

	List<BulkMemoDump> createMemoFromExcel(BulkMemoDump memoDump, List<ObjectNode> memoList);

	void updateBulkMemoDumpStatus(BulkMemoDump memoDump);

	List<ObjectNode> getMemoListForProcess(BulkMemoDump memoDump);

	void sendCreateMemoEventToRecipients(List<BulkMemoDump> updatedMemoDump);

}
