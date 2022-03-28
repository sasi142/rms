package core.daos;

import java.util.List;
import java.util.Map;

import core.entities.ClosedConnection;

public interface ClosedConnectionDao  extends JpaDao<ClosedConnection> {
	public Map<Integer, ClosedConnection> getByUserIds(List<Integer> userIds, Long time);
	public List<ClosedConnection> getExpiredConnections(Long time);
	public void usert(ClosedConnection connection);
}
