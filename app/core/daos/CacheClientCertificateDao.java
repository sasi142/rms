package core.daos;

import java.util.Set;

import core.entities.ClientCertificate;

public interface CacheClientCertificateDao {
	public Integer refresh();
	public ClientCertificate getClientCertificate(String bundleKey);
	public Set<String> getClientCertificateKeys();
}
