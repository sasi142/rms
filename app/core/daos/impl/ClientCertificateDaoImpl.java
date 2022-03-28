package core.daos.impl;

import java.util.List;

import org.springframework.stereotype.Repository;
import core.daos.ClientCertificateDao;
import core.entities.ClientCertificate;

@Repository
public class ClientCertificateDaoImpl extends AbstractJpaDAO<ClientCertificate> implements ClientCertificateDao {
	
	public ClientCertificateDaoImpl() {
		super();
		setClazz(ClientCertificate.class);
	}
	
	@Override	
	public List<ClientCertificate> getClientCertificates() {
		List<ClientCertificate> clientCertificateList = findAll();
		return clientCertificateList;
	}
}
