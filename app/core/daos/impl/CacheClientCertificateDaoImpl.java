package core.daos.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.daos.CacheClientCertificateDao;
import core.daos.ClientCertificateDao;
import core.entities.ClientCertificate;
import core.exceptions.InternalServerErrorException;
import core.redis.RedisConnection;
import core.utils.Constants;
import core.utils.Enums.DatabaseType;
import core.utils.Enums.ErrorCode;

@Repository("CacheClientCertificateDao")
public class CacheClientCertificateDaoImpl implements CacheClientCertificateDao, InitializingBean{
	final static Logger logger = LoggerFactory.getLogger(CacheClientCertificateDaoImpl.class);
	@Autowired
	private ClientCertificateDao clientCertificateDao;
	
	@Autowired
	public RedisConnection	redisConnection;

	@Autowired
	public Environment		env;

	private String			clientCertificateBucketName;
	
	private final ObjectMapper mapperObj = new ObjectMapper();

	@Override
	public void afterPropertiesSet() throws Exception {
		this.clientCertificateBucketName = env.getProperty(Constants.REDIS_RMS_CLIENT_CERTIFICATE_STORE);
		logger.info("clientCertificateBucketName: " + clientCertificateBucketName);
		mapperObj.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		refresh();
	}
	
	@Override
	public Integer refresh() {
		Jedis jedis = redisConnection.getMasterConnection(DatabaseType.Rms);
		List<ClientCertificate> clientCertificateList = clientCertificateDao.getClientCertificates();
		Map<String, String> clientCertificateJsonMap = new HashMap<>();
		try {
			Pipeline p = jedis.pipelined();
			p.del(clientCertificateBucketName);
			for (ClientCertificate cert: clientCertificateList) {
				clientCertificateJsonMap.put(createKey(cert), mapperObj.writeValueAsString(cert));
			}
			p.hmset(clientCertificateBucketName, clientCertificateJsonMap);
			p.sync();
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			redisConnection.releaseMasterConnection(jedis, DatabaseType.Rms);
		}
		Integer count = clientCertificateList.size();
		logger.debug("client certificate refreshed." + count);
		return count;
	}

	@Override
	public ClientCertificate getClientCertificate(String bundleKey) {
		ClientCertificate cert = null;
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Rms);
		try {
			String certStr = jedis.hget(clientCertificateBucketName, bundleKey);
			if (certStr != null) {
				cert = mapperObj.readValue(certStr, ClientCertificate.class);
			}
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}
		logger.debug("client certificate " + cert + " returned for bundleKey: " + bundleKey);
		return cert;
	}
	
	@Override
	public Set<String> getClientCertificateKeys(){		
		Set<String> certs = new HashSet<String>();
		Jedis jedis = redisConnection.getSlaveConnection(DatabaseType.Rms);
		try {
			certs = jedis.hkeys(clientCertificateBucketName);
		} catch (Exception ex) {
			throw new InternalServerErrorException(ErrorCode.Internal_Server_Error, ErrorCode.Internal_Server_Error.getName(), ex);
		} finally {
			redisConnection.releaseSlaveConnection(jedis, DatabaseType.Rms);
		}
		return certs;
	}
	
	private String createKey(ClientCertificate cert){
		return ((cert.getOrganizationId() != null)? (cert.getOrganizationId() + "_") : "") + cert.getClientId() + "_" + ((cert.getBundleId() != null && !"".equals(cert.getBundleId().trim())) ? cert.getBundleId():"");
	}
}
