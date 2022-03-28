package core.redis;
import core.utils.Enums.DatabaseType;
import net.javacrumbs.shedlock.core.LockProvider;
import redis.clients.jedis.Jedis;

public interface RedisConnection {
	Jedis getMasterConnection(DatabaseType type);
	Jedis getSlaveConnection(DatabaseType type);
	void releaseMasterConnection(Jedis jedis, DatabaseType type);
	void releaseSlaveConnection(Jedis jedis, DatabaseType type);
	LockProvider getLockProvider(String lockName);
}
