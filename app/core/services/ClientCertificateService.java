package core.services;

import java.util.List;
import java.util.Map;

import core.entities.ClientCertificate;

public interface ClientCertificateService {
	public Map<String, List<ClientCertificate>> getClientCertificates();
}
