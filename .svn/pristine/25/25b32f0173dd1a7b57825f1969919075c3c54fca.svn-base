package core.redis;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import core.exceptions.InternalServerErrorException;
import core.utils.Constants;
import core.utils.EncryptionUtil;
import core.utils.Enums.DatabaseType;
import core.utils.Enums.ErrorCode;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.jedis.JedisLockProvider;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Component
public class RedisConnectionImpl implements RedisConnection, InitializingBean {

	final static Logger logger = LoggerFactory.getLogger(RedisConnectionImpl.class);

	@Autowired
	private Environment	env;

	private boolean		isSSLConnectionEnabled;
	private JedisPool	masterImsConnectionPool;
	private JedisPool	masterRmsConnectionPool;

	@Override
	public void afterPropertiesSet() throws Exception {
		isSSLConnectionEnabled = Boolean.parseBoolean(env.getProperty(Constants.REDIS_SSL_ENABLED));
		masterImsConnectionPool = JedisImsMasterFactory();
		masterRmsConnectionPool = JedisRmsMasterFactory();
	}

	/**
	 * Return Jedis connection to master
	 * 
	 * @return
	 */
	public Jedis getMasterConnection(DatabaseType type) {
		if (type == DatabaseType.Ims) {
			return masterImsConnectionPool.getResource();
		} else if (type == DatabaseType.Rms) {
			return masterRmsConnectionPool.getResource();
		} else {
			return null;
		}
	}

	/**
	 * This API will return read replica connection to client.
	 * TODO: We currenly have no replica so we will use master connection. We also need to write code
	 * to load balance traffic between multiple read replicas. We will do it once we reach there
	 * 
	 * @return
	 */
	public Jedis getSlaveConnection(DatabaseType type) {
		return getMasterConnection(type);
	}

	public void releaseMasterConnection(Jedis jedis, DatabaseType type) {
		if (type == DatabaseType.Ims) {
			jedis.close();
		} else if (type == DatabaseType.Rms) {
			jedis.close(); //TODO return is deprecated so used close
			//masterRmsConnectionPool.returnResource(jedis);
		} else {
		}
	}

	public void releaseSlaveConnection(Jedis jedis, DatabaseType type) {		
		releaseMasterConnection(jedis, type);
	}

	public void getJedis() {

	}

	private JedisPool JedisImsMasterFactory() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();		
		Integer maxIdle = Integer.valueOf(env.getProperty(Constants.REDIS_IMS_CONNECTION_MAX_IDLE));
		Integer database = Integer.valueOf(env.getProperty(Constants.REDIS_IMS_DB_INDEX));
		Integer timeout = Integer.valueOf(env.getProperty(Constants.REDIS_IMS_CONNECTION_TIMEOUT));

		poolConfig.setMaxIdle(maxIdle);
		poolConfig.setMinIdle(maxIdle);

		String redisUrl = env.getProperty(Constants.REDIS_SERVER_MASTER);
		logger.info("redisUrl: " + redisUrl);
		try {		
			String url[] = redisUrl.split(":");
			String host = url[0];
			Integer port = Integer.valueOf(url[1]);
			String password = getRedisServerPassword();
			JedisPool pool = new JedisPool(poolConfig, host, port, timeout, password, database, isSSLConnectionEnabled);
			return pool;
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, "failed to get redis connection", e);
		}
	}

	private String getRedisServerPassword() {
		String password = null;
		Boolean authEnabled = Boolean.valueOf(env.getProperty(Constants.REDIS_PASSWORD_ENABLED));
		logger.debug("Auth Enabled: " + authEnabled);
		if (authEnabled) {
			logger.debug("Auth enable is true");
			password = env.getProperty(Constants.REDIS_SERVER_PASSWORD);
			logger.debug("Getting password from property file");
			password = EncryptionUtil.decryptPropertyValue(password);
			logger.debug("Password for redis received from properties file");
		}

		logger.debug("Auth Enabled is done");
		return password;
	}

	private JedisPool JedisRmsMasterFactory() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();				
		Integer maxIdle = Integer.valueOf(env.getProperty(Constants.REDIS_RMS_CONNECTION_MAX_IDLE));
		Integer database = Integer.valueOf(env.getProperty(Constants.REDIS_RMS_DB_INDEX));
		Integer timeout = Integer.valueOf(env.getProperty(Constants.REDIS_RMS_CONNECTION_TIMEOUT));
		poolConfig.setMaxIdle(maxIdle);
		poolConfig.setMinIdle(maxIdle);

		// poolConfig.setMaxWait(10000); // milliseconds
		// poolConfig.setTestOnBorrow(true);
		// poolConfig.setTestOnReturn(true);
		// poolConfig.setTestWhileIdle(true);

		String redisUrl = env.getProperty(Constants.REDIS_SERVER_MASTER);
		logger.info("redisUrl: " + redisUrl);
		try {		
			String url[] = redisUrl.split(":");
			String host = url[0];
			Integer port = Integer.valueOf(url[1]);
			String password = getRedisServerPassword();
			JedisPool pool = new JedisPool(poolConfig, host, port, timeout, password, database, isSSLConnectionEnabled);
			return pool;
		} catch (Exception e) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, "failed to get redis connection", e);
		}
	}
	public LockProvider getLockProvider(String lockName) {
		JedisPool jedisPool = JedisRmsMasterFactory();
	    return new JedisLockProvider(jedisPool, lockName);
	}
}
