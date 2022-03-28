package core.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import core.daos.ClientCertificateDao;
import core.entities.ClientCertificate;

@Service
@Transactional(rollbackFor = { Exception.class })
public class ClientCertificateServiceImpl implements ClientCertificateService, InitializingBean {
	final static Logger logger = LoggerFactory.getLogger(ClientCertificateServiceImpl.class);
	private Map<String, List<ClientCertificate>> clientCertificateMap = new HashMap<>();

	@Autowired
	private ClientCertificateDao clientCertificateDao;

	public ClientCertificateServiceImpl() {

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		List<ClientCertificate> clientCertificates = clientCertificateDao.getClientCertificates();
		logger.info("caching ClientCertificate. total: " + clientCertificates.size());
		for (ClientCertificate clientCertificate : clientCertificates) {
			if (clientCertificateMap.containsKey(clientCertificate.getClientId())) {
				clientCertificateMap.get(clientCertificate.getClientId()).add(clientCertificate);
			} else {
				List<ClientCertificate> list = new ArrayList<ClientCertificate>();
				list.add(clientCertificate);
				clientCertificateMap.put(clientCertificate.getClientId(), list);
			}
		}
	}

	@Override
	public Map<String, List<ClientCertificate>> getClientCertificates() {
		return clientCertificateMap;
	}
}
